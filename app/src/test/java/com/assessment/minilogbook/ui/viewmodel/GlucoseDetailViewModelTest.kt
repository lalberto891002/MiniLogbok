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
    fun `entry emits GlucoseEntryUi when dao returns a GlucoseEntry`() = runTest {
        val fakeEntry = GlucoseEntry(id = ENTRY_ID, valueInMmol = 5.5)
        val expectedMgdl = 5.5 * GlucoseService.CONVERSION_FACTOR
        whenever(glucoseDao.getEntryById(ENTRY_ID)).thenReturn(flowOf(fakeEntry))
        whenever(glucoseService.fromMmol(eq(5.5), eq(GlucoseUnit.MG_DL))).thenReturn(expectedMgdl)
        whenever(glucoseService.getGlucoseStatus(eq(5.5))).thenReturn(BloodGlucoseStatus.IN_TARGET)

        viewModel = GlucoseDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("entryId" to ENTRY_ID)),
            glucoseDao = glucoseDao,
            glucoseService = glucoseService
        )

        val job = collectEntry()
        advanceUntilIdle()

        val ui = viewModel.entry.first()
        assertEquals(ENTRY_ID, ui?.id)
        assertEquals(5.5, ui?.valueInMmol)
        assertEquals(expectedMgdl, ui?.valueInMgdl)
        assertEquals(BloodGlucoseStatus.IN_TARGET, ui?.status)

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
        whenever(glucoseService.fromMmol(eq(7.0), eq(GlucoseUnit.MG_DL))).thenReturn(7.0 * GlucoseService.CONVERSION_FACTOR)
        whenever(glucoseService.getGlucoseStatus(eq(7.0))).thenReturn(BloodGlucoseStatus.IN_TARGET)
        flow.emit(fakeEntry)
        advanceUntilIdle()
        assertEquals(ENTRY_ID, viewModel.entry.value?.id)
        assertEquals(7.0, viewModel.entry.value?.valueInMmol)

        job.cancel()
    }

    // ── mapping: valueInMgdl and status ──────────────────────────────────────

    @Test
    fun `entry maps valueInMgdl using glucoseService fromMmol`() = runTest {
        val valueInMmol = 5.5
        val expectedMgdl = valueInMmol * GlucoseService.CONVERSION_FACTOR
        whenever(glucoseDao.getEntryById(ENTRY_ID)).thenReturn(flowOf(GlucoseEntry(id = ENTRY_ID, valueInMmol = valueInMmol)))
        whenever(glucoseService.fromMmol(eq(valueInMmol), eq(GlucoseUnit.MG_DL))).thenReturn(expectedMgdl)
        whenever(glucoseService.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.IN_TARGET)

        viewModel = GlucoseDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("entryId" to ENTRY_ID)),
            glucoseDao = glucoseDao,
            glucoseService = glucoseService
        )

        val job = collectEntry()
        advanceUntilIdle()

        assertEquals(expectedMgdl, viewModel.entry.value?.valueInMgdl)
        verify(glucoseService).fromMmol(eq(valueInMmol), eq(GlucoseUnit.MG_DL))

        job.cancel()
    }

    @Test
    fun `entry maps status using glucoseService getGlucoseStatus`() = runTest {
        val valueInMmol = 160.0 / GlucoseService.CONVERSION_FACTOR
        whenever(glucoseDao.getEntryById(ENTRY_ID)).thenReturn(flowOf(GlucoseEntry(id = ENTRY_ID, valueInMmol = valueInMmol)))
        whenever(glucoseService.fromMmol(any(), any())).thenReturn(0.0)
        whenever(glucoseService.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.OK)

        viewModel = GlucoseDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("entryId" to ENTRY_ID)),
            glucoseDao = glucoseDao,
            glucoseService = glucoseService
        )

        val job = collectEntry()
        advanceUntilIdle()

        assertEquals(BloodGlucoseStatus.OK, viewModel.entry.value?.status)
        verify(glucoseService).getGlucoseStatus(eq(valueInMmol))

        job.cancel()
    }

    @Test
    fun `entry maps status OUT_OF_RANGE for critically high value`() = runTest {
        val valueInMmol = 200.0 / GlucoseService.CONVERSION_FACTOR
        whenever(glucoseDao.getEntryById(ENTRY_ID)).thenReturn(flowOf(GlucoseEntry(id = ENTRY_ID, valueInMmol = valueInMmol)))
        whenever(glucoseService.fromMmol(any(), any())).thenReturn(0.0)
        whenever(glucoseService.getGlucoseStatus(eq(valueInMmol))).thenReturn(BloodGlucoseStatus.OUT_OF_RANGE)

        viewModel = GlucoseDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("entryId" to ENTRY_ID)),
            glucoseDao = glucoseDao,
            glucoseService = glucoseService
        )

        val job = collectEntry()
        advanceUntilIdle()

        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, viewModel.entry.value?.status)

        job.cancel()
    }
}

