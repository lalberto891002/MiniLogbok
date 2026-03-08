package com.assessment.minilogbook.ui.viewmodel

import com.assessment.minilogbook.data.GlucoseDao
import com.assessment.minilogbook.data.GlucoseUnit
import com.assessment.minilogbook.domain.usecase.GlucoseConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
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
    private val converter = GlucoseConverter()
    private lateinit var viewModel: GlucoseViewModel

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
        whenever(glucoseDao.getAllEntries()).thenReturn(MutableStateFlow(emptyList()))
        viewModel = GlucoseViewModel(glucoseDao, converter)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onInputValueChanged updates state and clears error`() = runTest {
        viewModel.onInputValueChanged("5.5")
        val state = viewModel.state.first()
        assertEquals("5.5", state.inputValue)
        assertNull(state.errorMessage)
    }

    @Test
    fun `saveEntry with empty input sets error message`() = runTest {
        viewModel.onInputValueChanged("")
        viewModel.saveEntry()
        val state = viewModel.state.first()
        assertEquals("Please enter a valid value >= 0", state.errorMessage)
        verify(glucoseDao, never()).insert(any())
    }

    @Test
    fun `saveEntry with invalid number input sets error message`() = runTest {
        viewModel.onInputValueChanged("abc")
        viewModel.saveEntry()
        val state = viewModel.state.first()
        assertEquals("Please enter a valid value >= 0", state.errorMessage)
        verify(glucoseDao, never()).insert(any())
    }

    @Test
    fun `saveEntry with negative input sets error message`() = runTest {
        viewModel.onInputValueChanged("-1.0")
        viewModel.saveEntry()
        val state = viewModel.state.first()
        assertEquals("Please enter a valid value >= 0", state.errorMessage)
        verify(glucoseDao, never()).insert(any())
    }

    @Test
    fun `saveEntry with valid input inserts into dao and clears input`() = runTest {
        viewModel.onInputValueChanged("5.0")
        viewModel.saveEntry()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(glucoseDao).insert(any())
        val state = viewModel.state.first()
        assertEquals("", state.inputValue)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onUnitChanged converts current input value parameterized`() = runTest {
        // Start with mmol/L (default set in GlucoseState)
        viewModel.onInputValueChanged(inputMmol)

        // Change to mg/dL
        viewModel.onUnitChanged(GlucoseUnit.MG_DL)

        val state = viewModel.state.first()
        assertEquals(GlucoseUnit.MG_DL, state.unit)

        // Use a small helper to normalize strings if necessary,
        // but GlucoseConverter now returns exactly 4 decimal places via roundToFourDecimals
        assertEquals(expectedMgDl.toDouble(), state.inputValue.toDouble(), 0.0001)
    }

    @Test
    fun `getAverage returns 0 when no entries`() {
        val average = viewModel.getAverage(GlucoseUnit.MMOL_L)
        assertEquals(0.0, average, 0.001)
    }
}
