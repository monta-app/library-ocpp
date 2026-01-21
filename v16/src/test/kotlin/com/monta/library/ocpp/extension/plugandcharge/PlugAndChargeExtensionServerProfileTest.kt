package com.monta.library.ocpp.extension.plugandcharge

import com.monta.library.ocpp.TestUtils.ASYNC_TIMEOUT_MS
import com.monta.library.ocpp.common.profile.Feature
import com.monta.library.ocpp.common.profile.OcppConfirmation
import com.monta.library.ocpp.common.profile.OcppRequest
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.common.session.OcppSession
import com.monta.library.ocpp.common.transport.OcppCallException
import com.monta.library.ocpp.v16.AuthorizationStatus
import com.monta.library.ocpp.v16.IdTagInfo
import com.monta.library.ocpp.v16.core.DataTransferConfirmation
import com.monta.library.ocpp.v16.core.DataTransferFeature
import com.monta.library.ocpp.v16.core.DataTransferRequest
import com.monta.library.ocpp.v16.core.DataTransferStatus
import com.monta.library.ocpp.v16.error.MessageErrorCodeV16
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import com.monta.library.ocpp.v16.extension.plugandcharge.PlugAndChargeExtensionServerProfile
import com.monta.library.ocpp.v16.extension.plugandcharge.PlugAndChargeExtensionServerProfile.Companion.FEATURE_NAME_MAP
import com.monta.library.ocpp.v16.extension.plugandcharge.features.CertificateSignedFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.CertificateSignedRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.DeleteCertificateFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.DeleteCertificateRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.Get15118EVCertificateConfirmation
import com.monta.library.ocpp.v16.extension.plugandcharge.features.Get15118EVCertificateFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.Get15118EVCertificateRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.GetCertificateStatusConfirmation
import com.monta.library.ocpp.v16.extension.plugandcharge.features.GetCertificateStatusFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.GetCertificateStatusRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.GetInstalledCertificateIdsFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.GetInstalledCertificateIdsRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.InstallCertificateFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.InstallCertificateRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.InstallCertificateUse
import com.monta.library.ocpp.v16.extension.plugandcharge.features.PncAuthorizeConfirmation
import com.monta.library.ocpp.v16.extension.plugandcharge.features.PncAuthorizeFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.PncAuthorizeRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.PncTriggerMessageFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.PncTriggerMessageRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.SignCertificateConfirmation
import com.monta.library.ocpp.v16.extension.plugandcharge.features.SignCertificateFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.SignCertificateRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.model.CertificateHashData
import com.monta.library.ocpp.v16.extension.plugandcharge.model.GetCertificateIdUse
import com.monta.library.ocpp.v16.extension.plugandcharge.model.HashAlgorithmType
import com.monta.library.ocpp.v16.extension.plugandcharge.model.OCSPRequestData
import com.monta.library.ocpp.v16.server.OcppServerV16
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.time.ZonedDateTime
import java.util.UUID

class PlugAndChargeExtensionServerProfileTest : StringSpec() {
    private lateinit var messageSerializer: MessageSerializer
    private lateinit var sendRequestChannel: Channel<Message.Request>
    private lateinit var sendResponseChannel: Channel<Message.Response>
    private lateinit var sendErrorChannel: Channel<Message.Error>
    private lateinit var server: OcppServerV16
    private lateinit var profile: PnCProfileListener

    init {
        "invalid data transfer should cause error" {
            val session = createSession()
            runBlocking {
                withTimeout(ASYNC_TIMEOUT_MS) {
                    async {
                        server.receiveMessage(session, """[2,"test","DataTransfer",{}]""")
                    }.await() // let exceptions bobble up.
                }
            }
            val error = withTimeout(ASYNC_TIMEOUT_MS) { sendErrorChannel.receive() }
            error.errorCode shouldBe MessageErrorCodeV16.FormationViolation.name
        }

        /*
         * Verify we can deserialize requests and serialize confirmations.
         */
        "server can receive PncAuthorizeRequest" {
            val dtConf = dtReqExpectConf(
                DataTransferRequest(
                    vendorId = PlugAndChargeExtensionServerProfile.PNC_VENDOR_ID,
                    messageId = PncAuthorizeFeature.name,
                    data = messageSerializer.toPayloadString(
                        PncAuthorizeRequest(
                            certificate = acceptedVehicleCertificatePem(),
                            idToken = PncAuthorizeRequest.IdToken("DEICECMONTAQA5"),
                            iso15118CertificateHashData = listOf(ocspRequest())
                        )
                    )
                )
            )
            val conf =
                messageSerializer.parseDataTransferExtension(dtConf.data, PncAuthorizeConfirmation::class.java)
            conf.certificateStatus shouldBe PncAuthorizeConfirmation.Status.Accepted
            conf.idTokenInfo.status shouldBe AuthorizationStatus.Accepted
        }

        "server can receive Get15118EVCertificate" {
            val dtConf = dtReqExpectConf(
                DataTransferRequest(
                    vendorId = PlugAndChargeExtensionServerProfile.PNC_VENDOR_ID,
                    messageId = Get15118EVCertificateFeature.name,
                    data = messageSerializer.toPayloadString(
                        Get15118EVCertificateRequest(
                            iso15118SchemaVersion = "urn:iso:15118:2:2013:MsgDef", // ISO 15118:2 / prime256v1
                            action = Get15118EVCertificateRequest.Action.Install,
                            exiRequest = "base64 exi raw request from vehicle"
                        )
                    )
                )
            )
            val conf =
                messageSerializer.parseDataTransferExtension(dtConf.data, Get15118EVCertificateConfirmation::class.java)
            conf.status shouldBe Get15118EVCertificateConfirmation.Status.Accepted
            conf.exiResponse shouldBe "CertificateInstallationRes (ISO 15118:2) exi in base64"
        }

        "server can receive GetCertificateStatus" {
            val dtConf = dtReqExpectConf(
                DataTransferRequest(
                    vendorId = PlugAndChargeExtensionServerProfile.PNC_VENDOR_ID,
                    messageId = GetCertificateStatusFeature.name,
                    data = messageSerializer.toPayloadString(GetCertificateStatusRequest(ocspRequest()))
                )
            )
            val conf =
                messageSerializer.parseDataTransferExtension(dtConf.data, GetCertificateStatusConfirmation::class.java)
            conf.status shouldBe GetCertificateStatusConfirmation.Status.Accepted
        }

        "server can receive SignCertificate" {
            val dtConf = dtReqExpectConf(
                DataTransferRequest(
                    vendorId = PlugAndChargeExtensionServerProfile.PNC_VENDOR_ID,
                    messageId = SignCertificateFeature.name,
                    data = messageSerializer.toPayloadString(SignCertificateRequest("csr"))
                )
            )
            val conf =
                messageSerializer.parseDataTransferExtension(dtConf.data, SignCertificateConfirmation::class.java)
            conf.status shouldBe SignCertificateConfirmation.Status.Accepted
        }

        "server can reject SignCertificate with ExtensionDisabled" {
            val dtConf = dtReqExpectConf(
                DataTransferRequest(
                    vendorId = PlugAndChargeExtensionServerProfile.PNC_VENDOR_ID,
                    messageId = SignCertificateFeature.name,
                    data = messageSerializer.toPayloadString(SignCertificateRequest("not-enabled"))
                )
            )
            dtConf.status shouldBe DataTransferStatus.UnknownVendorId
            dtConf.data shouldBe null
        }

        /*
         * Verify that the Central System does not accept messages intended for the Charging Station.
         */

        "central system should not accept CertificateSigned request" {
            dtReqExpectConf(
                DataTransferRequest(
                    vendorId = PlugAndChargeExtensionServerProfile.PNC_VENDOR_ID,
                    messageId = CertificateSignedFeature.name,
                    data = messageSerializer.toPayloadString(CertificateSignedRequest("cert chain in PEM"))
                )
            ).status shouldBe DataTransferStatus.Rejected
        }

        "central system should not accept DeleteCertificate request" {
            dtReqExpectConf(
                DataTransferRequest(
                    vendorId = PlugAndChargeExtensionServerProfile.PNC_VENDOR_ID,
                    messageId = DeleteCertificateFeature.name,
                    data = messageSerializer.toPayloadString(
                        DeleteCertificateRequest(
                            certificateHashData = CertificateHashData(
                                HashAlgorithmType.SHA256,
                                issuerNameHash = "test",
                                issuerKeyHash = "test",
                                serialNumber = "1000"
                            )
                        )
                    )
                )
            ).status shouldBe DataTransferStatus.Rejected
        }

        "central system should not accept GetInstalledCertificateIds request" {
            dtReqExpectConf(
                DataTransferRequest(
                    vendorId = PlugAndChargeExtensionServerProfile.PNC_VENDOR_ID,
                    messageId = GetInstalledCertificateIdsFeature.name,
                    data = messageSerializer.toPayloadString(
                        GetInstalledCertificateIdsRequest(
                            certificateType = listOf(GetCertificateIdUse.V2GCertificateChain)
                        )
                    )
                )
            ).status shouldBe DataTransferStatus.Rejected
        }

        "central system should not accept InstallCertificate request" {
            dtReqExpectConf(
                DataTransferRequest(
                    vendorId = PlugAndChargeExtensionServerProfile.PNC_VENDOR_ID,
                    messageId = InstallCertificateFeature.name,
                    data = messageSerializer.toPayloadString(
                        InstallCertificateRequest(
                            certificate = "test",
                            certificateType = InstallCertificateUse.V2GRootCertificate
                        )
                    )
                )
            ).status shouldBe DataTransferStatus.Rejected
        }

        "central system should not accept TriggerMessage request" {
            dtReqExpectConf(
                DataTransferRequest(
                    vendorId = PlugAndChargeExtensionServerProfile.PNC_VENDOR_ID,
                    messageId = PncTriggerMessageFeature.name,
                    data = messageSerializer.toPayloadString(PncTriggerMessageRequest())
                )
            ).status shouldBe DataTransferStatus.Rejected
        }

        "central system should not accept Authorize confirmation" {
            shouldThrow<OcppCallException> {
                confOfdtRequest(
                    PncAuthorizeConfirmation(
                        idTokenInfo = IdTagInfo(
                            status = AuthorizationStatus.Accepted,
                            expiryDate = ZonedDateTime.now().plusYears(1)
                        ),
                        certificateStatus = PncAuthorizeConfirmation.Status.Accepted
                    )
                )
            }.errorCode shouldBe MessageErrorCodeV16.FormationViolation
        }

        "central system should not accept Get15118EVCertificate confirmation" {
            shouldThrow<OcppCallException> {
                confOfdtRequest(
                    Get15118EVCertificateConfirmation(
                        status = Get15118EVCertificateConfirmation.Status.Accepted,
                        exiResponse = "base64 encoded raw EXI"
                    )
                )
            }.errorCode shouldBe MessageErrorCodeV16.FormationViolation
        }

        "central system should not accept GetCertificateStatus confirmation" {
            shouldThrow<OcppCallException> {
                confOfdtRequest(
                    GetCertificateStatusConfirmation(
                        status = GetCertificateStatusConfirmation.Status.Accepted,
                        ocspResult = "base64 encoded raw OCSP"
                    )
                )
            }.errorCode shouldBe MessageErrorCodeV16.FormationViolation
        }

        "central system should not accept SignCertificate confirmation" {
            shouldThrow<OcppCallException> {

                confOfdtRequest(
                    SignCertificateConfirmation(
                        status = SignCertificateConfirmation.Status.Accepted
                    )
                )
            }.errorCode shouldBe MessageErrorCodeV16.FormationViolation
        }
    }

    override suspend fun beforeSpec(spec: Spec) {
        messageSerializer = MessageSerializer(SerializationMode.OCPP_1_6, OcppErrorResponderV16)
        sendRequestChannel = Channel(8)
        sendResponseChannel = Channel(8)
        sendErrorChannel = Channel(8)
        val res = PnCProfileListener.createServer(
            sendRequestChannel,
            sendResponseChannel,
            sendErrorChannel
        )
        server = res.first
        profile = res.second
        super.beforeSpec(spec)
    }

    private val requestClazzMap: Map<Class<out OcppRequest>, Feature> =
        FEATURE_NAME_MAP.values.associateBy { it.requestType }
    private val confirmationClazzMap: Map<Class<out OcppConfirmation>, Feature> =
        FEATURE_NAME_MAP.values.associateBy { it.confirmationType }

    private fun createSession(): OcppSession.Info {
        return OcppSession.Info("server1", "DFSD123123", OcppSession.OcppVersion.V1_6, OcppSession.Index())
    }

    private fun msgId() = UUID.randomUUID().toString()
    private suspend fun dtReqExpectConf(
        dtReq: DataTransferRequest,
        msgIdProvider: () -> String = { msgId() }
    ): DataTransferConfirmation {
        return reqExpectConf(
            Message.Request(
                msgIdProvider(),
                DataTransferFeature.name,
                messageSerializer.toPayload(dtReq)
            )
        )
    }

    private suspend fun reqExpectConf(req: Message): DataTransferConfirmation {
        val session = createSession()
        withTimeout(ASYNC_TIMEOUT_MS) {
            async {
                server.receiveMessage(session, req)
            }.await() // let exceptions bobble up.
        }
        val msg = withTimeout(ASYNC_TIMEOUT_MS) { sendResponseChannel.receive() }
        val dtConf =
            messageSerializer.deserializePayload(msg, DataTransferConfirmation::class.java) as ParsingResult.Success
        return dtConf.value
    }

    private suspend fun confOfdtRequest(
        conf: OcppConfirmation,
        msgIdProvider: () -> String = { msgId() }
    ): DataTransferConfirmation {
        val dtRequest = pncDataTransferConfirmation(conf)
        return reqExpectConf(
            Message.Request(
                msgIdProvider(),
                DataTransferFeature.name,
                messageSerializer.toPayload(dtRequest)
            )
        )
    }

    private fun pncDataTransferConfirmation(conf: OcppConfirmation): DataTransferRequest {
        return DataTransferRequest(
            vendorId = PlugAndChargeExtensionServerProfile.PNC_VENDOR_ID,
            messageId = confirmationClazzMap[conf.javaClass]?.name!!,
            data = messageSerializer.toPayloadString(conf)
        )
    }

    private fun ocspRequest() = OCSPRequestData(
        hashAlgorithm = HashAlgorithmType.SHA256,
        issuerNameHash = "",
        issuerKeyHash = "",
        serialNumber = "42",
        responderURL = "https://localhost"
    )

    private fun acceptedVehicleCertificatePem(): String {
        // Subject: CN=UKSWI123456791A, O=EVerest, C=DE, DC=MO
        return """
            -----BEGIN CERTIFICATE-----
            MIICZTCCAgugAwIBAgICMEQwCgYIKoZIzj0EAwIwVzEiMCAGA1UEAwwZUEtJLUV4
            dF9DUlRfTU9fU1VCMl9WQUxJRDEQMA4GA1UECgwHRVZlcmVzdDELMAkGA1UEBhMC
            REUxEjAQBgoJkiaJk/IsZAEZFgJNTzAeFw0yNDAyMTMwODQ1NTNaFw0yNjAyMTIw
            ODQ1NTNaME0xGDAWBgNVBAMMD1VLU1dJMTIzNDU2NzkxQTEQMA4GA1UECgwHRVZl
            cmVzdDELMAkGA1UEBhMCREUxEjAQBgoJkiaJk/IsZAEZFgJNTzBZMBMGByqGSM49
            AgEGCCqGSM49AwEHA0IABJqFJmUcWtUejtcmLKO3BnQrR/sVWBa+61jvzBVY/qOm
            4gKvxyJM2zlupb2MKxJ+ZEY6Y5zhqvrYWawwKVpjOJ6jgdAwgc0wDAYDVR0TAQH/
            BAIwADAOBgNVHQ8BAf8EBAMCA+gwHQYDVR0OBBYEFKjRU967lAvl6+U+bcAvJIUk
            d/2rMG0GCCsGAQUFBwEBBGEwXzAkBggrBgEFBQcwAYYYaHR0cHM6Ly93d3cuZXhh
            bXBsZS5jb20vMDcGCCsGAQUFBzAChitodHRwczovL3d3dy5leGFtcGxlLmNvbS9J
            bnRlcm1lZGlhdGUtQ0EuY2VyMB8GA1UdIwQYMBaAFH8IHoO89QDoSNZ/2gNhCuic
            +M2xMAoGCCqGSM49BAMCA0gAMEUCIGgNOBMLxt+l/j4WBNGNi3skcfLKZmrq/6iG
            Gq1OIBYeAiEAt7OVMymbwLv/2Fkv3w23bXruModmYL4D2la7cNtLaSI=
            -----END CERTIFICATE-----
        """.trimIndent()
    }
}
