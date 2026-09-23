package com.monta.library.ocpp.v16.core

import com.monta.library.ocpp.common.profile.Feature
import com.monta.library.ocpp.common.profile.OcppConfirmation
import com.monta.library.ocpp.common.profile.OcppRequest

/**
 * To retrieve the value of configuration settings, the Central System SHALL send a [GetConfigurationRequest] PDU to the Charge Point.
 *
 * If the list of keys in the request PDU is empty or missing (it is optional),
 * the Charge Point SHALL return a list of all configuration settings in GetConfiguration.conf.
 * Otherwise, Charge Point SHALL return a list of recognized keys and their corresponding values and read-only state.
 * Unrecognized keys SHALL be placed in the response PDU as part of the optional unknown key list element of [GetConfigurationConfirmation].
 *
 * The number of configuration keys requested in a single PDU MAY be limited by the Charge Point.
 * This maximum can be retrieved by reading the configuration key GetConfigurationMaxKeys.
 */
object GetConfigurationFeature : Feature {
    override val name: String = "GetConfiguration"
    override val requestType: Class<out OcppRequest> = GetConfigurationRequest::class.java
    override val confirmationType: Class<out OcppConfirmation> = GetConfigurationConfirmation::class.java
}

data class GetConfigurationRequest(
    /**
     * Optional
     *
     * List of keys for which the configuration value is requested.
     */
    val key: List<String>? = null
) : OcppRequest

/**
 * Contains information about a specific configuration key.
 * It is returned in [GetConfigurationConfirmation]
 */
data class KeyValueType(
    /**
     * TODO String[50]
     *
     * Required
     */
    val key: String,
    /**
     * Required
     *
     * False if the value can be set with the ChangeConfiguration message.
     * Defaults to false because some charge points (e.g. Rolec) omit it for keys they don't support.
     */
    val readonly: Boolean = false,
    /**
     * Optional
     *
     * If key is known but not set, this field may be absent
     */
    val value: String? = null
)
/*
*
 * Response to a [GetConfigurationRequest]
 */

data class GetConfigurationConfirmation(
    /**
     * Optional
     *
     * List of requested or known keys
     */
    val configurationKey: List<KeyValueType>?,
    /**
     * String[50]
     *
     * Optional
     *
     * Requested keys that are unknown
     */
    val unknownKey: List<String>?
) : OcppConfirmation
