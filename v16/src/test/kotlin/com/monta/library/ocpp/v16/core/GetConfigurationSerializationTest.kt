package com.monta.library.ocpp.v16.core

import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class GetConfigurationSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    fun parseConfirmation(json: String): GetConfigurationConfirmation {
        val parsingResult = messageSerializer.parse(json)
        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Response>>()
        val message = parsingResult.value
        message.shouldBeInstanceOf<Message.Response>()
        val payloadResult = messageSerializer.deserializePayload(message, GetConfigurationConfirmation::class.java)
        payloadResult.shouldBeInstanceOf<ParsingResult.Success<GetConfigurationConfirmation>>()
        return payloadResult.value
    }

    "parse spec compliant GetConfiguration response" {
        val confirmation = parseConfirmation(
            """[3,"res-1",{"configurationKey":[{"key":"HeartbeatInterval","readonly":false,"value":"300"},{"key":"NumberOfConnectors","readonly":true,"value":"2"}],"unknownKey":["Foo"]}]"""
        )

        confirmation.configurationKey shouldBe listOf(
            KeyValueType(key = "HeartbeatInterval", readonly = false, value = "300"),
            KeyValueType(key = "NumberOfConnectors", readonly = true, value = "2")
        )
        confirmation.unknownKey shouldBe listOf("Foo")
    }

    "parse Rolec GetConfiguration response with keys missing readonly" {
        val confirmation = parseConfirmation(
            """[3,"res-2",{"configurationKey":[{"key":"AuthorizeRemoteTxRequests","readonly":false,"value":"true"},{"key":"ISO15118PnCEnabled"},{"key":"MinimumStatusDuration"},{"key":"NumberOfConnectors","readonly":true,"value":"2"}]}]"""
        )

        confirmation.configurationKey shouldBe listOf(
            KeyValueType(key = "AuthorizeRemoteTxRequests", readonly = false, value = "true"),
            KeyValueType(key = "ISO15118PnCEnabled", readonly = false, value = null),
            KeyValueType(key = "MinimumStatusDuration", readonly = false, value = null),
            KeyValueType(key = "NumberOfConnectors", readonly = true, value = "2")
        )
    }

    "parse GetConfiguration response with value but missing readonly" {
        val confirmation = parseConfirmation(
            """[3,"res-3",{"configurationKey":[{"key":"MeterValueSampleInterval","value":"60"}]}]"""
        )

        confirmation.configurationKey shouldBe listOf(
            KeyValueType(key = "MeterValueSampleInterval", readonly = false, value = "60")
        )
    }
})
