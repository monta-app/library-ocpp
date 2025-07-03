package com.monta.library.ocpp.v201.blocks

import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v201.blocks.availability.HeartbeatRequest
import com.monta.library.ocpp.v201.common.CustomData
import com.monta.library.ocpp.v201.error.OcppErrorResponderV201
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CustomDataSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_2, OcppErrorResponderV201)

    "custom data (de)serialization should work" {
        val x = CustomData()
        x["vendorId"] = "testVendor"
        x["testKey"] = "testValue"
        val heartbeatRequest = Message.Request(
            "123",
            "Heartbeat",
            messageSerializer.toPayload(
                HeartbeatRequest(customData = x)
            )
        )
        val serialized = heartbeatRequest.toJsonString(messageSerializer)
        serialized shouldBe "[2,\"123\",\"Heartbeat\",{\"customData\":{\"vendorId\":\"testVendor\",\"testKey\":\"testValue\"}}]"
    }

    "deserialization" {
        val serialized = "[2,\"123\",\"Heartbeat\",{\"customData\":{\"vendorId\":\"testVendor\",\"testKey\":\"testValue\"}}]"
        val parsed = messageSerializer.parse(serialized)
        (parsed is ParsingResult.Success<Message>) shouldBe true
        val message = (parsed as ParsingResult.Success<Message>).value
        (message is Message.Request) shouldBe true
        val request = message as Message.Request
        request.uniqueId shouldBe "123"
        request.action shouldBe "Heartbeat"
        val parsedPayload = messageSerializer.deserializePayload(request, HeartbeatRequest::class.java)
        (parsedPayload is ParsingResult.Success<HeartbeatRequest>) shouldBe true
        (parsedPayload as ParsingResult.Success<HeartbeatRequest>).value.customData?.vendorId shouldBe "testVendor"
        parsedPayload.value.customData?.get("testKey") shouldBe "testValue"
    }
})
