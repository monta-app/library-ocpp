package com.monta.library.ocpp.v201.blocks.firmwaremanagement

import com.monta.library.ocpp.common.profile.Feature
import com.monta.library.ocpp.common.profile.OcppConfirmation
import com.monta.library.ocpp.common.profile.OcppRequest
import com.monta.library.ocpp.v201.common.CustomData

object PublishFirmwareStatusNotificationFeature : Feature {
    override val name: String = "PublishFirmwareStatusNotification"
    override val requestType: Class<out OcppRequest> = PublishFirmwareStatusNotificationRequest::class.java
    override val confirmationType: Class<out OcppConfirmation> = PublishFirmwareStatusNotificationResponse::class.java
}

data class PublishFirmwareStatusNotificationRequest(
    val status: Status,
    /** Required if status is Published. Can be multiple URI’s, if the Local Controller supports e.g. HTTP, HTTPS, and FTP. */
    val location: List<String>? = null,
    /** The request id that was
     provided in the
     PublishFirmwareRequest which
     triggered this action. */
    val requestId: Long? = null,
    val customData: CustomData? = null
) : OcppRequest {

    init {
        if (location != null) {
            for (cg_0 in location) {
                require(cg_0.length <= 512) {
                    "location item length > maximum 512 - ${cg_0.length}"
                }
            }
            require(location.isNotEmpty()) {
                "location length < minimum 1 - ${location.size}"
            }
        }
    }

    enum class Status {
        Idle,
        DownloadScheduled,
        Downloading,
        Downloaded,
        Published,
        DownloadFailed,
        DownloadPaused,
        InvalidChecksum,
        ChecksumVerified,
        PublishFailed
    }
}

data class PublishFirmwareStatusNotificationResponse(
    val customData: CustomData? = null
) : OcppConfirmation
