package com.assessment.minilogbook.domain.model

/**
 * Represents the measurement unit for blood glucose values.
 *
 * - [MMOL_L] — millimoles per litre; used in the UK, Canada, Australia and most of Europe.
 * - [MG_DL]  — milligrams per decilitre; used in the US and several other countries.
 *
 * All values are stored internally in [MMOL_L]. Conversion to [MG_DL] happens only at the UI layer.
 */
enum class GlucoseUnit {
    MMOL_L,
    MG_DL
}

