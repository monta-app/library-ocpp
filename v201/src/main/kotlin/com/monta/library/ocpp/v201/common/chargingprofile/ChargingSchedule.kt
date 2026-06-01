package com.monta.library.ocpp.v201.common.chargingprofile

import com.monta.library.ocpp.common.chargingprofile.ChargingRateUnit
import com.monta.library.ocpp.common.chargingprofile.CommonChargingProfile
import com.monta.library.ocpp.common.serialization.OneDecimalFloorDeserializer
import com.monta.library.ocpp.common.serialization.OneDecimalFloorSerializer
import com.monta.library.ocpp.common.toZonedDateTime
import com.monta.library.ocpp.v201.common.CustomData
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.annotation.JsonSerialize
import java.time.ZonedDateTime

data class ChargingSchedule(
    /** Identifies the ChargingSchedule. */
    val id: Int,
    /** Charging_ Schedule. Start_ Schedule. Date_ Time
     urn:x-oca:ocpp:uid:1:569237
     Starting point of an absolute schedule. If absent the schedule will be relative to start of charging. */
    val startSchedule: ZonedDateTime? = null,
    /** Charging_ Schedule. Duration. Elapsed_ Time
     urn:x-oca:ocpp:uid:1:569236
     Duration of the charging schedule in seconds. If the duration is left empty, the last period will continue indefinitely or until end of the transaction if chargingProfilePurpose = TxProfile. */
    val duration: Int? = null,
    val chargingRateUnit: ChargingRateUnit,
    val chargingSchedulePeriod: List<ChargingSchedulePeriod>,
    /** Charging_ Schedule. Min_ Charging_ Rate. Numeric
     urn:x-oca:ocpp:uid:1:569239
     Minimum charging rate supported by the EV. The unit of measure is defined by the chargingRateUnit. This parameter is intended to be used by a local smart charging algorithm to optimize the power allocation for in the case a charging process is inefficient at lower charging rates. Accepts at most one digit fraction (e.g. 8.1) */
    @param:JsonSerialize(using = OneDecimalFloorSerializer::class)
    @param:JsonDeserialize(using = OneDecimalFloorDeserializer::class)
    val minChargingRate: Double? = null,
    val salesTariff: SalesTariff? = null,
    val customData: CustomData? = null
) {
    companion object {
        fun fromCommon(
            commonChargingSchedule: CommonChargingProfile.Schedule
        ): ChargingSchedule {
            return ChargingSchedule(
                id = commonChargingSchedule.id,
                startSchedule = commonChargingSchedule.startSchedule.toZonedDateTime(),
                duration = commonChargingSchedule.duration,
                chargingRateUnit = commonChargingSchedule.chargingRateUnit,
                chargingSchedulePeriod = commonChargingSchedule.chargingSchedulePeriod.map {
                    ChargingSchedulePeriod.fromCommon(it)
                },
                minChargingRate = commonChargingSchedule.minChargingRate,
                salesTariff = null,
                customData = null
            )
        }
    }

    init {
        require(chargingSchedulePeriod.size in 1..1024) {
            "chargingSchedulePeriod length not in range 1..1024 - ${chargingSchedulePeriod.size}"
        }
    }
}
