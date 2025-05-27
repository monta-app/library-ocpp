package com.monta.library.ocpp.v201.blocks.firmwaremanagement

import com.monta.library.ocpp.common.profile.Feature
import com.monta.library.ocpp.common.profile.OcppConfirmation
import com.monta.library.ocpp.common.profile.OcppRequest
import com.monta.library.ocpp.v201.common.CustomData
import com.monta.library.ocpp.v201.common.StatusInfo
import java.time.ZonedDateTime

object UpdateFirmwareFeature : Feature {
    override val name: String = "UpdateFirmware"
    override val requestType: Class<out OcppRequest> = UpdateFirmwareRequest::class.java
    override val confirmationType: Class<out OcppConfirmation> = UpdateFirmwareResponse::class.java
}

data class UpdateFirmwareRequest(
    /** This specifies how many times Charging Station must try to download the firmware before giving up. If this field is not present, it is left to Charging Station to decide how many times it wants to retry. */
    val retries: Long? = null,
    /** The interval in seconds after which a retry may be attempted. If this field is not present, it is left to Charging Station to decide how long to wait between attempts. */
    val retryInterval: Long? = null,
    /** The Id of this request */
    val requestId: Long,
    val firmware: Firmware,
    val customData: CustomData? = null
) : OcppRequest {

    data class Firmware(
        val customData: CustomData? = null,
        /**
         * URI defining the origin of the firmware.
         **/
        val location: String,
        /**
         * Date and time at which the firmware shall be retrieved.
         **/
        val retrieveDateTime: ZonedDateTime,
        /**
         * Date and time at which the firmware shall be installed.
         **/
        val installDateTime: ZonedDateTime? = null,
        /**
         * PEM encoded X.509 certificate.
         **/
        val signingCertificate: String? = null,
        /**
         * Base64 encoded firmware signature.
         **/
        val signature: String? = null
    ) {

        init {
            require(location.length <= 512) {
                "location length > maximum 512 - ${location.length}"
            }
            if (signingCertificate != null) {
                require(signingCertificate.length <= 5500) {
                    "signingCertificate length > maximum 5500 - ${signingCertificate.length}"
                }
            }
            if (signature != null) {
                require(signature.length <= 800) {
                    "signature length > maximum 800 - ${signature.length}"
                }
            }
        }
    }
}

data class UpdateFirmwareResponse(
    val status: Status,
    val statusInfo: StatusInfo? = null,
    val customData: CustomData? = null
) : OcppConfirmation {

    enum class Status {
        Accepted,
        Rejected,
        AcceptedCanceled,
        InvalidCertificate,
        RevokedCertificate
    }
}
