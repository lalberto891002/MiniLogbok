package com.assessment.minilogbook.ui.screen

import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.assessment.minilogbook.MainActivity
import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.data.GlucoseDatabase
import com.assessment.minilogbook.domain.service.GlucoseService
import com.assessment.minilogbook.domain.service.IGlucoseService
import com.assessment.minilogbook.ui.viewmodel.GlucoseViewModel
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Instrumented UI tests for [MiniLogbookScreen].
 *
 * Koin must be started BEFORE the Activity launches.
 * [RuleChain] guarantees [koinRule] runs first, then [composeRule],
 * so the ViewModel can be resolved when MainActivity.onCreate() fires.
 */
@RunWith(AndroidJUnit4::class)
class MiniLogbookScreenTest {

    private lateinit var db: GlucoseDatabase
    private lateinit var dao: GlucoseDao

    // Starts Koin with an in-memory SQLCipher DB before the Activity is created
    private val koinRule = object : TestWatcher() {
        override fun starting(description: Description) {
            System.loadLibrary("sqlcipher")

            db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                GlucoseDatabase::class.java
            )
                .openHelperFactory(SupportOpenHelperFactory(ByteArray(0)))
                .build()
            dao = db.glucoseDao()

            startKoin {
                androidContext(ApplicationProvider.getApplicationContext())
                modules(
                    module {
                        single<GlucoseDao> { dao }
                        singleOf(::GlucoseService) bind IGlucoseService::class
                        viewModelOf(::GlucoseViewModel)
                    }
                )
            }
        }

        override fun finished(description: Description) {
            stopKoin()
            db.close()
        }
    }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    // koinRule MUST run before composeRule — RuleChain enforces this order
    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(koinRule)
        .around(composeRule)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun enterValue(value: String) {
        composeRule
            .onNode(hasSetTextAction())
            .performTextClearance()
        composeRule
            .onNode(hasSetTextAction())
            .performTextInput(value)
    }

    private fun clickSave() {
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()
    }

    private fun waitForEntry(value: String) {
        composeRule.waitUntil(5_000) {
            composeRule
                .onAllNodesWithText(value, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Swipes left the entry card whose test tag contains [value].
     * Targets the Card container directly via testTag so the
     * SwipeToDismissBox gesture detector always receives the full swipe.
     */
    private fun swipeEntry(value: String) {
        composeRule
            .onAllNodes(hasTestTag("entry_$value mmol/L"))
            .onFirst()
            .performTouchInput {
                swipe(
                    start = androidx.compose.ui.geometry.Offset(right - 10f, centerY),
                    end   = androidx.compose.ui.geometry.Offset(left  + 10f, centerY),
                    durationMillis = 600
                )
            }
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    fun enterValidValue_saveIt_appearsInList() {
        enterValue("5.5")
        clickSave()

        waitForEntry("5.50")
        composeRule.onAllNodesWithText("5.50", substring = true).assertAny(hasText("5.50 mmol/L"))
    }

    @Test
    fun enterInvalidValue_showsError_doesNotAppearInList() {
        enterValue("abc")
        clickSave()

        composeRule.onNodeWithText("Please enter a valid value >= 0").assertIsDisplayed()
        // No entry card should have been created — testTag nodes are only on Cards
        composeRule.onAllNodes(hasTestTag("entry_abc mmol/L")).assertCountEquals(0)
    }

    @Test
    fun enterNegativeValue_showsError() {
        enterValue("-1")
        clickSave()

        composeRule.onNodeWithText("Please enter a valid value >= 0").assertIsDisplayed()
    }

    @Test
    fun saveEntry_inputFieldClears() {
        enterValue("4.2")
        clickSave()

        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).assert(hasText(""))
    }

    @Test
    fun saveEntry_averageUpdates() {
        enterValue("8.0")
        clickSave()

        waitForEntry("8.00")
        composeRule.onNodeWithText("Average Blood Glucose").assertIsDisplayed()
    }

    @Test
    fun deleteEntry_viaIconButton_removesFromList() {
        enterValue("6.0")
        clickSave()
        waitForEntry("6.00")

        composeRule
            .onAllNodesWithContentDescription("Entry removed")
            .onFirst()
            .performClick()

        composeRule.waitUntil(5_000) {
            composeRule
                .onAllNodesWithText("6.00", substring = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun deleteEntry_viaIconButton_snackbarShown() {
        enterValue("7.0")
        clickSave()
        waitForEntry("7.00")

        composeRule
            .onAllNodesWithContentDescription("Entry removed")
            .onFirst()
            .performClick()

        composeRule.onNodeWithText("Entry removed").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").assertIsDisplayed()
    }

    @Test
    fun deleteEntry_viaSwipe_removesFromList() {
        enterValue("5.0")
        clickSave()
        waitForEntry("5.00")

        swipeEntry("5.00")

        composeRule.waitUntil(5_000) {
            composeRule
                .onAllNodesWithText("5.00", substring = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun deleteEntry_viaSwipe_undoRestoresItem() {
        enterValue("9.0")
        clickSave()
        waitForEntry("9.00")

        swipeEntry("9.00")

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Undo")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Undo").performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("9.00", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("9.00", substring = true)
            .assertAny(hasText("9.00 mmol/L"))
    }

    @Test
    fun saveMultipleEntries_allAppearInList() {
        listOf("3.5", "5.5", "7.5").forEach { value ->
            enterValue(value)
            clickSave()
        }

        waitForEntry("3.50")
        composeRule.onAllNodesWithText("3.50", substring = true).assertAny(hasText("3.50 mmol/L"))
        composeRule.onAllNodesWithText("5.50", substring = true).assertAny(hasText("5.50 mmol/L"))
        composeRule.onAllNodesWithText("7.50", substring = true).assertAny(hasText("7.50 mmol/L"))
    }

    @Test
    fun configurationChange_dataIsPreserved() {
        enterValue("6.6")
        clickSave()
        waitForEntry("6.60")

        composeRule.activity.requestedOrientation = Configuration.ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("6.60", substring = true).assertAny(hasText("6.60 mmol/L"))

        composeRule.activity.requestedOrientation = Configuration.ORIENTATION_PORTRAIT
    }

    @Test
    fun previousEntriesLabel_isAlwaysVisible() {
        composeRule.onNodeWithText("Previous Entries").assertIsDisplayed()
    }

    @Test
    fun unitSelector_switchToMgDl_convertedValueShown() {
        enterValue("5.0")
        clickSave()
        waitForEntry("5.00")

        composeRule.onNodeWithText("mg/dL").performClick()

        // 5 mmol/L * 18.0182 ≈ 90.09 mg/dL
        waitForEntry("90.09")
        composeRule.onAllNodes(hasTestTag("entry_90.09 mg/dL")).assertCountEquals(1)
    }

    @Test
    fun unitSelector_switchUnit_allEntriesShowCorrectUnit() {
        listOf("4.0", "6.0", "8.0").forEach { value ->
            enterValue(value)
            clickSave()
        }
        waitForEntry("4.00")
        waitForEntry("6.00")
        waitForEntry("8.00")

        // Use testTags to count only entry cards, not the unit selector buttons or input suffix
        composeRule.onAllNodes(hasTestTag("entry_4.00 mmol/L")).assertCountEquals(1)
        composeRule.onAllNodes(hasTestTag("entry_6.00 mmol/L")).assertCountEquals(1)
        composeRule.onAllNodes(hasTestTag("entry_8.00 mmol/L")).assertCountEquals(1)

        // Switch to mg/dL — 4→72.07, 6→108.11, 8→144.15
        composeRule.onNodeWithText("mg/dL").performClick()

        waitForEntry("72.07")
        waitForEntry("108.11")
        waitForEntry("144.15")

        composeRule.onAllNodes(hasTestTag("entry_72.07 mg/dL")).assertCountEquals(1)
        composeRule.onAllNodes(hasTestTag("entry_108.11 mg/dL")).assertCountEquals(1)
        composeRule.onAllNodes(hasTestTag("entry_144.15 mg/dL")).assertCountEquals(1)

        // No mmol/L entries should remain
        composeRule.onAllNodes(hasTestTag("entry_4.00 mmol/L")).assertCountEquals(0)
        composeRule.onAllNodes(hasTestTag("entry_6.00 mmol/L")).assertCountEquals(0)
        composeRule.onAllNodes(hasTestTag("entry_8.00 mmol/L")).assertCountEquals(0)

        // Switch back to mmol/L — verify round-trip
        composeRule.onNodeWithText("mmol/L").performClick()

        waitForEntry("4.00")
        waitForEntry("6.00")
        waitForEntry("8.00")

        composeRule.onAllNodes(hasTestTag("entry_4.00 mmol/L")).assertCountEquals(1)
        composeRule.onAllNodes(hasTestTag("entry_6.00 mmol/L")).assertCountEquals(1)
        composeRule.onAllNodes(hasTestTag("entry_8.00 mmol/L")).assertCountEquals(1)

        composeRule.onAllNodes(hasTestTag("entry_72.07 mg/dL")).assertCountEquals(0)
        composeRule.onAllNodes(hasTestTag("entry_108.11 mg/dL")).assertCountEquals(0)
        composeRule.onAllNodes(hasTestTag("entry_144.15 mg/dL")).assertCountEquals(0)
    }
}

