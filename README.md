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
- **Detail screen** — tap any entry to open a full-detail view showing both mmol/L and mg/dL values, date/time, and colour-coded status
- Responsive layout — two-column on tablets/landscape, single-column on phones
- **Encrypted database** — patient data is encrypted at rest using SQLCipher + Android Keystore

---

## Architecture

The project follows a layered architecture with a clear separation of concerns:

```
app/
├── data/
│   ├── GlucoseEntry.kt      — Room entity
│   ├── GlucoseDao.kt        — DAO interface
│   ├── GlucoseDatabase.kt   — Encrypted Room database singleton
│   ├── Migrations.kt        — Central registry for Room schema migrations
│   └── PassphraseManager.kt — Android Keystore passphrase management
├── domain/                  — Business logic (conversion, validation, status)
│   ├── model/               — BloodGlucoseStatus enum, GlucoseUnit enum
│   └── service/             — IGlucoseService + GlucoseService
├── di/                      — Koin dependency injection module
└── ui/
    ├── components/          — Reusable Composables
    ├── navigation/          — AppNavGraph (NavHost) + AppRoutes (sealed class)
    ├── screen/              — MiniLogbookScreen, GlucoseDetailScreen + section composables
    ├── theme/               — Material3 theme, colours, typography
    ├── util/                — Status → colour mapper
    └── viewmodel/           — GlucoseViewModel + GlucoseState + GlucoseListEntryUi + GlucoseDetailViewModel + GlucoseEntryUi
```

### Data layer

Each type lives in its own file under `data/`:

| File | Responsibility |
|---|---|
| `GlucoseEntry.kt` | Room entity — stores `valueInMmol` as the canonical unit, plus `timestamp` |
| `GlucoseDao.kt` | `insert`, `delete`, `deleteById`, `deleteAll`, `getAllEntries` (PagingSource), `getAverageValue` (Flow) |
| `GlucoseDatabase.kt` | Room database singleton with SQLCipher encryption via `SupportOpenHelperFactory` |
| `PassphraseManager.kt` | Generates, encrypts and persists the DB passphrase via Android Keystore |

**All values are stored in mmol/L.** Conversion to mg/dL happens at the UI layer only.

### Domain layer

`domain/model/` contains two enums:
- `GlucoseUnit` — the two supported measurement units (`MMOL_L`, `MG_DL`)
- `BloodGlucoseStatus` — classification of a reading (`IN_TARGET`, `OK`, `OUT_OF_RANGE`)

`GlucoseService` encapsulates all conversion and validation logic:
- `validateValue(Double?)` — rejects null and negative values; **0.0 is accepted as valid**
- `toMmol(Double, GlucoseUnit)` — converts a value from any unit to mmol/L
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
- `glucoseState: StateFlow<GlucoseState>` — combines `average + unit + status` from Room/`_unit` only. **Never triggered by keystrokes.**
- `glucoseEntries: Flow<PagingData<GlucoseListEntryUi>>` — Paging 3 flow of pre-mapped list items; uses `flatMapLatest` on `_unit` so every unit change re-maps all loaded pages with the new `convertedValue`, then `cachedIn(viewModelScope)`.
- `inputValue: StateFlow<String>` — separate flow for the text field value
- `displayErrorMessage: StateFlow<Boolean>` — set to `true` when `saveEntry()` fails validation, reset to `false` on any input change or successful save

**Key design decision:** `inputValue` and `displayErrorMessage` are kept outside the `combine()` operator so that typing in the input field never triggers a Room query emission or recomposes the entries list. Each `StateFlow` is collected independently in the screen.

`GlucoseListEntryUi` is the UI model for each history list item, pre-computing all display values so the composable reads plain properties with no function calls:

| Property | Description |
|---|---|
| `id` | Primary key (used for navigation to detail and delete) |
| `valueInMmol` | Raw mmol/L value (canonical storage unit) |
| `convertedValue` | Value converted to `unit` for display (via `IGlucoseService.fromMmol`) |
| `status` | `BloodGlucoseStatus` classification (via `IGlucoseService.getGlucoseStatus`) |
| `timestamp` | Unix epoch milliseconds for date formatting in the list item |
| `unit` | The unit that `convertedValue` is expressed in |

`GlucoseState` now also carries:
- `status: BloodGlucoseStatus` — computed from `average` and `unit` inside the `combine` operator; the screen reads it as a plain property without any ViewModel function call

#### GlucoseDetailViewModel

`GlucoseDetailViewModel` is scoped to the `GlucoseDetail` destination and obtains the entry id from its `SavedStateHandle` (populated automatically by Navigation Compose). It maps the raw `GlucoseEntry` from the DAO into a `GlucoseEntryUi` model, keeping all derived computations out of the UI layer. It exposes:
- `entry: StateFlow<GlucoseEntryUi?>` — single-entry flow mapped from `GlucoseDao.getEntryById(entryId)`; emits `null` while loading or if the entry is not found

`GlucoseEntryUi` is the UI model produced by the mapping:

| Property | Description |
|---|---|
| `id` | Primary key of the entry |
| `formattedDate` | Pre-formatted date/time string (`EEEE, MMM dd yyyy  •  HH:mm`) |
| `valueInMmol` | Glucose value in mmol/L |
| `valueInMgdl` | Glucose value converted to mg/dL (via `IGlucoseService.fromMmol`) |
| `status` | `BloodGlucoseStatus` classification (via `IGlucoseService.getGlucoseStatus`) |

### Navigation layer

Navigation is implemented with **Navigation Compose** using a type-safe sealed class `AppRoutes` and a single `AppNavGraph` composable.

#### AppRoutes

```kotlin
sealed class AppRoutes(val route: String) {
    data object LogbookList : AppRoutes("logbook_list")
    data object GlucoseDetail : AppRoutes("glucose_detail/{entryId}") {
        const val ARG_ENTRY_ID = "entryId"
        fun createRoute(entryId: Int) = "glucose_detail/$entryId"
    }
}
```

#### AppNavGraph

`AppNavGraph` creates a `NavController` with `rememberNavController()` and defines two destinations inside a `NavHost`:

| Destination | Route | Screen | Notes |
|---|---|---|---|
| `LogbookList` | `logbook_list` | `MiniLogbookScreen` | Start destination; passes `onEntryClick` lambda that navigates to detail |
| `GlucoseDetail` | `glucose_detail/{entryId}` | `GlucoseDetailScreen` | `entryId` declared as `NavType.IntType`; Koin injects `GlucoseDetailViewModel` with the correct `SavedStateHandle` automatically |

```
LogbookList ──(tap entry)──► GlucoseDetail
                                   │
                            (back button)
                                   │
                             LogbookList
```

### UI layer

`MiniLogbookScreen` is split into three private composables to minimise recomposition scope:

- **`InputSection`** — receives individual slices (`inputValue`, `unit`, `errorMessage`) and stable lambda references; only recomposes when its own data changes
- **`SummarySection`** — receives `average`, `unit`, and an already-computed `status: BloodGlucoseStatus`; purely declarative with no ViewModel dependency — previewable in isolation
- **`HistorySection`** — receives `LazyPagingItems<GlucoseListEntryUi>` directly; reads `convertedValue`, `unit`, `status` and `timestamp` as plain properties from each item — no lambda calls or `remember` wrappers needed per item; `LazyColumn` with `SwipeToDismissBox` per item; auto-scrolls to the top when a new entry is added

All three private composables follow **state hoisting**: they receive only the data and lambdas they need, making them independently testable and previewable without a real ViewModel.

#### GlucoseDetailScreen

`GlucoseDetailScreen` is a full-screen read-only view for a single glucose entry. It collects `GlucoseDetailViewModel.entry` and renders four `GlucoseDetailCard` composables:

| Card | Content | Icon |
|---|---|---|
| Primary glucose value | Value in **mmol/L** (2 decimal places), colour-coded by status | Star |
| Secondary glucose value | Value in **mg/dL** (1 decimal place), colour-coded by status | Star |
| Date & time | Full weekday + date + time (`EEEE, MMM dd yyyy  •  HH:mm`) | DateRange |
| Status | `In target` / `Ok` / `Out of range`, colour-coded | Info |

While the entry is loading (`entry == null`) a centred `CircularProgressIndicator` is shown. Because all derived values (`valueInMgdl`, `status`, `formattedDate`) are pre-computed inside `GlucoseDetailViewModel` and exposed through `GlucoseEntryUi`, the screen reads them directly as plain properties — no `remember` wrappers or ViewModel function calls are needed in the composable. The screen's `TopAppBar` provides a back-navigation arrow that calls `onNavigateBack`.

---

## Key Technical Decisions

### Recomposition optimisation

| Technique | Where | Why |
|---|---|---|
| `remember`-wrapped `onDeleteRequest` lambda | `MiniLogbookScreen` | The lambda captures coroutine scope and snackbar state; wrapping it in `remember` gives it a stable reference so `HistorySection` skips recomposition when nothing else changed |
| `remember(average, unit)` for `summaryStatus` eliminated — `status` now lives in `GlucoseState` | `MiniLogbookScreen` → `GlucoseViewModel` | Status is computed once inside `combine()` and delivered as a plain property; no per-recomposition derivation needed in the screen |
| `PagingData<GlucoseListEntryUi>` pre-mapped in ViewModel | `GlucoseViewModel` / `HistorySection` | `convertedValue` and `status` per item are computed once in the ViewModel when the page is loaded or the unit changes; composables read stable values directly — eliminates the `remember(entry.valueInMmol, unit)` / `remember(convertedValue, unit)` wrappers that were needed per item |
| `derivedStateOf` for `isDismissed` | Each `SwipeToDismissBox` item | Converts continuous `dismissState.currentValue` reads into a boolean that only invalidates downstream computations when the settled state actually flips |
| `snapshotFlow + filter` instead of `LaunchedEffect(currentValue)` | `HistorySection` item | Fires only once when the swipe fully settles to `EndToStart`, not on every drag-offset change |
| `Animatable + drawBehind` instead of `animateColorAsState` | `SwipeToDismissBox` background | Color animation runs entirely in the draw phase — zero recompositions per frame during the swipe |
| `LaunchedEffect(itemCount)` + `rememberSaveable` guard | `HistorySection` | Scrolls the list to the top only when `itemCount` strictly increases (new entry added) **and** the list is scrolled down — skips the initial load emission and avoids unnecessary scroll calls |
| `remember(accentColor)` for `resolvedLabelStyle` | `GlucoseInputField` | `TextStyle.copy()` allocates a new object on every call; memoising it prevents a fresh allocation on every keystroke-triggered recomposition. `label` and `supportingText` slot lambdas are defined inline — Compose does not use lambda identity to skip recomposition of `@Composable` slot parameters, so `remember`-wrapping them provides no benefit |

### Swipe-to-delete + Undo

The delete flow is designed to avoid full-screen recomposition:
1. Each item's `LaunchedEffect` (via `snapshotFlow`) calls `onDeleteRequest` with a `Pair<GlucoseListEntryUi, suspend () -> Unit>` — the lambda captures `dismissState::reset` so the item can animate back without any state hoisting
2. `onDeleteRequest` (held at screen scope) launches a coroutine that shows the Snackbar
3. On **Undo**: `dismissState.reset()` is called via the captured lambda — the item animates back
4. On **dismiss**: `deleteEntry(entry)` is called with the `GlucoseListEntryUi`; the ViewModel uses `entry.id` to call `GlucoseDao.deleteById`

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
    SharedPreferences (ciphertext + IV stored as Base64 strings)
    encrypted by Cipher (AES/GCM/NoPadding) directly via Android Keystore
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
GlucoseService (singleton)
GlucoseViewModel (viewModel)
GlucoseDetailViewModel (viewModel) — SavedStateHandle injected automatically by Koin + Navigation Compose
```

---

## Testing

### Unit tests (`src/test`)

| Test class | Coverage |
|---|---|
| `GlucoseServiceTest` | Validation, mmol↔mg/dL conversion, status thresholds for both units |
| `GlucoseViewModelTest` | Input value updates, error on invalid/negative save, successful save clears input, unit conversion of input value, average calculation, `isLoading` lifecycle, `status` in `GlucoseState` computed via `getGlucoseStatusByUnit`, `deleteEntry` delegates to `glucoseDao.deleteById` with the correct id |
| `GlucoseDetailViewModelTest` | `SavedStateHandle` guard, `entry` emits `null` initially and when DAO returns null, mapping of `GlucoseEntry` → `GlucoseEntryUi` (id, valueInMmol, valueInMgdl via `fromMmol`, status via `getGlucoseStatus`, formattedDate), reactive updates on successive DAO emissions |

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
| `deleteById_removesCorrectEntry` | `deleteById(id)` removes exactly the entry with that id |
| `deleteById_nonExistentId_doesNothing` | `deleteById` with an unknown id is a no-op |
| `deleteById_doesNotAffectOtherEntries` | `deleteById` on the middle entry preserves order of remaining entries |

### Instrumented PassphraseManager tests (`src/androidTest`) — `PassphraseManagerTest`

Uses isolated SharedPreferences and a dedicated Keystore alias so tests never touch the production key:

| Test | What it verifies |
|---|---|
| `getOrCreatePassphrase_returnsConsistency` | Generated passphrase is 32 bytes and identical on subsequent calls |
| `getOrCreatePassphrase_differentPrefs_differentPassphrases` | Two independent prefs names produce independent passphrases |
| `getOrCreatePassphrase_concurrentAccess_returnsSamePassphrase` | Concurrent threads both obtain the same passphrase (no duplicate generation) |

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
| Navigation | Navigation Compose |
| State | `StateFlow` + `collectAsStateWithLifecycle` |
| Database | Room 2.8.4 |
| Pagination | Paging 3 (3.3.6) |
| Encryption | SQLCipher 4.13.0 + Android Keystore (AES-256-GCM via `KeyGenerator` / `Cipher` directly) |
| DI | Koin 4.1.1 |
| Unit tests | JUnit 5 (Jupiter) + Mockito + kotlinx-coroutines-test |
| UI tests | Compose UI Test + AndroidJUnit4 |
| Build | Gradle KTS + KSP |

