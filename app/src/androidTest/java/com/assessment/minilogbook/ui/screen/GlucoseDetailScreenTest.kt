package com.assessment.minilogbook.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.assessment.minilogbook.MainActivity
import com.assessment.minilogbook.R
import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.data.GlucoseDatabase
import com.assessment.minilogbook.domain.service.GlucoseService
import com.assessment.minilogbook.domain.service.IGlucoseService
import com.assessment.minilogbook.ui.viewmodel.GlucoseDetailViewModel
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
 * Instrumented UI tests for navigation to/from [GlucoseDetailScreen].
 *
 * The same RuleChain pattern as [MiniLogbookScreenTest] is used:
 * [koinRule] starts Koin (with a fresh in-memory SQLCipher DB) before
 * [composeRule] launches [MainActivity].
 */
@RunWith(AndroidJUnit4::class)
class GlucoseDetailScreenTest {

    private lateinit var db: GlucoseDatabase
    private lateinit var dao: GlucoseDao

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
                        viewModelOf(::GlucoseDetailViewModel)
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

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(koinRule)
        .around(composeRule)

    // ── String resources (resolved at runtime from the Activity context) ──────

    private val strSave get() = composeRule.activity.getString(R.string.action_save)
    private val strEntryDetail get() = composeRule.activity.getString(R.string.title_entry_detail)
    private val strMiniLogbook get() = composeRule.activity.getString(R.string.title_mini_logbook)
    private val strNavigateBack get() = composeRule.activity.getString(R.string.navigate_back)

    private val strUnitMmol get() = composeRule.activity.getString(R.string.unit_mmol_l)

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Types [value] into the glucose input field. */
    private fun enterValue(value: String) {
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput(value)
    }

    /** Taps the Save button and waits for the UI to settle. */
    private fun clickSave() {
        composeRule.onNodeWithText(strSave).performClick()
        composeRule.waitForIdle()
    }

    /**
     * Blocks until an entry card showing [value] (substring match) appears in the list,
     * or the timeout elapses.
     */
    private fun waitForEntry(value: String) {
        composeRule.waitUntil(10_000) {
            composeRule
                .onAllNodesWithText(value, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Clicks the entry card whose testTag matches the formatted value + unit label.
     * The Card composable in EntryItem uses testTag("entry_$formattedValue"), e.g.
     * "entry_5.50 mmol/L". Targeting by testTag ensures the click is received by the
     * clickable Card and not by a child Text node.
     */
    private fun clickEntry(displayValue: String) {
        composeRule
            .onNode(hasTestTag("entry_$displayValue $strUnitMmol"))
            .performClick()
    }

    private fun waitForScreen(title: String) {
        composeRule.waitUntil(10_000) {
            composeRule
                .onAllNodesWithText(title)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Tapping an entry card navigates to the detail screen.
     * Verifies that the "Entry Detail" top-bar title is visible after the tap.
     */
    @Test
    fun tapEntryCard_navigatesToDetailScreen() {
        enterValue("5.5")
        clickSave()
        waitForEntry("5.50")

        clickEntry("5.50")

        waitForScreen(strEntryDetail)

        composeRule.onNodeWithText(strEntryDetail).assertIsDisplayed()
    }

    /**
     * While on the detail screen, tapping the back arrow navigates back to the
     * main list. Verifies that the "Mini Logbook" top-bar title is visible again.
     */
    @Test
    fun tapBackButton_onDetailScreen_navigatesBackToList() {
        enterValue("7.0")
        clickSave()
        waitForEntry("7.00")

        // Navigate to the detail screen
        clickEntry("7.00")

        waitForScreen(strEntryDetail)
        composeRule.onNodeWithText(strEntryDetail).assertIsDisplayed()

        // Tap the back navigation icon
        composeRule.onNodeWithContentDescription(strNavigateBack).performClick()
        waitForScreen(strMiniLogbook)

        composeRule.onNodeWithText(strMiniLogbook).assertIsDisplayed()
    }
}


