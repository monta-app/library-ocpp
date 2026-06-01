package com.monta.library.ocpp

import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper

object TestUtils {
    private val objectMapper = jacksonObjectMapper()
    const val ASYNC_TIMEOUT_MS: Long = 5000

    fun getFileAsString(filename: String): String {
        return requireNotNull(object {}.javaClass.classLoader.getResource(filename)).readText()
    }

    fun toJsonNode(value: String): JsonNode {
        return objectMapper.readTree(value)
    }
}
