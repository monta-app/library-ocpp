package com.monta.library.ocpp.v201.blocks.smartcharging

import com.monta.library.ocpp.TestUtils
import com.monta.library.ocpp.common.chargingprofile.ChargingProfileKind
import com.monta.library.ocpp.common.chargingprofile.ChargingRateUnit
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v201.common.chargingprofile.ChargingProfile
import com.monta.library.ocpp.v201.common.chargingprofile.ChargingProfilePurpose
import com.monta.library.ocpp.v201.common.chargingprofile.ChargingSchedule
import com.monta.library.ocpp.v201.common.chargingprofile.ChargingSchedulePeriod
import com.monta.library.ocpp.v201.error.OcppErrorResponderV201
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SetChargingProfileSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_2, OcppErrorResponderV201)

    "parse a valid set charging profile request and floor limit and minChargingRate to 1 decimal" {
        val jsonString = TestUtils.getFileAsString("smartcharging/set-charging-profile-req.json")
        val parsingResult = messageSerializer.parse(jsonString)

        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value

        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "b2c3d4e5-f6a7-8901-bcde-f12345678901"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(
            """{"evseId":1,"chargingProfile":{"id":99,"stackLevel":0,"chargingProfilePurpose":"TxDefaultProfile","chargingProfileKind":"Absolute","chargingSchedule":[{"id":1,"chargingRateUnit":"A","chargingSchedulePeriod":[{"startPeriod":0,"limit":16.789,"numberPhases":3},{"startPeriod":3600,"limit":10.456,"numberPhases":3}],"minChargingRate":6.999}]}}"""
        )

        val request = messageSerializer.deserializePayload(ocppMessage, SetChargingProfileRequest::class.java)
        request.shouldBeInstanceOf<ParsingResult.Success<SetChargingProfileRequest>>()
        val payload = request.value
        payload.evseId shouldBe 1L
        payload.chargingProfile shouldNotBeNull {
            id shouldBe 99
            stackLevel shouldBe 0
            chargingProfilePurpose shouldBe ChargingProfilePurpose.TxDefaultProfile
            chargingProfileKind shouldBe ChargingProfileKind.Absolute
            chargingSchedule shouldNotBeNull {
                size shouldBe 1
                this[0].chargingRateUnit shouldBe ChargingRateUnit.A
                this[0].minChargingRate shouldBe 6.9
                this[0].chargingSchedulePeriod shouldNotBeNull {
                    size shouldBe 2
                    this[0].startPeriod shouldBe 0
                    this[0].limit shouldBe 16.7
                    this[0].numberPhases shouldBe 3
                    this[1].startPeriod shouldBe 3600
                    this[1].limit shouldBe 10.4
                    this[1].numberPhases shouldBe 3
                }
            }
        }
    }

    "serialize a set charging profile request and floor limit and minChargingRate to 1 decimal" {
        val request = SetChargingProfileRequest(
            evseId = 1L,
            chargingProfile = ChargingProfile(
                id = 99,
                stackLevel = 0,
                chargingProfilePurpose = ChargingProfilePurpose.TxDefaultProfile,
                chargingProfileKind = ChargingProfileKind.Absolute,
                chargingSchedule = listOf(
                    ChargingSchedule(
                        id = 1,
                        chargingRateUnit = ChargingRateUnit.A,
                        chargingSchedulePeriod = listOf(
                            ChargingSchedulePeriod(startPeriod = 0, limit = 16.789, numberPhases = 3),
                            ChargingSchedulePeriod(startPeriod = 3600, limit = 10.456, numberPhases = 3)
                        ),
                        minChargingRate = 6.999
                    )
                )
            )
        )

        messageSerializer.toPayloadString(request) shouldBe
            """{"evseId":1,"chargingProfile":{"id":99,"stackLevel":0,"chargingProfilePurpose":"TxDefaultProfile","chargingProfileKind":"Absolute","chargingSchedule":[{"id":1,"chargingRateUnit":"A","chargingSchedulePeriod":[{"startPeriod":0,"limit":16.7,"numberPhases":3},{"startPeriod":3600,"limit":10.4,"numberPhases":3}],"minChargingRate":6.9}]}}"""
    }
})
