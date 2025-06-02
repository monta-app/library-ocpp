package com.monta.library.ocpp.v16.smartcharge

import com.monta.library.ocpp.common.chargingprofile.ChargingRateUnit
import com.monta.library.ocpp.common.profile.Feature
import com.monta.library.ocpp.common.profile.OcppConfirmation
import com.monta.library.ocpp.common.profile.OcppRequest
import java.time.ZonedDateTime

object GetCompositeScheduleFeature : Feature {
    override val name: String = "GetCompositeSchedule"
    override val requestType: Class<out OcppRequest> = GetCompositeScheduleRequest::class.java
    override val confirmationType: Class<out OcppConfirmation> = GetCompositeScheduleConfirmation::class.java
}

enum class GetCompositeScheduleStatus {
    Accepted,
    Rejected
}

data class GetCompositeScheduleRequest(
    val connectorId: Int,
    val duration: Int,
    val chargingRateUnit: ChargingRateUnit? = null
) : OcppRequest

data class GetCompositeScheduleConfirmation(
    val status: GetCompositeScheduleStatus,
    val connectorId: Int? = null,
    val scheduleStart: ZonedDateTime? = null,
    val chargingSchedule: ChargingSchedule? = null
) : OcppConfirmation
