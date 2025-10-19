package com.monta.library.ocpp.v16.smartcharge

import com.monta.library.ocpp.common.profile.Feature
import com.monta.library.ocpp.common.profile.OcppConfirmation
import com.monta.library.ocpp.common.profile.OcppRequest

object ClearChargingProfileFeature : Feature {
    override val name: String = "ClearChargingProfile"
    override val requestType: Class<out OcppRequest> = ClearChargingProfileRequest::class.java
    override val confirmationType: Class<out OcppConfirmation> = ClearChargingProfileConfirmation::class.java
}

enum class ClearChargingProfileStatus {
    Accepted,
    Unknown
}

data class ClearChargingProfileRequest(
    val id: Int? = null,
    val connectorId: Int? = null,
    val chargingProfilePurpose: ChargingProfilePurposeType? = null,
    val stackLevel: Int? = null
) : OcppRequest

class ClearChargingProfileConfirmation(
    val status: ClearChargingProfileStatus
) : OcppConfirmation
