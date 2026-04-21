package com.monta.library.ocpp.profiles.serialization

import com.monta.library.ocpp.TestUtils
import com.monta.library.ocpp.common.chargingprofile.ChargingProfileKind
import com.monta.library.ocpp.common.chargingprofile.ChargingRateUnit
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import com.monta.library.ocpp.v16.smartcharge.ChargingProfile
import com.monta.library.ocpp.v16.smartcharge.ChargingProfilePurposeType
import com.monta.library.ocpp.v16.smartcharge.ChargingSchedule
import com.monta.library.ocpp.v16.smartcharge.ChargingSchedulePeriod
import com.monta.library.ocpp.v16.smartcharge.SetChargingProfileRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SetChargingProfileSerializationTest : StringSpec({
    val messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)

    "parse a valid set charging profile request and floor limit and minChargingRate to 1 decimal" {
        val jsonString = TestUtils.getFileAsString("smartcharge/set-charging-profile-req.json")
        val parsingResult = messageSerializer.parse(jsonString)

        parsingResult.shouldBeInstanceOf<ParsingResult.Success<Message.Request>>()
        val ocppMessage = parsingResult.value

        ocppMessage.shouldBeInstanceOf<Message.Request>()
        ocppMessage.uniqueId shouldBe "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        ocppMessage.payload shouldBe TestUtils.toJsonNode(
            """{"connectorId":1,"csChargingProfiles":{"chargingProfileId":42,"stackLevel":0,"chargingProfilePurpose":"TxDefaultProfile","chargingProfileKind":"Absolute","chargingSchedule":{"chargingRateUnit":"A","chargingSchedulePeriod":[{"startPeriod":0,"limit":16.789,"numberPhases":3},{"startPeriod":3600,"limit":10.456,"numberPhases":3}],"minChargingRate":6.999}}}"""
        )

        val request = messageSerializer.deserializePayload(ocppMessage, SetChargingProfileRequest::class.java)
        request.shouldBeInstanceOf<ParsingResult.Success<SetChargingProfileRequest>>()
        val payload = request.value
        payload.connectorId shouldBe 1
        payload.csChargingProfiles shouldNotBeNull {
            chargingProfileId shouldBe 42
            stackLevel shouldBe 0
            chargingProfilePurpose shouldBe ChargingProfilePurposeType.TxDefaultProfile
            chargingProfileKind shouldBe ChargingProfileKind.Absolute
            chargingSchedule shouldNotBeNull {
                chargingRateUnit shouldBe ChargingRateUnit.A
                minChargingRate shouldBe 7.0
                chargingSchedulePeriod shouldNotBeNull {
                    size shouldBe 2
                    this[0].startPeriod shouldBe 0
                    this[0].limit shouldBe 16.8
                    this[0].numberPhases shouldBe 3
                    this[1].startPeriod shouldBe 3600
                    this[1].limit shouldBe 10.5
                    this[1].numberPhases shouldBe 3
                }
            }
        }
    }

    "serialize a set charging profile request and floor limit and minChargingRate to 1 decimal" {
        val request = SetChargingProfileRequest(
            connectorId = 1,
            csChargingProfiles = ChargingProfile(
                chargingProfileId = 42,
                stackLevel = 0,
                chargingProfilePurpose = ChargingProfilePurposeType.TxDefaultProfile,
                chargingProfileKind = ChargingProfileKind.Absolute,
                chargingSchedule = ChargingSchedule(
                    chargingRateUnit = ChargingRateUnit.A,
                    chargingSchedulePeriod = listOf(
                        ChargingSchedulePeriod(startPeriod = 0, limit = 16.789, numberPhases = 3),
                        ChargingSchedulePeriod(startPeriod = 3600, limit = 10.456, numberPhases = 3)
                    ),
                    minChargingRate = 6.999
                )
            )
        )

        messageSerializer.toPayloadString(request) shouldBe
            """{"connectorId":1,"csChargingProfiles":{"chargingProfileId":42,"stackLevel":0,"chargingProfilePurpose":"TxDefaultProfile","chargingProfileKind":"Absolute","chargingSchedule":{"chargingRateUnit":"A","chargingSchedulePeriod":[{"startPeriod":0,"limit":16.8,"numberPhases":3},{"startPeriod":3600,"limit":10.5,"numberPhases":3}],"minChargingRate":7.0}}}"""
    }
})
