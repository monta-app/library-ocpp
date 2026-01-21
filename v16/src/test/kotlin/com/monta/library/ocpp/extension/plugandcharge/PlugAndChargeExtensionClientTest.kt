package com.monta.library.ocpp.extension.plugandcharge

import com.monta.library.ocpp.TestUtils.ASYNC_TIMEOUT_MS
import com.monta.library.ocpp.common.profile.OcppConfirmation
import com.monta.library.ocpp.common.serialization.Message
import com.monta.library.ocpp.common.serialization.MessageSerializer
import com.monta.library.ocpp.common.serialization.ParsingResult
import com.monta.library.ocpp.common.serialization.SerializationMode
import com.monta.library.ocpp.common.session.OcppSession
import com.monta.library.ocpp.v16.core.DataTransferConfirmation
import com.monta.library.ocpp.v16.core.DataTransferRequest
import com.monta.library.ocpp.v16.core.DataTransferStatus
import com.monta.library.ocpp.v16.error.OcppErrorResponderV16
import com.monta.library.ocpp.v16.extension.plugandcharge.features.CertificateSignedConfirmation
import com.monta.library.ocpp.v16.extension.plugandcharge.features.CertificateSignedFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.CertificateSignedRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.DeleteCertificateConfirmation
import com.monta.library.ocpp.v16.extension.plugandcharge.features.DeleteCertificateFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.DeleteCertificateRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.GetInstalledCertificateIdsConfirmation
import com.monta.library.ocpp.v16.extension.plugandcharge.features.GetInstalledCertificateIdsFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.GetInstalledCertificateIdsRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.InstallCertificateConfirmation
import com.monta.library.ocpp.v16.extension.plugandcharge.features.InstallCertificateFeature
import com.monta.library.ocpp.v16.extension.plugandcharge.features.InstallCertificateRequest
import com.monta.library.ocpp.v16.extension.plugandcharge.features.InstallCertificateUse
import com.monta.library.ocpp.v16.extension.plugandcharge.model.CertificateHashData
import com.monta.library.ocpp.v16.extension.plugandcharge.model.CertificateHashDataChain
import com.monta.library.ocpp.v16.extension.plugandcharge.model.GetCertificateIdUse
import com.monta.library.ocpp.v16.extension.plugandcharge.model.HashAlgorithmType
import com.monta.library.ocpp.v16.server.OcppServerV16
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.UUID

class PlugAndChargeExtensionClientTest : StringSpec() {
    private lateinit var replyConsumerThread: Thread
    private lateinit var getInstalledSession: OcppSession.Info
    private lateinit var certificateSignedSession: OcppSession.Info
    private lateinit var deleteCertificateSession: OcppSession.Info
    private lateinit var installCertificateSession: OcppSession.Info
    private lateinit var messageSerializer: MessageSerializer
    private lateinit var sendRequestChannel: Channel<Message.Request>
    private lateinit var sendResponseChannel: Channel<Message.Response>
    private lateinit var sendErrorChannel: Channel<Message.Error>
    private lateinit var server: OcppServerV16
    private lateinit var profile: PnCProfileListener

    init {
        "GetInstalledCertificateIdsConfirmation can be sent" {
            val conf = withTimeout(ASYNC_TIMEOUT_MS) {
                async {
                    server.asPlugAndChargeProfile(getInstalledSession)
                        .getInstalledCertificateIds(
                            GetInstalledCertificateIdsRequest(
                                listOf(
                                    GetCertificateIdUse.V2GCertificateChain,
                                    GetCertificateIdUse.V2GRootCertificate
                                )
                            )
                        )
                }
            }.await()
            conf.status shouldBe GetInstalledCertificateIdsConfirmation.Status.Accepted
            conf.certificateHashDataChain?.size shouldBe 2
            conf.certificateHashDataChain?.get(0)?.certificateType shouldBe GetCertificateIdUse.V2GCertificateChain
        }

        "CertificateSigned can be sent" {
            val conf = withTimeout(ASYNC_TIMEOUT_MS) {
                async {
                    server.asPlugAndChargeProfile(certificateSignedSession)
                        .certificateSigned(
                            CertificateSignedRequest(
                                "pem cert"
                            )
                        )
                }
            }.await()
            conf.status shouldBe CertificateSignedConfirmation.Status.Accepted
        }

        "DeleteCertificate can be sent" {
            val conf = withTimeout(ASYNC_TIMEOUT_MS) {
                async {
                    server.asPlugAndChargeProfile(deleteCertificateSession)
                        .deleteCertificate(
                            DeleteCertificateRequest(
                                CertificateHashData(
                                    HashAlgorithmType.SHA256,
                                    "name",
                                    "key",
                                    "42"
                                )
                            )
                        )
                }
            }.await()
            conf.status shouldBe DeleteCertificateConfirmation.Status.Accepted
        }

        "InstallCertificate can be sent" {
            val conf = withTimeout(ASYNC_TIMEOUT_MS) {
                async {
                    server.asPlugAndChargeProfile(installCertificateSession)
                        .installCertificate(
                            InstallCertificateRequest(
                                InstallCertificateUse.V2GRootCertificate,
                                "pem cert"
                            )
                        )
                }
            }.await()
            conf.status shouldBe InstallCertificateConfirmation.Status.Accepted
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

        getInstalledSession = createSession()
        certificateSignedSession = createSession()
        deleteCertificateSession = createSession()
        installCertificateSession = createSession()

        replyConsumerThread = Thread(consumer())
        replyConsumerThread.start()
        super.beforeSpec(spec)
    }

    override fun afterSpec(f: suspend (Spec) -> Unit) {
        replyConsumerThread.interrupt()
        super.afterSpec(f)
    }

    private fun createSession(): OcppSession.Info {
        return OcppSession.Info(
            "server1",
            UUID.randomUUID().toString(),
            OcppSession.OcppVersion.V1_6,
            OcppSession.Index()
        )
    }

    private val mockGetInstalledCertificateIdsConfirmation = GetInstalledCertificateIdsConfirmation(
        status = GetInstalledCertificateIdsConfirmation.Status.Accepted,
        certificateHashDataChain = listOf(
            CertificateHashDataChain(
                certificateType = GetCertificateIdUse.V2GCertificateChain,
                certificateHashData = CertificateHashData( // Contract Certificate (Leaf)
                    hashAlgorithm = HashAlgorithmType.SHA256,
                    issuerNameHash = "9cebaef513b4357e46b8977744d0ed2ec91ce1e5866658ef4991d7a16afc41f9",
                    issuerKeyHash = "f8c198974f525d085db8da28447f8e63316f7954120ac4ade70e6625a43ce4e0",
                    serialNumber = "3"
                ),
                childCertificateHashData = listOf(
                    CertificateHashData( // Sub CA
                        hashAlgorithm = HashAlgorithmType.SHA256,
                        issuerNameHash = "9cebaef513b4357e46b8977744d0ed2ec91ce1e5866658ef4991d7a16afc41f9",
                        issuerKeyHash = "f8c198974f525d085db8da28447f8e63316f7954120ac4ade70e6625a43ce4e0",
                        serialNumber = "2"
                    )
                )
            ),
            CertificateHashDataChain(
                certificateType = GetCertificateIdUse.V2GRootCertificate,
                certificateHashData = CertificateHashData( // V2G Root Certificate itself
                    hashAlgorithm = HashAlgorithmType.SHA256,
                    issuerNameHash = "9cebaef513b4357e46b8977744d0ed2ec91ce1e5866658ef4991d7a16afc41f9",
                    issuerKeyHash = "f8c198974f525d085db8da28447f8e63316f7954120ac4ade70e6625a43ce4e0",
                    serialNumber = "1"
                ),
                childCertificateHashData = emptyList()
            )
        )
    )

    private fun <T> verifyRequestNotNull(data: String?, clazz: Class<T>) {
        requireNotNull(
            messageSerializer.parseDataTransferExtension(
                data,
                clazz
            )
        )
    }

    private fun consumer(): Runnable {
        return Runnable {
            runBlocking {
                sendRequestChannel.consumeEach { msg ->
                    val req = messageSerializer.deserializePayload(
                        msg,
                        DataTransferRequest::class.java
                    ) as ParsingResult.Success
                    val pncConf: OcppConfirmation = when (req.value.messageId) {
                        GetInstalledCertificateIdsFeature.name -> {
                            verifyRequestNotNull(req.value.data, GetInstalledCertificateIdsRequest::class.java)
                            mockGetInstalledCertificateIdsConfirmation
                        }

                        CertificateSignedFeature.name -> {
                            verifyRequestNotNull(req.value.data, CertificateSignedRequest::class.java)
                            CertificateSignedConfirmation(CertificateSignedConfirmation.Status.Accepted)
                        }

                        DeleteCertificateFeature.name -> {
                            verifyRequestNotNull(req.value.data, DeleteCertificateRequest::class.java)
                            DeleteCertificateConfirmation(DeleteCertificateConfirmation.Status.Accepted)
                        }

                        InstallCertificateFeature.name -> {
                            verifyRequestNotNull(req.value.data, InstallCertificateRequest::class.java)
                            InstallCertificateConfirmation(InstallCertificateConfirmation.Status.Accepted)
                        }

                        else -> throw AssertionError("unhandled msg $msg")
                    }
                    val dtConf = DataTransferConfirmation(
                        status = DataTransferStatus.Accepted,
                        data = messageSerializer.toPayloadString(pncConf)
                    )
                    val session = sessionByMessageId(req.value.messageId)
                    server.receiveMessage(
                        session,
                        Message.Response(
                            msg.uniqueId,
                            messageSerializer.toPayload(dtConf)
                        )
                    )
                }
            }
        }
    }

    private fun sessionByMessageId(messageId: String?): OcppSession.Info {
        return when (messageId) {
            GetInstalledCertificateIdsFeature.name -> getInstalledSession
            CertificateSignedFeature.name -> certificateSignedSession
            DeleteCertificateFeature.name -> deleteCertificateSession
            InstallCertificateFeature.name -> installCertificateSession
            else -> throw AssertionError("should never happen")
        }
    }
}
