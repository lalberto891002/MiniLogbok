# MiniLogbook

A minimal blood glucose logbook Android app for patients, built with **Jetpack Compose**, **Room**, and **Koin**. Patients can log readings in either mmol/L or mg/dL, view their history with colour-coded status indicators, and delete entries via swipe or a dedicated button.

---

## Features

- Log blood glucose values in **mmol/L** or **mg/dL**
- Live unit conversion — switching units converts the entire history list instantly
- **Average blood glucose** card updates after every save
- Colour-coded entries: 🟢 in target / 🟠 ok / 🔴 out of range
- Delete entries via **swipe-to-dismiss** or the **trash icon** inside each card
- Undo deletion via a **Snackbar** before the action is committed to the database
- Responsive layout — two-column on tablets/landscape, single-column on phones
- **Encrypted database** — patient data is encrypted at rest using SQLCipher + Android Keystore

---

## Architecture

The project follows a layered architecture with a clear separation of concerns:

```
app/
├── data/           — Room entities, DAO, database, encryption
├── domain/         — Business logic (conversion, validation, status)
│   ├── model/      — BloodGlucoseStatus enum
│   └── service/    — GlucoseService
├── di/             — Koin dependency injection module
└── ui/
    ├── components/ — Reusable Composables
    ├── screen/     — MiniLogbookScreen + section composables
    ├── theme/      — Material3 theme, colours, typography
    ├── util/       — Status → colour mapper
    └── viewmodel/  — GlucoseViewModel + GlucoseState
```

### Data layer

| Class | Responsibility |
|---|---|
| `GlucoseEntry` | Room entity — stores `valueInMmol` as the canonical unit, plus `timestamp` |
| `GlucoseDao` | `insert`, `delete`, `deleteAll`, `getAllEntries` (Flow, ordered DESC by timestamp) |
| `GlucoseDatabase` | Room database with SQLCipher encryption via `SupportOpenHelperFactory` |
| `PassphraseManager` | Generates and persists the DB passphrase in `EncryptedSharedPreferences` |

**All values are stored in mmol/L.** Conversion to mg/dL happens at the UI layer only.

### Domain layer

`GlucoseService` encapsulates all conversion and validation logic:
- `validateValue(Double?)` — rejects null and negative values
- `toMmolIfValid(Double?, GlucoseUnit)` — validates and converts to mmol/L
- `convertValue(Double, GlucoseUnit, GlucoseUnit)` — bidirectional conversion
- `fromMmol(Double, GlucoseUnit)` — converts stored mmol to display unit
- `getGlucoseStatus(Double)` — classifies a mmol/L value as `IN_TARGET` / `OK` / `OUT_OF_RANGE`
- `getGlucoseStatusByUnit(Double, GlucoseUnit)` — same, for any unit

**Status thresholds (mg/dL):**

| Status | Range |
|---|---|
| `IN_TARGET` | 90 – 140 |
| `OK` | 70 – 90 or 140 – 180 |
| `OUT_OF_RANGE` | < 70 or > 180 |

### ViewModel layer

`GlucoseViewModel` consumes `IGlucoseService` (injected via Koin constructor injection) to handle business logic like unit conversion and status determination. It exposes:
- `glucoseState: StateFlow<GlucoseState>` — combines `average + unit` from Room/`_unit` only. **Never triggered by keystrokes.**
- `glucoseEntries: Flow<PagingData<GlucoseEntry>>` — Paging 3 flow for list items, cached in `viewModelScope`.
- `inputValue: StateFlow<String>` — separate flow for the text field value
- `displayErrorMessage: StateFlow<Boolean>` — set to `true` when `saveEntry()` fails validation, reset to `false` on any input change or successful save

**Key design decision:** `inputValue` and `displayErrorMessage` are kept outside the `combine()` operator so that typing in the input field never triggers a Room query emission or recomposes the entries list. Each `StateFlow` is collected independently in the screen.

### UI layer

`MiniLogbookScreen` is split into three private composables to minimise recomposition scope:

- **`InputSection`** — receives individual slices (`inputValue`, `unit`, `errorMessage`) and stable lambda references; only recomposes when its own data changes
- **`SummarySection`** — receives `average`, `unit`, and an already-computed `status: BloodGlucoseStatus`; purely declarative with no ViewModel dependency — previewable in isolation
- **`HistorySection`** — receives `onConvertValue` and `onGetStatus` lambdas instead of the ViewModel; `LazyColumn` with `SwipeToDismissBox` per item; auto-scrolls to the top when a new entry is added

All three private composables follow **state hoisting**: they receive only the data and lambdas they need, making them independently testable and previewable without a real ViewModel.

---

## Key Technical Decisions

### Recomposition optimisation

| Technique | Where | Why |
|---|---|---|
| `remember`-wrapped `onDeleteRequest` lambda | `MiniLogbookScreen` | The lambda captures coroutine scope and snackbar state; wrapping it in `remember` gives it a stable reference so `HistorySection` skips recomposition when nothing else changed |
| `remember`-wrapped `onConvertValue` / `onGetStatus` lambdas | `MiniLogbookScreen` | Wrapping ViewModel method references in `remember` (no keys) produces stable function references; `HistorySection` receives the same instance across recompositions and is never recomposed unnecessarily |
| `remember(average, unit)` for `summaryStatus` | `MiniLogbookScreen` | Status is derived once at the screen level and passed down as a plain value; `SummarySection` is fully declarative and skips recomposition unless `average` or `unit` actually change |
| `remember(entry.valueInMmol, unit)` / `remember(convertedValue, unit)` per item | `HistorySection` `LazyColumn` items | Conversion and status classification are pure functions — memoised per item so they only re-run when their specific inputs change, not on every list recomposition |
| `derivedStateOf` for `isDismissed` | Each `SwipeToDismissBox` item | Converts continuous `dismissState.currentValue` reads into a boolean that only invalidates downstream computations when the settled state actually flips |
| `snapshotFlow + filter` instead of `LaunchedEffect(currentValue)` | `HistorySection` item | Fires only once when the swipe fully settles to `EndToStart`, not on every drag-offset change |
| `Animatable + drawBehind` instead of `animateColorAsState` | `SwipeToDismissBox` background | Color animation runs entirely in the draw phase — zero recompositions per frame during the swipe |
| `LaunchedEffect(itemCount)` + `rememberSaveable` guard | `HistorySection` | Scrolls the list to the top only when `itemCount` strictly increases (new entry added) **and** the list is scrolled down — skips the initial load emission and avoids unnecessary scroll calls |
| `remember(isError)` for `colors`, `labelStyle`, and slot lambdas | `GlucoseInputField` | Memoises `TextFieldColors`, the label `TextStyle`, and the `label`/`supportingText` slot lambdas so they are only recreated when the error state changes, not on every parent recomposition |

### Swipe-to-delete + Undo

The delete flow is designed to avoid full-screen recomposition:
1. Each item's `LaunchedEffect` (via `snapshotFlow`) calls `onDeleteRequest` with a `Pair<GlucoseEntry, suspend () -> Unit>` — the lambda captures `dismissState::reset` so the item can animate back without any state hoisting
2. `onDeleteRequest` (held at screen scope) launches a coroutine that shows the Snackbar
3. On **Undo**: `dismissState.reset()` is called via the captured lambda — the item animates back
4. On **dismiss**: `deleteEntry()` is called directly from the snackbar result handler

### Database encryption

Patient glucose data is sensitive health data and is encrypted at rest:

```
Patient data
    │
    ▼
Room ──► SQLCipher (AES-256) ──► encrypted .db file
              │
         passphrase (32 random bytes)
              │
    EncryptedSharedPreferences (AES-256-GCM)
              │
    Android Keystore
         ├── API 28+ with StrongBox → dedicated security chip (Titan M / SE)
         └── API 23+ without StrongBox → TEE-backed hardware key
```

---

## Dependency Injection

Koin is used for DI via `AppModule`:

```
GlucoseDatabase (singleton)
    └── GlucoseDao (singleton)
GlucoseService (factory)
GlucoseViewModel (viewModel)
```

---

## Testing

### Unit tests (`src/test`)

| Test class | Coverage |
|---|---|
| `GlucoseServiceTest` | Validation, mmol↔mg/dL conversion, status thresholds for both units |
| `GlucoseViewModelTest` | Input value updates, error on invalid/negative save, successful save clears input, unit conversion of input value, average calculation, `isLoading` lifecycle |

### Instrumented DAO tests (`src/androidTest`)

Run against an **in-memory SQLCipher database**:

| Test | What it verifies |
|---|---|
| `insertAndReadParameterizedEntry` | Inserted value is retrieved correctly (parameterized) |
| `insertMultipleEntriesOrderedByTimestamp` | Entries are returned DESC by timestamp |
| `deleteAllEntries` | `deleteAll()` empties the table |
| `deleteSingleEntry_removesOnlyThatEntry` | Deleting one entry leaves others intact |
| `deleteSingleEntry_listBecomesEmptyWhenOnlyOneEntry` | Deleting the only entry results in empty list |
| `deleteEntry_doesNotAffectOtherEntries` | Deleting middle entry preserves order of remaining |
| `deleteNonExistentEntry_doesNothing` | Deleting an entry with an unknown id is a no-op |

### Instrumented UI tests (`src/androidTest`) — `MiniLogbookScreenTest`

Uses a custom `MiniLogbookTestRunner` + `TestApplication` to prevent the real Koin/database from starting. `RuleChain(koinRule → composeRule)` guarantees Koin is initialised with a fresh in-memory database **before** `MainActivity` launches.

| Test | What it verifies |
|---|---|
| `enterValidValue_saveIt_appearsInList` | Happy path — value appears in list after save |
| `enterInvalidValue_showsError_doesNotAppearInList` | Text input `"abc"` shows error, no card created |
| `enterNegativeValue_showsError` | Negative value is rejected with error message |
| `saveEntry_inputFieldClears` | Input field resets to empty after successful save |
| `saveEntry_averageUpdates` | Average card is visible after saving an entry |
| `deleteEntry_viaIconButton_removesFromList` | Tap trash icon → entry removed after snackbar timeout |
| `deleteEntry_viaIconButton_snackbarShown` | Tap trash icon → snackbar with "Entry removed" + "Undo" shown |
| `deleteEntry_viaSwipe_removesFromList` | Swipe left → entry removed after snackbar timeout |
| `deleteEntry_viaSwipe_undoRestoresItem` | Swipe left → tap Undo → entry is restored |
| `saveMultipleEntries_allAppearInList` | Three entries saved, all three visible |
| `configurationChange_dataIsPreserved` | Rotation → saved entry still visible |
| `previousEntriesLabel_isAlwaysVisible` | History section header always present |
| `unitSelector_switchToMgDl_convertedValueShown` | Switching to mg/dL shows converted value in entry card |
| `unitSelector_switchUnit_allEntriesShowCorrectUnit` | All three entries update unit on toggle, verified via `testTag`; round-trip mmol/L → mg/dL → mmol/L |

---

## Tech Stack

| Component | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| State | `StateFlow` + `collectAsStateWithLifecycle` |
| Database | Room 2.8.4 |
| Pagination | Paging 3 (3.3.6) |
| Encryption | SQLCipher 4.13.0 + Android Keystore (AES-256-GCM via `KeyGenerator` / `Cipher` directly) |
| DI | Koin 4.1.1 |
| Unit tests | JUnit 4 + Mockito + kotlinx-coroutines-test |
| UI tests | Compose UI Test + AndroidJUnit4 |
| Build | Gradle KTS + KSP |

