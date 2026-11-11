package com.monta.library.ocpp.common.serialization

import com.monta.library.ocpp.common.error.OcppErrorCode
import com.monta.library.ocpp.common.error.OcppErrorResponder
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class Ocpp21FramesTest : StringSpec({

    val errorResponder = object : OcppErrorResponder {
        private fun code(name: String) = object : OcppErrorCode {
            override fun getName() = name
            override fun getMessage() = name
        }

        override fun getInternalError() = code("InternalError")
        override fun getJsonFormatError() = code("FormatViolation")
        override fun getPropertyConstraintViolationError() = code("PropertyConstraintViolation")
        override fun getNotImplementedError() = code("NotImplemented")
    }

    val serializer21 = MessageSerializer(SerializationMode.OCPP_2_1, errorResponder)
    val serializer2 = MessageSerializer(SerializationMode.OCPP_2, errorResponder)
    val serializer16 = MessageSerializer(SerializationMode.OCPP_1_6, errorResponder)

    "CALLRESULTERROR is parsed in OCPP 2.1 mode" {
        val result = serializer21.parse("""[5,"abc","FormatViolation","bad response",{"field":"status"}]""")
        result.shouldBeInstanceOf<ParsingResult.Success<Message>>()
        val message = result.value
        message.shouldBeInstanceOf<Message.CallResultError>()
        message.uniqueId shouldBe "abc"
        message.errorCode shouldBe "FormatViolation"
        message.errorDescription shouldBe "bad response"
        message.errorDetails shouldBe """{"field":"status"}"""
    }

    "SEND is parsed in OCPP 2.1 mode" {
        val result = serializer21.parse(
            """[6,"19223201","NotifyPeriodicEventStream",{"id":123,"pending":0,"basetime":"2024-08-27T12:30:40.000Z","data":[]}]"""
        )
        result.shouldBeInstanceOf<ParsingResult.Success<Message>>()
        val message = result.value
        message.shouldBeInstanceOf<Message.Send>()
        message.action shouldBe "NotifyPeriodicEventStream"
        message.payload["id"].asInt() shouldBe 123
    }

    "unknown message types are ignored in OCPP 2.1 mode" {
        serializer21.parse("""[7,"id-7","Future",{}]""").shouldBeInstanceOf<ParsingResult.Ignored<Message>>()
        val ignored = serializer21.parse("""[1,"id-1"]""")
        ignored.shouldBeInstanceOf<ParsingResult.Ignored<Message>>()
        ignored.uniqueId shouldBe "id-1"
        ignored.messageTypeId shouldBe 1
    }

    "message types 5, 6 and 7 are rejected in OCPP 2.0.1 and 1.6 mode" {
        listOf(serializer2, serializer16).forEach { serializer ->
            serializer.parse("""[5,"abc","FormatViolation","",{}]""").shouldBeInstanceOf<ParsingResult.Failure<Message>>()
            serializer.parse("""[6,"abc","NotifyPeriodicEventStream",{}]""").shouldBeInstanceOf<ParsingResult.Failure<Message>>()
            serializer.parse("""[7,"abc","Future",{}]""").shouldBeInstanceOf<ParsingResult.Failure<Message>>()
        }
    }

    "CALLRESULTERROR is written with proper escaping" {
        val message = Message.CallResultError(
            uniqueId = "id\"1",
            errorCode = "FormatViolation",
            errorDescription = "field \"status\" missing",
            errorDetails = """{"a":1}"""
        )
        message.toJsonString(serializer21) shouldBe """[5,"id\"1","FormatViolation","field \"status\" missing",{"a":1}]"""
    }

    "CALLRESULTERROR with invalid details falls back to an empty object" {
        Message.CallResultError("x", "GenericError", "", "not json").toJsonString(serializer21) shouldBe """[5,"x","GenericError","",{}]"""
    }

    "SEND round trips" {
        val payload = (serializer21.parse("""[2,"x","A",{"id":1,"data":[{"t":1.5,"v":"230.4"}]}]""") as ParsingResult.Success<Message>)
            .value
            .let { it as Message.Request }
            .payload
        val json = Message.Send("19223201", "NotifyPeriodicEventStream", payload).toJsonString(serializer21)
        json shouldBe """[6,"19223201","NotifyPeriodicEventStream",{"id":1,"data":[{"t":1.5,"v":"230.4"}]}]"""
        val reparsed = serializer21.parse(json)
        reparsed.shouldBeInstanceOf<ParsingResult.Success<Message>>()
        reparsed.value shouldBe Message.Send("19223201", "NotifyPeriodicEventStream", payload)
    }
    "a SEND or CALLRESULTERROR that cannot be read is ignored, never answered (2.1 mode)" {
        listOf(
            """[6,"s-1"]""",
            """[6,"s-1","NotifyPeriodicEventStream"]""",
            """[6,"s-1","NotifyPeriodicEventStream","not an object"]""",
            """[6,42,"NotifyPeriodicEventStream",{}]""",
            """[5,"r-1"]""",
            """[5,"r-1","FormatViolation"]"""
        ).forEach { frame ->
            val result = serializer21.parse(frame)
            result.shouldBeInstanceOf<ParsingResult.Ignored<Message>>()
            result.messageTypeId shouldBe frame[1].digitToInt()
        }
    }

    "an unreadable message id is answered with -1 in 2.1 mode only" {
        val invalidJson = serializer21.parse("""[2,"abc""")
        invalidJson.shouldBeInstanceOf<ParsingResult.Failure<Message>>()
        invalidJson.uniqueId shouldBe "-1"
        // the id is readable: keep it
        val incompleteCall = serializer21.parse("""[2,"call-1"]""")
        incompleteCall.shouldBeInstanceOf<ParsingResult.Failure<Message>>()
        incompleteCall.uniqueId shouldBe "call-1"
        // 1.6 and 2.0.1 unchanged
        listOf(serializer2, serializer16).forEach { serializer ->
            val result = serializer.parse("""[2,"abc""")
            result.shouldBeInstanceOf<ParsingResult.Failure<Message>>()
            result.uniqueId shouldBe ""
        }
    }
})
