package com.assessment.minilogbook.ui.viewmodel

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.data.GlucoseEntry
import com.assessment.minilogbook.data.GlucoseUnit
import com.assessment.minilogbook.domain.service.GlucoseService
import com.assessment.minilogbook.domain.service.IGlucoseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class GlucoseViewModelTest(
    private val inputMmol: String,
    private val expectedMgDl: String
) {

    private val glucoseDao: GlucoseDao = mock()
    private val converter: IGlucoseService = GlucoseService()
    private lateinit var viewModel: GlucoseViewModel
    private val averageFlow = MutableStateFlow<Double?>(null)

    private val testDispatcher = StandardTestDispatcher()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} mmol/L -> {1} mg/dL")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf("1.0", "18.0182"),
            arrayOf("5.0", "90.0910"),
            arrayOf("10.0", "180.1820"),
            arrayOf("0.0", "0.0"),
            arrayOf("5.5", "99.1001")
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(glucoseDao.getAverageValue()).thenReturn(averageFlow)
        whenever(glucoseDao.getAllEntries()).thenReturn(FakePagingSource())
        viewModel = GlucoseViewModel(glucoseDao, converter)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Helper to activate the stateIn flow and get the first emission.
    // stateIn with WhileSubscribed requires an active collector to start upstream.
    // We launch a collector inside the test scope to trigger the flow subscription.
    private fun TestScope.collectState() = launch {
        viewModel.state.collect {}
    }

    @Test
    fun `onInputValueChanged updates inputValue and clears error`() = runTest {
        val job = collectState()

        viewModel.onInputValueChanged("5.5")
        advanceUntilIdle()

        assertEquals("5.5", viewModel.inputValue.first())
        assertNull(viewModel.errorMessage.first())

        job.cancel()
    }

    @Test
    fun `saveEntry with empty input sets error message`() = runTest {
        val job = collectState()

        viewModel.onInputValueChanged("")
        viewModel.saveEntry()
        advanceUntilIdle()

        assertEquals("Please enter a valid value >= 0", viewModel.errorMessage.first())
        verify(glucoseDao, never()).insert(any())

        job.cancel()
    }

    @Test
    fun `saveEntry with invalid number input sets error message`() = runTest {
        val job = collectState()

        viewModel.onInputValueChanged("abc")
        viewModel.saveEntry()
        advanceUntilIdle()

        assertEquals("Please enter a valid value >= 0", viewModel.errorMessage.first())
        verify(glucoseDao, never()).insert(any())

        job.cancel()
    }

    @Test
    fun `saveEntry with negative input sets error message`() = runTest {
        val job = collectState()

        viewModel.onInputValueChanged("-1.0")
        viewModel.saveEntry()
        advanceUntilIdle()

        assertEquals("Please enter a valid value >= 0", viewModel.errorMessage.first())
        verify(glucoseDao, never()).insert(any())

        job.cancel()
    }

    @Test
    fun `saveEntry with valid input inserts into dao and clears input`() = runTest {
        val job = collectState()

        viewModel.onInputValueChanged("5.0")
        viewModel.saveEntry()
        advanceUntilIdle()

        verify(glucoseDao).insert(any())
        assertEquals("", viewModel.inputValue.first())
        assertNull(viewModel.errorMessage.first())

        job.cancel()
    }

    @Test
    fun `onUnitChanged converts current input value parameterized`() = runTest {
        val job = collectState()

        viewModel.onInputValueChanged(inputMmol)
        viewModel.onUnitChanged(GlucoseUnit.MG_DL)
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(GlucoseUnit.MG_DL, state.unit)
        assertEquals(expectedMgDl.toDouble(), viewModel.inputValue.first().toDouble(), 0.0001)

        job.cancel()
    }

    @Test
    fun `state average returns 0 when db average is null`() = runTest {
        val job = collectState()
        averageFlow.emit(null)
        advanceUntilIdle()

        val state = viewModel.state.first()
        assertEquals(0.0, state.average, 0.001)

        job.cancel()
    }

    @Test
    fun `state average reflects db value`() = runTest {
        val job = collectState()

        // Simulate DB returning average of 10.0 mmol/L
        averageFlow.emit(10.0)
        advanceUntilIdle()

        var state = viewModel.state.first()
        // Default unit is MMOL_L, so average should be 10.0
        assertEquals(10.0, state.average, 0.001)

        // Switch unit to MG_DL
        viewModel.onUnitChanged(GlucoseUnit.MG_DL)
        advanceUntilIdle()

        state = viewModel.state.first()
        // 10.0 * 18.0182 = 180.182
        assertEquals(180.182, state.average, 0.001)

        job.cancel()
    }

    @Test
    fun `isLoading is initially true and becomes false after average emission`() = runTest {

        // Initial state
        assertEquals(true, viewModel.state.value.isLoading)

        // Start collecting
        val job = collectState()

        // Emit a value from DB
        averageFlow.emit(5.0)
        advanceUntilIdle()

        // State should now be loaded
        assertEquals(false, viewModel.state.first().isLoading)

        job.cancel()
    }

    // --- getGlucoseStatus (value in mmol/L) ---

    @Test
    fun `getGlucoseStatus returns IN_TARGET for value in 90-140 mgDl range`() {
        // 100 mg/dL → ~5.55 mmol/L
        val valueInMmol = 100.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.IN_TARGET, viewModel.getGlucoseStatus(valueInMmol))
    }

    @Test
    fun `getGlucoseStatus returns OK for value in low ok range`() {
        // 80 mg/dL → ~4.44 mmol/L
        val valueInMmol = 80.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.OK, viewModel.getGlucoseStatus(valueInMmol))
    }

    @Test
    fun `getGlucoseStatus returns OK for value in high ok range`() {
        // 160 mg/dL → ~8.88 mmol/L
        val valueInMmol = 160.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.OK, viewModel.getGlucoseStatus(valueInMmol))
    }

    @Test
    fun `getGlucoseStatus returns OUT_OF_RANGE for value below 70 mgDl`() {
        // 50 mg/dL → ~2.77 mmol/L
        val valueInMmol = 50.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, viewModel.getGlucoseStatus(valueInMmol))
    }

    @Test
    fun `getGlucoseStatus returns OUT_OF_RANGE for value above 180 mgDl`() {
        // 200 mg/dL → ~11.10 mmol/L
        val valueInMmol = 200.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, viewModel.getGlucoseStatus(valueInMmol))
    }

    // --- getGlucoseStatusByUnit (value + explicit unit) ---

    @Test
    fun `getGlucoseStatusByUnit returns IN_TARGET for mgDl value in target range`() {
        assertEquals(BloodGlucoseStatus.IN_TARGET, viewModel.getGlucoseStatusByUnit(100.0, GlucoseUnit.MG_DL))
    }

    @Test
    fun `getGlucoseStatusByUnit returns IN_TARGET for mmolL value in target range`() {
        val valueInMmol = 100.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.IN_TARGET, viewModel.getGlucoseStatusByUnit(valueInMmol, GlucoseUnit.MMOL_L))
    }

    @Test
    fun `getGlucoseStatusByUnit returns OK for mgDl value in ok range`() {
        assertEquals(BloodGlucoseStatus.OK, viewModel.getGlucoseStatusByUnit(160.0, GlucoseUnit.MG_DL))
    }

    @Test
    fun `getGlucoseStatusByUnit returns OK for mmolL value in ok range`() {
        val valueInMmol = 160.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.OK, viewModel.getGlucoseStatusByUnit(valueInMmol, GlucoseUnit.MMOL_L))
    }

    @Test
    fun `getGlucoseStatusByUnit returns OUT_OF_RANGE for mgDl value above 180`() {
        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, viewModel.getGlucoseStatusByUnit(200.0, GlucoseUnit.MG_DL))
    }

    @Test
    fun `getGlucoseStatusByUnit returns OUT_OF_RANGE for mmolL value below 70 mgDl equivalent`() {
        val valueInMmol = 50.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, viewModel.getGlucoseStatusByUnit(valueInMmol, GlucoseUnit.MMOL_L))
    }
}

class FakePagingSource : PagingSource<Int, GlucoseEntry>() {
    override fun getRefreshKey(state: PagingState<Int, GlucoseEntry>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GlucoseEntry> {
        return LoadResult.Page(emptyList(), null, null)
    }
}
