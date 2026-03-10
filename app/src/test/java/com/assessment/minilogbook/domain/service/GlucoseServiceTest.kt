package com.assessment.minilogbook.domain.service

import com.assessment.minilogbook.domain.model.GlucoseUnit
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GlucoseServiceTest {

    private lateinit var converter: GlucoseService
    private val epsilon = 0.0001 // to compare floating point numbers we can use a smaller epsilon value if more precision is needed

    @BeforeEach
    fun setUp() {
        converter = GlucoseService()
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

    @Test
    fun `getGlucoseStatus returns IN_TARGET for values within range`() {
        // Target: 90 - 140 mg/dL. 100 mg/dL is ~5.55 mmol/L
        val valueMmol = 100.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.IN_TARGET, converter.getGlucoseStatus(valueMmol))
    }

    @Test
    fun `getGlucoseStatus returns OK for marginal values`() {
        // Ok: 160 mg/dL is ~8.88 mmol/L
        val valueMmolHigh = 160.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.OK, converter.getGlucoseStatus(valueMmolHigh))

        // Ok: 80 mg/dL is ~4.44 mmol/L
        val valueMmolLow = 80.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.OK, converter.getGlucoseStatus(valueMmolLow))
    }

    @Test
    fun `getGlucoseStatus returns OUT_OF_RANGE for critical values`() {
        // Critical: 200 mg/dL is ~11.1 mmol/L
        val valueMmolHigh = 200.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, converter.getGlucoseStatus(valueMmolHigh))

        // Critical: 60 mg/dL is ~3.33 mmol/L
        val valueMmolLow = 60.0 / GlucoseService.CONVERSION_FACTOR
        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, converter.getGlucoseStatus(valueMmolLow))
    }

    @Test
    fun `getGlucoseStatusByUnit correctly assesses status for both units`() {
        // Test Target range (90-140 mg/dL)
        val targetMgDl = 100.0
        val targetMmol = 100.0 / GlucoseService.CONVERSION_FACTOR
        
        assertEquals(BloodGlucoseStatus.IN_TARGET, converter.getGlucoseStatusByUnit(targetMgDl, GlucoseUnit.MG_DL))
        assertEquals(BloodGlucoseStatus.IN_TARGET, converter.getGlucoseStatusByUnit(targetMmol, GlucoseUnit.MMOL_L))

        // Test OK range (70-90 or 140-180 mg/dL)
        val okMgDl = 160.0
        val okMmol = 160.0 / GlucoseService.CONVERSION_FACTOR

        assertEquals(BloodGlucoseStatus.OK, converter.getGlucoseStatusByUnit(okMgDl, GlucoseUnit.MG_DL))
        assertEquals(BloodGlucoseStatus.OK, converter.getGlucoseStatusByUnit(okMmol, GlucoseUnit.MMOL_L))

        // Test OUT_OF_RANGE range (<70 or >180 mg/dL)
        val outMgDl = 200.0
        val outMmol = 200.0 / GlucoseService.CONVERSION_FACTOR

        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, converter.getGlucoseStatusByUnit(outMgDl, GlucoseUnit.MG_DL))
        assertEquals(BloodGlucoseStatus.OUT_OF_RANGE, converter.getGlucoseStatusByUnit(outMmol, GlucoseUnit.MMOL_L))
    }
}
