package com.monta.library.ocpp.v201.blocks.diagnostics

import com.monta.library.ocpp.common.profile.Feature
import com.monta.library.ocpp.common.profile.OcppConfirmation
import com.monta.library.ocpp.common.profile.OcppRequest
import com.monta.library.ocpp.v201.common.CustomData
import com.monta.library.ocpp.v201.common.GenericStatus
import com.monta.library.ocpp.v201.common.StatusInfo

object SetMonitoringLevelFeature : Feature {
    override val name: String = "SetMonitoringLevel"
    override val requestType: Class<out OcppRequest> = SetMonitoringLevelRequest::class.java
    override val confirmationType: Class<out OcppConfirmation> = SetMonitoringLevelResponse::class.java
}

data class SetMonitoringLevelRequest(
    /**
     * The Charging Station SHALL only report events with a severity number lower than or equal to this severity.
     * The severity range is 0-9, with 0 as the highest and 9 as the lowest severity level.
     *
     * The severity levels have the following meaning: +
     *  *0-Danger* +
     * Indicates lives are potentially in danger. Urgent attention is needed and action should be taken immediately. +
     *  *1-Hardware Failure* +
     * Indicates that the Charging Station is unable to continue regular operations due to Hardware issues. Action is required. +
     *  *2-System Failure* +
     * Indicates that the Charging Station is unable to continue regular operations due to software or minor hardware issues. Action is required. +
     *  *3-Critical* +
     * Indicates a critical error. Action is required. +
     *  *4-Error* +
     * Indicates a non-urgent error. Action is required. +
     *  *5-Alert* +
     * Indicates an alert event. Default severity for any type of monitoring event.  +
     *  *6-Warning* +
     * Indicates a warning event. Action may be required. +
     *  *7-Notice* +
     * Indicates an unusual event. No immediate action is required. +
     *  *8-Informational* +
     * Indicates a regular operational event. May be used for reporting, measuring throughput, etc. No action is required. +
     *  *9-Debug* +
     * Indicates information useful to developers for debugging, not useful during operations.
     **/
    val severity: Long,
    val customData: CustomData? = null
) : OcppRequest

data class SetMonitoringLevelResponse(
    val status: GenericStatus,
    val statusInfo: StatusInfo? = null,
    val customData: CustomData? = null
) : OcppConfirmation
