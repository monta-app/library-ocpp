package com.monta.library.ocpp.v201.common.chargingprofile

import com.monta.library.ocpp.common.chargingprofile.CommonChargingProfile
import com.monta.library.ocpp.common.serialization.OneDecimalFloorDeserializer
import com.monta.library.ocpp.common.serialization.OneDecimalFloorSerializer
import com.monta.library.ocpp.v201.common.CustomData
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.annotation.JsonSerialize

data class ChargingSchedulePeriod(
    /**
     * Start of the period, in seconds from the start of schedule.
     * The value of StartPeriod also defines the stop time of the previous period.
     **/
    val startPeriod: Int,
    /**
     * Charging rate limit during the schedule period, in the applicable chargingRateUnit,
     * for example in Amperes (A) or Watts (W).
     * Accepts at most one digit fraction (e.g. 8.1).
     **/
    @param:JsonSerialize(using = OneDecimalFloorSerializer::class)
    @param:JsonDeserialize(using = OneDecimalFloorDeserializer::class)
    val limit: Double,
    /**
     * The number of phases that can be used for charging.
     * If a number of phases is needed, numberPhases=3 will be assumed unless another number is given.
     **/
    val numberPhases: Int? = null,
    /**
     * Values: 1..3, Used if numberPhases=1 and if the EVSE is capable of switching the phase connected to the EV, i.e. ACPhaseSwitchingSupported is defined and true.
     * It’s not allowed unless both conditions above are true. If both conditions are true, and phaseToUse is omitted, the Charging Station / EVSE will make the selection on its own.
     **/
    val phaseToUse: Int? = null,
    val customData: CustomData? = null
) {
    companion object {
        fun fromCommon(
            commonChargingSchedulePeriod: CommonChargingProfile.Period
        ): ChargingSchedulePeriod {
            return ChargingSchedulePeriod(
                startPeriod = commonChargingSchedulePeriod.startPeriod,
                limit = commonChargingSchedulePeriod.limit,
                numberPhases = commonChargingSchedulePeriod.numberPhases,
                phaseToUse = null,
                customData = null
            )
        }
    }
}
