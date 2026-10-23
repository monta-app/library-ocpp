package com.monta.library.ocpp.v201.common

class CustomData : HashMap<String, Any?>() {

    val vendorId: String
        get() = requireNotNull(get("vendorId")) as String
}
