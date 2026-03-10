package com.assessment.minilogbook.ui.viewmodel

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.data.GlucoseEntry
import com.assessment.minilogbook.data.GlucoseUnit
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.domain.service.GlucoseService
import com.assessment.minilogbook.domain.service.IGlucoseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
class GlucoseViewModelTest {

    private val glucoseDao: GlucoseDao = mock()
    private val converter: IGlucoseService = mock()
    private lateinit var viewModel: GlucoseViewModel
    private val averageFlow = MutableStateFlow<Double?>(null)
    private val testDispatcher = StandardTestDispatcher()

    companion object {
        @JvmStatic
        fun unitConversionData(): Stream<Array<Any>> = Stream.of(
            arrayOf("1.0", "18.0182"),
            arrayOf("5.0", "90.0910"),
            arrayOf("10.0", "180.1820"),
            arrayOf("0.0", "0.0"),
            arrayOf("5.5", "99.1001")
        )
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(glucoseDao.getAverageValue()).thenReturn(averageFlow)
        whenever(glucoseDao.getAllEntries()).thenReturn(FakePagingSource())
        whenever(converter.validateValue(any())).thenReturn(true)
        whenever(converter.toMmolIfValid(org.mockito.kotlin.anyOrNull(), any())).thenReturn(null)
        whenever(converter.convertValue(any(), any(), any())).thenReturn(0.0)
        whenever(converter.fromMmol(any(), any())).thenReturn(0.0)
        whenever(converter.getGlucoseStatus(any())).thenReturn(BloodGlucoseStatus.IN_TARGET)
        whenever(converter.getGlucoseStatusByUnit(any(), any())).thenReturn(BloodGlucoseStatus.IN_TARGET)
        viewModel = GlucoseViewModel(glucoseDao, converter)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.collectState() = launch {
        viewModel.glucoseState.collect {}
    }

    @Test
    fun `onInputValueChanged updates inputValue and clears error`() = runTest {
        val job = collectState()

        viewModel.onInputValueChanged("5.5")
        advanceUntilIdle()

        assertEquals("5.5", viewModel.inputValue.first())
        assertFalse(viewModel.displayErrorMessage.first())

        job.cancel()
    }

    @Test
    fun `saveEntry with empty input sets error message`() = runTest {
        val job = collectState()

        viewModel.onInputValueChanged("")
        viewModel.saveEntry()
        advanceUntilIdle()

        assertTrue(viewModel.displayErrorMessage.value)
        verify(converter).toMmolIfValid(org.mockito.kotlin.isNull(), eq(GlucoseUnit.MMOL_L))
        verify(glucoseDao, never()).insert(any())

        job.cancel()
    }

    @Test
    fun `saveEntry with invalid number input sets error message`() = runTest {
        val job = collectState()

        viewModel.onInputValueChanged("abc")
        viewModel.saveEntry()
        advanceUntilIdle()

        assertTrue(viewModel.displayErrorMessage.value)
        verify(converter).toMmolIfValid(org.mockito.kotlin.isNull(), eq(GlucoseUnit.MMOL_L))
        verify(glucoseDao, never()).insert(any())

        job.cancel()
    }

    @Test
    fun `saveEntry with negative input sets error message`() = runTest {
        val job = collectState()

        viewModel.onInputValueChanged("-1.0")
        viewModel.saveEntry()
        advanceUntilIdle()

        assertTrue(viewModel.displayErrorMessage.value)
        verify(converter).toMmolIfValid(eq(-1.0), eq(GlucoseUnit.MMOL_L))
        verify(glucoseDao, never()).insert(any())

        job.cancel()
    }

    @Test
    fun `saveEntry with valid input inserts into dao and clears input`() = runTest {
        val job = collectState()

        whenever(converter.toMmolIfValid(eq(5.0), eq(GlucoseUnit.MMOL_L))).thenReturn(5.0)

        viewModel.onInputValueChanged("5.0")
        viewModel.saveEntry()
        advanceUntilIdle()

        verify(converter).toMmolIfValid(eq(5.0), eq(GlucoseUnit.MMOL_L))
        verify(glucoseDao).insert(any())
        assertEquals("", viewModel.inputValue.first())
        assertFalse(viewModel.displayErrorMessage.value)

        job.cancel()
    }

    @ParameterizedTest(name = "{0} mmol/L -> {1} mg/dL")
    @MethodSource("unitConversionData")
    fun `onUnitChanged converts current input value`(inputMmol: String, expectedMgDl: String) = runTest {
        val job = collectState()

        val inputDouble = inputMmol.toDouble()
        val expectedDouble = expectedMgDl.toDouble()

        whenever(
            converter.convertValue(eq(inputDouble), eq(GlucoseUnit.MMOL_L), eq(GlucoseUnit.MG_DL))
        ).thenReturn(expectedDouble)

        viewModel.onInputValueChanged(inputMmol)
        viewModel.onUnitChanged(GlucoseUnit.MG_DL)
        advanceUntilIdle()

        val state = viewModel.glucoseState.first()
        assertEquals(GlucoseUnit.MG_DL, state.unit)
        verify(converter).convertValue(eq(inputDouble), eq(GlucoseUnit.MMOL_L), eq(GlucoseUnit.MG_DL))
        assertEquals(expectedDouble, viewModel.inputValue.first().toDouble(), 0.0001)

        job.cancel()
    }

    @Test
    fun `onUnitChanged with non-numeric input does not call convertValue and keeps original input`() = runTest {
        val job = collectState()
        val nonNumericInput = "abc"
        viewModel.onInputValueChanged(nonNumericInput)

        viewModel.onUnitChanged(GlucoseUnit.MG_DL)
        advanceUntilIdle()

        verify(converter, never()).convertValue(any(), any(), any())
        assertEquals(nonNumericInput, viewModel.inputValue.first())

        job.cancel()
    }

    @Test
    fun `onUnitChanged with empty input does not call convertValue and keeps empty input`() = runTest {
        val job = collectState()
        viewModel.onInputValueChanged("")

        viewModel.onUnitChanged(GlucoseUnit.MG_DL)
        advanceUntilIdle()

        verify(converter, never()).convertValue(any(), any(), any())
        assertEquals("", viewModel.inputValue.first())

        job.cancel()
    }

    @Test
    fun `state average returns 0 when db average is null`() = runTest {
        val job = collectState()
        averageFlow.emit(null)
        advanceUntilIdle()

        val state = viewModel.glucoseState.first()
        assertEquals(0.0, state.average, 0.001)

        job.cancel()
    }

    @Test
    fun `state average reflects db value`() = runTest {
        val job = collectState()

        whenever(converter.fromMmol(eq(10.0), eq(GlucoseUnit.MMOL_L))).thenReturn(10.0)
        whenever(converter.fromMmol(eq(10.0), eq(GlucoseUnit.MG_DL))).thenReturn(180.182)

        averageFlow.emit(10.0)
        advanceUntilIdle()

        var state = viewModel.glucoseState.first()
        assertEquals(10.0, state.average, 0.001)
        verify(converter).fromMmol(eq(10.0), eq(GlucoseUnit.MMOL_L))

        viewModel.onUnitChanged(GlucoseUnit.MG_DL)
        advanceUntilIdle()

        state = viewModel.glucoseState.first()
        assertEquals(180.182, state.average, 0.001)
        verify(converter).fromMmol(eq(10.0), eq(GlucoseUnit.MG_DL))

        job.cancel()
    }

    @Test
    fun `isLoading is initially true and becomes false after average emission`() = runTest {
        assertEquals(true, viewModel.glucoseState.value.isLoading)

        val job = collectState()

        averageFlow.emit(5.0)
        advanceUntilIdle()

        assertEquals(false, viewModel.glucoseState.first().isLoading)

        job.cancel()
    }

    // --- getGlucoseStatus ---

    @Test
    fun `getGlucoseStatus returns IN_TARGET for value in 90-140 mgDl range`() {
        val valueInMmol = 100.0 / GlucoseService.CONVERSION_FACTOR
        whenever(converter.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.IN_TARGET)

        assertEquals(BloodGlucoseStatus.IN_TARGET, viewModel.getGlucoseStatus(valueInMmol))
        verify(converter).getGlucoseStatus(eq(valueInMmol))
    }

    @Test
    fun `getGlucoseStatus returns OK for value in low ok range`() {
        val valueInMmol = 80.0 / GlucoseService.CONVERSION_FACTOR
        whenever(converter.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.OK)

        assertEquals(BloodGlucoseStatus.OK, viewModel.getGlucoseStatus(valueInMmol))
        verify(converter).getGlucoseStatus(eq(valueInMmol))
    }

    @Test
    fun `getGlucoseStatus returns OK for value in high ok range`() {
        val valueInMmol = 160.0 / GlucoseService.CONVERSION_FACTOR
        whenever(converter.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.OK)

        assertEquals(BloodGlucoseStatus.OK, viewModel.getGlucoseStatus(valueInMmol))
        verify(converter).getGlucoseStatus(eq(valueInMmol))
    }

    @Test
    fun `getGlucoseStatus returns OUT_OF_RANGE for value below 70 mgDl`() {
        val valueInMmol = 50.0 / GlucoseService.CONVERSION_FACTOR
        whenever(converter.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.OUT_OF_RANGE)

        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, viewModel.getGlucoseStatus(valueInMmol))
        verify(converter).getGlucoseStatus(eq(valueInMmol))
    }

    @Test
    fun `getGlucoseStatus returns OUT_OF_RANGE for value above 180 mgDl`() {
        val valueInMmol = 200.0 / GlucoseService.CONVERSION_FACTOR
        whenever(converter.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.OUT_OF_RANGE)

        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, viewModel.getGlucoseStatus(valueInMmol))
        verify(converter).getGlucoseStatus(eq(valueInMmol))
    }

    // --- getGlucoseStatusByUnit ---

    @Test
    fun `getGlucoseStatusByUnit returns IN_TARGET for mgDl value in target range`() {
        whenever(converter.getGlucoseStatusByUnit(eq(100.0), eq(GlucoseUnit.MG_DL))).thenReturn(BloodGlucoseStatus.IN_TARGET)

        assertEquals(BloodGlucoseStatus.IN_TARGET, viewModel.getGlucoseStatusByUnit(100.0, GlucoseUnit.MG_DL))
        verify(converter).getGlucoseStatusByUnit(eq(100.0), eq(GlucoseUnit.MG_DL))
    }

    @Test
    fun `getGlucoseStatusByUnit returns IN_TARGET for mmolL value in target range`() {
        val valueInMmol = 100.0 / GlucoseService.CONVERSION_FACTOR
        whenever(converter.getGlucoseStatusByUnit(eq(valueInMmol), eq(GlucoseUnit.MMOL_L))).thenReturn(BloodGlucoseStatus.IN_TARGET)

        assertEquals(BloodGlucoseStatus.IN_TARGET, viewModel.getGlucoseStatusByUnit(valueInMmol, GlucoseUnit.MMOL_L))
        verify(converter).getGlucoseStatusByUnit(eq(valueInMmol), eq(GlucoseUnit.MMOL_L))
    }

    @Test
    fun `getGlucoseStatusByUnit returns OK for mgDl value in ok range`() {
        whenever(converter.getGlucoseStatusByUnit(eq(160.0), eq(GlucoseUnit.MG_DL))).thenReturn(BloodGlucoseStatus.OK)

        assertEquals(BloodGlucoseStatus.OK, viewModel.getGlucoseStatusByUnit(160.0, GlucoseUnit.MG_DL))
        verify(converter).getGlucoseStatusByUnit(eq(160.0), eq(GlucoseUnit.MG_DL))
    }

    @Test
    fun `getGlucoseStatusByUnit returns OK for mmolL value in ok range`() {
        val valueInMmol = 160.0 / GlucoseService.CONVERSION_FACTOR
        whenever(converter.getGlucoseStatusByUnit(eq(valueInMmol), eq(GlucoseUnit.MMOL_L))).thenReturn(BloodGlucoseStatus.OK)

        assertEquals(BloodGlucoseStatus.OK, viewModel.getGlucoseStatusByUnit(valueInMmol, GlucoseUnit.MMOL_L))
        verify(converter).getGlucoseStatusByUnit(eq(valueInMmol), eq(GlucoseUnit.MMOL_L))
    }

    @Test
    fun `getGlucoseStatusByUnit returns OUT_OF_RANGE for mgDl value above 180`() {
        whenever(converter.getGlucoseStatusByUnit(eq(200.0), eq(GlucoseUnit.MG_DL))).thenReturn(BloodGlucoseStatus.OUT_OF_RANGE)

        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, viewModel.getGlucoseStatusByUnit(200.0, GlucoseUnit.MG_DL))
        verify(converter).getGlucoseStatusByUnit(eq(200.0), eq(GlucoseUnit.MG_DL))
    }

    @Test
    fun `getGlucoseStatusByUnit returns OUT_OF_RANGE for mmolL value below 70 mgDl equivalent`() {
        val valueInMmol = 50.0 / GlucoseService.CONVERSION_FACTOR
        whenever(converter.getGlucoseStatusByUnit(eq(valueInMmol), eq(GlucoseUnit.MMOL_L))).thenReturn(BloodGlucoseStatus.OUT_OF_RANGE)

        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, viewModel.getGlucoseStatusByUnit(valueInMmol, GlucoseUnit.MMOL_L))
        verify(converter).getGlucoseStatusByUnit(eq(valueInMmol), eq(GlucoseUnit.MMOL_L))
    }
}

class FakePagingSource : PagingSource<Int, GlucoseEntry>() {
    override fun getRefreshKey(state: PagingState<Int, GlucoseEntry>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GlucoseEntry> {
        return LoadResult.Page(emptyList(), null, null)
    }
}
