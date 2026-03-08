package com.assessment.minilogbook.domain.usecase

import com.assessment.minilogbook.data.GlucoseUnit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GlucoseConverterTest {

    private lateinit var converter: GlucoseConverter
    private val epsilon = 0.0001 // to compare floating point numbers we can use a smaller epsilon value if more precision is needed

    @Before
    fun setUp() {
        converter = GlucoseConverter()
    }

    @Test
    fun `validateValue returns true for positive numbers`() {
        assertTrue(converter.validateValue(5.5))
        assertTrue(converter.validateValue(0.0))
    }

    @Test
    fun `validateValue returns false for negative numbers`() {
        assertFalse(converter.validateValue(-1.0))
        assertFalse(converter.validateValue(-0.1))
    }

    @Test
    fun `validateValue returns false for null`() {
        assertFalse(converter.validateValue(null))
    }

    @Test
    fun `toMmolIfValid returns converted value for positive numbers`() {
        val result = converter.toMmolIfValid(18.0182, GlucoseUnit.MG_DL)
        assertNotNull(result)
        assertEquals(1.0, result!!, epsilon)
    }

    @Test
    fun `toMmolIfValid returns null for negative numbers`() {
        assertNull(converter.toMmolIfValid(-1.0, GlucoseUnit.MMOL_L))
    }

    @Test
    fun `toMmolIfValid returns null for null input`() {
        assertNull(converter.toMmolIfValid(null, GlucoseUnit.MMOL_L))
    }

    @Test
    fun `toMmol converts correctly from mg dl`() {
        val mgDlValue = 18.0182
        val expectedMmol = 1.0
        val result = converter.toMmol(mgDlValue, GlucoseUnit.MG_DL)
        assertEquals(expectedMmol, result, epsilon)
    }

    @Test
    fun `toMmol returns same value if already mmol l`() {
        val mmolValue = 5.0
        val result = converter.toMmol(mmolValue, GlucoseUnit.MMOL_L)
        assertEquals(mmolValue, result, epsilon)
    }

    @Test
    fun `convertValue from mmol to mg dl is correct`() {
        val mmolValue = 1.0
        val expectedMgDl = 18.0182
        val result = converter.convertValue(mmolValue, GlucoseUnit.MMOL_L, GlucoseUnit.MG_DL)
        assertEquals(expectedMgDl, result, epsilon)
    }

    @Test
    fun `convertValue from mg dl to mmol is correct`() {
        val mgDlValue = 18.0182
        val expectedMmol = 1.0
        val result = converter.convertValue(mgDlValue, GlucoseUnit.MG_DL, GlucoseUnit.MMOL_L)
        assertEquals(expectedMmol, result, epsilon)
    }

    @Test
    fun `convertValue returns same value if units are equal`() {
        val value = 100.0
        assertEquals(value, converter.convertValue(value, GlucoseUnit.MG_DL, GlucoseUnit.MG_DL), epsilon)
        assertEquals(value, converter.convertValue(value, GlucoseUnit.MMOL_L, GlucoseUnit.MMOL_L), epsilon)
    }
}
