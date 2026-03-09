package com.assessment.minilogbook.domain.service

import com.assessment.minilogbook.data.GlucoseUnit
import com.assessment.minilogbook.domain.model.BloodGlucoseStatus

/**
 * Contract for glucose value conversion, validation and status evaluation.
 * if the project grows in complexity, we can consider splitting this into separate interfaces (e.g., IGlucoseValidator and IGlucoseConverter),
 * or even create use cases
 */
interface IGlucoseService {
    fun validateValue(value: Double?): Boolean
    fun convertValue(value: Double, fromUnit: GlucoseUnit, toUnit: GlucoseUnit): Double
    fun toMmol(value: Double, currentUnit: GlucoseUnit): Double
    fun fromMmol(valueInMmol: Double, targetUnit: GlucoseUnit): Double
    fun toMmolIfValid(value: Double?, currentUnit: GlucoseUnit): Double?
    fun getGlucoseStatus(valueInMmol: Double): BloodGlucoseStatus
    fun getGlucoseStatusByUnit(value: Double, unit: GlucoseUnit): BloodGlucoseStatus
}

