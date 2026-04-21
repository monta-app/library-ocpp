package com.monta.library.ocpp.v16.smartcharge

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.monta.library.ocpp.common.chargingprofile.ChargingProfileKind
import com.monta.library.ocpp.common.chargingprofile.ChargingRateUnit
import com.monta.library.ocpp.common.chargingprofile.CommonChargingProfile
import com.monta.library.ocpp.common.chargingprofile.CommonChargingProfilePurpose
import com.monta.library.ocpp.common.chargingprofile.RecurrencyKind
import com.monta.library.ocpp.common.serialization.OneDecimalFloorDeserializer
import com.monta.library.ocpp.common.serialization.OneDecimalFloorSerializer
import com.monta.library.ocpp.common.toZonedDateTime
import java.time.ZonedDateTime

enum class ChargingProfilePurposeType {
    ChargePointMaxProfile,
    TxDefaultProfile,
    TxProfile;

    companion object {
        fun fromCommon(
            commonChargingProfilePurpose: CommonChargingProfilePurpose
        ): ChargingProfilePurposeType {
            return when (commonChargingProfilePurpose) {
                CommonChargingProfilePurpose.TxDefaultProfile -> TxDefaultProfile
                CommonChargingProfilePurpose.TxProfile -> TxProfile
                CommonChargingProfilePurpose.ChargePointMaxProfile -> ChargePointMaxProfile
            }
        }
    }
}

data class ChargingSchedulePeriod(
    val startPeriod: Int? = null,
    @param:JsonSerialize(using = OneDecimalFloorSerializer::class)
    @param:JsonDeserialize(using = OneDecimalFloorDeserializer::class)
    val limit: Double? = null,
    val numberPhases: Int = 3
) {
    companion object {
        fun fromCommon(
            commonChargingSchedulePeriod: CommonChargingProfile.Period
        ): ChargingSchedulePeriod {
            return ChargingSchedulePeriod(
                startPeriod = commonChargingSchedulePeriod.startPeriod,
                limit = commonChargingSchedulePeriod.limit,
                numberPhases = commonChargingSchedulePeriod.numberPhases
            )
        }
    }
}

data class ChargingSchedule(
    val duration: Int? = null,
    val startSchedule: ZonedDateTime? = null,
    val chargingRateUnit: ChargingRateUnit? = null,
    val chargingSchedulePeriod: List<ChargingSchedulePeriod>,
    @param:JsonSerialize(using = OneDecimalFloorSerializer::class)
    @param:JsonDeserialize(using = OneDecimalFloorDeserializer::class)
    var minChargingRate: Double? = null
) {
    companion object {
        fun fromCommon(
            commonChargingSchedule: CommonChargingProfile.Schedule
        ): ChargingSchedule {
            return ChargingSchedule(
                duration = commonChargingSchedule.duration,
                startSchedule = commonChargingSchedule.startSchedule.toZonedDateTime(),
                chargingRateUnit = commonChargingSchedule.chargingRateUnit,
                chargingSchedulePeriod = commonChargingSchedule.chargingSchedulePeriod.map { commonChargingSchedulePeriod ->
                    ChargingSchedulePeriod.fromCommon(commonChargingSchedulePeriod)
                },
                minChargingRate = commonChargingSchedule.minChargingRate
            )
        }
    }
}

data class ChargingProfile(
    val chargingProfileId: Int? = null,
    val transactionId: Int? = null,
    val stackLevel: Int? = null,
    val chargingProfilePurpose: ChargingProfilePurposeType? = null,
    val chargingProfileKind: ChargingProfileKind? = null,
    val recurrencyKind: RecurrencyKind? = null,
    val validFrom: ZonedDateTime? = null,
    val validTo: ZonedDateTime? = null,
    val chargingSchedule: ChargingSchedule? = null
) {
    companion object {
        fun fromCommon(
            commonChargingProfile: CommonChargingProfile
        ): ChargingProfile {
            commonChargingProfile.chargingSchedule
            return ChargingProfile(
                chargingProfileId = commonChargingProfile.id,
                transactionId = commonChargingProfile.transactionId?.toInt(),
                stackLevel = commonChargingProfile.stackLevel,
                chargingProfilePurpose = ChargingProfilePurposeType.fromCommon(
                    commonChargingProfile.chargingProfilePurpose
                ),
                chargingProfileKind = commonChargingProfile.chargingProfileKind,
                recurrencyKind = commonChargingProfile.recurrencyKind,
                validFrom = commonChargingProfile.validFrom.toZonedDateTime(),
                validTo = commonChargingProfile.validTo.toZonedDateTime(),
                chargingSchedule = ChargingSchedule.fromCommon(
                    commonChargingSchedule = commonChargingProfile.chargingSchedule
                )
            )
        }
    }
}
