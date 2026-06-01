package com.monta.library.ocpp.common.serialization

import tools.jackson.databind.JsonNode

/**
 * I made up a word *_*
 */
interface Payloadable {
    val payload: JsonNode
}
