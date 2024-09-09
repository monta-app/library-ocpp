package com.monta.library.ocpp.v16.extension.plugandcharge

import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v16.core.DataTransferConfirmation
import com.monta.library.ocpp.v16.core.DataTransferRequest
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import com.monta.library.ocpp.v16.extension.plugandcharge.features.Get15118EVCertificateRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.GetInstalledCertificateIdsConfirmation
import com.monta.library.ocpp.v16.extension.plugandcharge.features.GetInstalledCertificateIdsRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.PncAuthorizeRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beInstanceOf

class PlugAndChargeSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    fun <T> dtReq(fileName: String, dtDataType: Class<T>) {
        val json = this.javaClass.getResourceAsStream(fileName)?.bufferedReader().use { it?.readText() }!!
        val result = messageSerializer.parse(json)
        result should beInstanceOf<ParsingResult.Success<DataTransferRequest>>()
        val resp = (result as ParsingResult.Success<Message.Request>).value
        val parseResult = messageSerializer.deserializePayload(resp, DataTransferRequest::class.java)
        when (parseResult) {
            is ParsingResult.Success<DataTransferRequest> -> {
                messageSerializer.parseDataTransferExtension(parseResult.value.data, dtDataType)
            }

            else -> {
                throw AssertionError("could not parse:\n $json\n\n, $parseResult")
            }
        }
    }

    fun <T> dtConf(fileName: String, dtDataType: Class<T>) {
        val json = this.javaClass.getResourceAsStream(fileName)?.bufferedReader().use { it?.readText() }!!
        val result = messageSerializer.parse(json)
        result should beInstanceOf<ParsingResult.Success<DataTransferConfirmation>>()
        val resp = (result as ParsingResult.Success<Message.Response>).value
        val parseResult = messageSerializer.deserializePayload(resp, DataTransferConfirmation::class.java)
        when (parseResult) {
            is ParsingResult.Success<DataTransferConfirmation> -> {
                messageSerializer.parseDataTransferExtension(parseResult.value.data, dtDataType)
            }

            else -> {
                throw AssertionError("could not parse:\n $json\n\n, $parseResult")
            }
        }
    }

    "GetInstalledCertificate request should parse" {
        val conf =
            dtReq("tally-key-bender-GetInstalledCertificateIds.req.json", GetInstalledCertificateIdsRequest::class.java)
        conf shouldNotBe null
    }

    "GetInstalledCertificate confirmation should parse without child certificates" {
        val conf = dtConf(
            "tally-key-bender-GetInstalledCertificateIds.conf.json",
            GetInstalledCertificateIdsConfirmation::class.java
        )
        conf shouldNotBe null
    }

    "Authorize request should parse with extra type tag" {
        val req = dtReq(
            "kempower-Authorize.req.json",
            PncAuthorizeRequest::class.java
        )
        req shouldNotBe null
    }

    "Kempower Get15118EVCertificate request should be deserialized" {
        val req = dtReq(
            "kempower-Get15118EVCertificate.req.json",
            Get15118EVCertificateRequest::class.java
        )
        req shouldNotBe null
    }

    "Kempower GetInstalledCertificateIds response should be deserialized" {
        val conf = dtConf(
            "kempower-GetInstalledCertificateIds.conf.json",
            GetInstalledCertificateIdsConfirmation::class.java
        )
        conf shouldNotBe null
    }

    "Alpitronic Authorize request should be deserialized" {
        val req = dtReq(
            "alpitronic-Authorize.req.json",
            PncAuthorizeRequest::class.java
        )
        req shouldNotBe null
    }
})
