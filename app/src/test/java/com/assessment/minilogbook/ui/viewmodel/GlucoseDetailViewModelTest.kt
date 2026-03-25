package com.assessment.minilogbook.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.data.GlucoseEntry
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.domain.service.GlucoseService
import com.assessment.minilogbook.domain.service.IGlucoseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GlucoseDetailViewModelTest {

    private val glucoseDao: GlucoseDao = mock()
    private val glucoseService: IGlucoseService = mock()
    private lateinit var viewModel: GlucoseDetailViewModel
    private val testDispatcher = StandardTestDispatcher()

    companion object {
        private const val ENTRY_ID = 42
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(glucoseDao.getEntryById(ENTRY_ID)).thenReturn(MutableStateFlow(null))
        whenever(glucoseService.fromMmol(any(), any())).thenReturn(0.0)
        whenever(glucoseService.getGlucoseStatus(any())).thenReturn(BloodGlucoseStatus.IN_TARGET)
        viewModel = GlucoseDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("entryId" to ENTRY_ID)),
            glucoseDao = glucoseDao,
            glucoseService = glucoseService
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.collectEntry() = launch {
        viewModel.entry.collect {}
    }

    // ── SavedStateHandle ──────────────────────────────────────────────────────

    @Test
    fun `constructor throws when entryId is missing from SavedStateHandle`() {
        assertThrows(IllegalStateException::class.java) {
            GlucoseDetailViewModel(
                savedStateHandle = SavedStateHandle(),
                glucoseDao = glucoseDao,
                glucoseService = glucoseService
            )
        }
    }

    // ── entry StateFlow ───────────────────────────────────────────────────────

    @Test
    fun `entry emits null as initial value`() = runTest {
        val job = collectEntry()
        advanceUntilIdle()

        assertNull(viewModel.entry.value)

        job.cancel()
    }

    @Test
    fun `entry emits GlucoseEntry when dao returns one`() = runTest {
        val fakeEntry = GlucoseEntry(id = ENTRY_ID, valueInMmol = 5.5)
        whenever(glucoseDao.getEntryById(ENTRY_ID)).thenReturn(flowOf(fakeEntry))

        // Re-create the ViewModel so it picks up the new stub
        viewModel = GlucoseDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("entryId" to ENTRY_ID)),
            glucoseDao = glucoseDao,
            glucoseService = glucoseService
        )

        val job = collectEntry()
        advanceUntilIdle()

        assertEquals(fakeEntry, viewModel.entry.first())

        job.cancel()
    }

    @Test
    fun `entry emits null when dao returns null`() = runTest {
        whenever(glucoseDao.getEntryById(ENTRY_ID)).thenReturn(flowOf(null))

        viewModel = GlucoseDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("entryId" to ENTRY_ID)),
            glucoseDao = glucoseDao,
            glucoseService = glucoseService
        )

        val job = collectEntry()
        advanceUntilIdle()

        assertNull(viewModel.entry.first())

        job.cancel()
    }

    @Test
    fun `entry reacts to successive emissions from dao`() = runTest {
        val flow = MutableStateFlow<GlucoseEntry?>(null)
        whenever(glucoseDao.getEntryById(ENTRY_ID)).thenReturn(flow)

        viewModel = GlucoseDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("entryId" to ENTRY_ID)),
            glucoseDao = glucoseDao,
            glucoseService = glucoseService
        )

        val job = collectEntry()

        flow.emit(null)
        advanceUntilIdle()
        assertNull(viewModel.entry.value)

        val fakeEntry = GlucoseEntry(id = ENTRY_ID, valueInMmol = 7.0)
        flow.emit(fakeEntry)
        advanceUntilIdle()
        assertEquals(fakeEntry, viewModel.entry.value)

        job.cancel()
    }

    // ── convertValue ──────────────────────────────────────────────────────────

    @Test
    fun `convertValue delegates to glucoseService fromMmol with MMOL_L`() {
        val valueInMmol = 5.5
        whenever(glucoseService.fromMmol(eq(valueInMmol), eq(GlucoseUnit.MMOL_L))).thenReturn(5.5)

        val result = viewModel.convertValue(valueInMmol, GlucoseUnit.MMOL_L)

        assertEquals(5.5, result, 0.0001)
        verify(glucoseService).fromMmol(eq(valueInMmol), eq(GlucoseUnit.MMOL_L))
    }

    @Test
    fun `convertValue delegates to glucoseService fromMmol with MG_DL`() {
        val valueInMmol = 5.5
        val expectedMgDl = 5.5 * GlucoseService.CONVERSION_FACTOR
        whenever(glucoseService.fromMmol(eq(valueInMmol), eq(GlucoseUnit.MG_DL))).thenReturn(expectedMgDl)

        val result = viewModel.convertValue(valueInMmol, GlucoseUnit.MG_DL)

        assertEquals(expectedMgDl, result, 0.01)
        verify(glucoseService).fromMmol(eq(valueInMmol), eq(GlucoseUnit.MG_DL))
    }

    @Test
    fun `convertValue returns 0 for zero mmol value`() {
        whenever(glucoseService.fromMmol(eq(0.0), eq(GlucoseUnit.MMOL_L))).thenReturn(0.0)

        val result = viewModel.convertValue(0.0, GlucoseUnit.MMOL_L)

        assertEquals(0.0, result, 0.0001)
        verify(glucoseService).fromMmol(eq(0.0), eq(GlucoseUnit.MMOL_L))
    }

    // ── getStatus ─────────────────────────────────────────────────────────────

    @Test
    fun `getStatus returns IN_TARGET for value in target range`() {
        val valueInMmol = 100.0 / GlucoseService.CONVERSION_FACTOR
        whenever(glucoseService.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.IN_TARGET)

        val result = viewModel.getStatus(valueInMmol)

        assertEquals(BloodGlucoseStatus.IN_TARGET, result)
        verify(glucoseService).getGlucoseStatus(eq(valueInMmol))
    }

    @Test
    fun `getStatus returns OK for value in ok range`() {
        val valueInMmol = 160.0 / GlucoseService.CONVERSION_FACTOR
        whenever(glucoseService.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.OK)

        val result = viewModel.getStatus(valueInMmol)

        assertEquals(BloodGlucoseStatus.OK, result)
        verify(glucoseService).getGlucoseStatus(eq(valueInMmol))
    }

    @Test
    fun `getStatus returns OUT_OF_RANGE for critically high value`() {
        val valueInMmol = 200.0 / GlucoseService.CONVERSION_FACTOR
        whenever(glucoseService.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.OUT_OF_RANGE)

        val result = viewModel.getStatus(valueInMmol)

        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, result)
        verify(glucoseService).getGlucoseStatus(eq(valueInMmol))
    }

    @Test
    fun `getStatus returns OUT_OF_RANGE for critically low value`() {
        val valueInMmol = 50.0 / GlucoseService.CONVERSION_FACTOR
        whenever(glucoseService.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.OUT_OF_RANGE)

        val result = viewModel.getStatus(valueInMmol)

        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, result)
        verify(glucoseService).getGlucoseStatus(eq(valueInMmol))
    }
}

