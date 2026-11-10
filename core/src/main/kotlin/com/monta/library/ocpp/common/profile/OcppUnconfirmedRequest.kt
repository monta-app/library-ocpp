package com.monta.library.ocpp.common.profile

/**
 * Marker for OCPP 2.1 actions that are only ever sent as a SEND message (message type 6), e.g. NotifyPeriodicEventStream.
 *
 * These requests never get a response; their [Feature.confirmationType] is [NoConfirmation].
 */
interface OcppUnconfirmedRequest : OcppRequest

/**
 * Placeholder confirmation type for features of [OcppUnconfirmedRequest]s. It is never sent on the wire.
 */
object NoConfirmation : OcppConfirmation
