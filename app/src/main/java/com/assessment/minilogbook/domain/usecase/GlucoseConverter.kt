package com.assessment.minilogbook.domain.usecase

import com.assessment.minilogbook.data.GlucoseUnit
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus
import java.util.Locale

/**
 * Utility class for converting and validating blood glucose values.
 *
 * This class handles the conversion logic between different glucose units (mmol/L and mg/dL)
 * and provides basic validation rules.
 */
class GlucoseConverter {
    companion object {
        /**
         * The constant factor used for converting between mmol/L and mg/dL.
         * 1 mmol/L = 18.0182 mg/dL.
         */
        const val CONVERSION_FACTOR = 18.0182
    }

    /**
     * Validates if a glucose value is a valid reading.
     *
     * @param value The glucose value to validate.
     * @return `true` if the value is non-null and greater than or equal to 0, `false` otherwise.
     *
     *
     * In the future if needed we can move this logic to a separate validator class, but for now it is simple enough to keep it here.
     */
    fun validateValue(value: Double?): Boolean {
        return value != null && value >= 0
    }

    /**
     * Converts a glucose value from one unit to another.
     *
     * @param value The value to convert.
     * @param fromUnit The current unit of the value.
     * @param toUnit The target unit for conversion.
     * @return The converted glucose value rounded to 4 decimal places.
     */
    fun convertValue(value: Double, fromUnit: GlucoseUnit, toUnit: GlucoseUnit): Double {
        if (fromUnit == toUnit) return roundToFourDecimals(value)

        val converted = if (toUnit == GlucoseUnit.MG_DL) {
            value * CONVERSION_FACTOR
        } else {
            value / CONVERSION_FACTOR
        }
        return roundToFourDecimals(converted)
    }

    /**
     * Converts a glucose value from its current unit to mmol/L.
     *
     * @param value The value to convert.
     * @param currentUnit The unit the value is currently in.
     * @return The value converted to mmol/L rounded to 4 decimal places.
     */
    fun toMmol(value: Double, currentUnit: GlucoseUnit): Double {
        val result = if (currentUnit == GlucoseUnit.MG_DL) {
            value / CONVERSION_FACTOR
        } else {
            value
        }
        return roundToFourDecimals(result)
    }

    /**
     * Converts a glucose value from mmol/L to a target unit.
     *
     * @param valueInMmol The value in mmol/L.
     * @param targetUnit The unit to convert to.
     * @return The value converted to the target unit rounded to 4 decimal places.
     */
    fun fromMmol(valueInMmol: Double, targetUnit: GlucoseUnit): Double {
        val result = if (targetUnit == GlucoseUnit.MG_DL) {
            valueInMmol * CONVERSION_FACTOR
        } else {
            valueInMmol
        }
        return roundToFourDecimals(result)
    }

    /**
     * Attempts to convert a numeric input to its mmol/L equivalent if valid.
     *
     * This method combines validation and conversion. If the value is valid according to [validateValue],
     * it will be converted to mmol/L using [toMmol].
     *
     * @param value The numeric value to process.
     * @param currentUnit The unit the value is currently in.
     * @return The value in mmol/L rounded to 4 decimal places if valid, or `null` if the input is null or invalid.
     */
    fun toMmolIfValid(value: Double?, currentUnit: GlucoseUnit): Double? {
        return value?.takeIf { validateValue(it) }?.let {
            toMmol(it, currentUnit)
        }
    }

    /**
     * Determines the status of a glucose reading based on typical target ranges.
     * - Target (Green): 90 - 140 mg/dL
     * - Ok (Orange): 70-90 or 140-180 mg/dL
     * - Out of range (Red): <70 or >180 mg/dL
     *
     * @param valueInMmol The glucose value in mmol/L.
     * @return The corresponding [BloodGlucoseStatus].
     */
    fun getGlucoseStatus(valueInMmol: Double): BloodGlucoseStatus {
        val valueInMgDl = valueInMmol * CONVERSION_FACTOR
        return when {
            valueInMgDl < 70.0 || valueInMgDl > 180.0 -> BloodGlucoseStatus.OUT_OF_RANGE
            valueInMgDl < 90.0 || valueInMgDl > 140.0 -> BloodGlucoseStatus.OK
            else -> BloodGlucoseStatus.IN_TARGET
        }
    }

    /**
     * Determines the status of a glucose reading based on typical target ranges,
     * considering any unit provided.
     *
     * @param value The glucose value.
     * @param unit The unit of the value (mmol/L or mg/dL).
     * @return The corresponding [BloodGlucoseStatus].
     */
    fun getGlucoseStatusByUnit(value: Double, unit: GlucoseUnit): BloodGlucoseStatus {
        val valueInMmol = toMmol(value, unit)
        return getGlucoseStatus(valueInMmol)
    }

    private fun roundToFourDecimals(value: Double): Double {
        return String.format(Locale.US, "%.4f", value).toDouble()
    }
}
