# OCPP Library

### Setup

[![](https://www.jitpack.io/v/monta-app/library-ocpp.svg)](https://www.jitpack.io/#monta-app/library-ocpp)

Add jitpack to your repositories section in your gradle or maven build file

```kotlin
repositories {
    maven("https://jitpack.io")
}
```

OCPP V16

```kotlin
dependencies {
    implementation("com.monta.library.ocpp:core:<version>")
    implementation("com.monta.library.ocpp:v16:<version>")
}
```

OCPP V201

```kotlin
dependencies {
    implementation("com.monta.library.ocpp:core:<version>")
    implementation("com.monta.library.ocpp:v201:<version>")
}
```

## OCPP Message Direction

The OCPP protocol is an RPC framework with a **request-response** protocol

The *synchronicity* of the protocol is well described in OCPP-J-1.6 in chapter *4.1.1 Synchronicity*

In summary this describes that there can always only be one message in flight, in either direction.

### General Design Idea

The library should be seen as a library. I.e. it is a passive component.

* if any thread pools should be used, they should be passed in from the outside when creating an instance of the
  library.
* if any logging framework should be used it should also be passed in from the outside.

### Technology

* Use a simple JSON parsing library
  like [Moshi](https://github.com/square/moshi) ([guide](https://www.baeldung.com/java-json-moshi))

### Messages Initiated by the **charge point**

**Chapter 4 of the OCPP-1.6 specification**

The library should be usable both in a blocking and an asynchronous way for messages initiated by the charge point.

```kotlin
/**
 * A blocking core profile implementation. Today such an interface is found
 * in `com.monta.ocpp.profiles.core.CoreProfileListener`
 */
CoreProfileListenerSync {
    /**
     * @throws OCPPCallErrorException on unable to handle authorize
     */
    override fun authorize(
        request: AuthorizeRequest
    ): AuthorizeConfirmation
}

/**
 * An callback based core profile implementation
 */
CoreProfileListenerASync {
    override fun authorize(
        request: AuthorizeRequest,
        onError: (CallError) -> Unit,
        onSuccess: (AuthorizeConfirmation) -> Unit
    )
}
```

For both scenarios we need to figure out how to handle timeouts in our own code.

The implementation in the library should be relatively straight forward

1. Parse the incoming message into a Kotlin type
2. Find the profile that should handle it - if not supported, send a CallError to the charge point
3. Call the profile and let it handle e.g. the `Authorize`
4. When the library gets the `AuthorizeConfirmation` format it to a json string and send it back on the same WS
   connection it was received on

### Messages Initiated by the **OCPP Server**

**Chapter 5 of the OCPP-1.6 specification**

The library should be usable both in a blocking and an asynchronous way for messages initiated by the OCPP server.

```kotlin
/**
 * A blocking core profile implementation. Today such an implementation is found
 * in `com.monta.ocpp.service.ChargePointConnection`
 */
CoreProfileSync {
    /**
     * @throws OCPPCallErrorException on unable to handle reset
     */
    override fun reset(
        request: ResetRequest
    ): Future<ResetConfirmation>
}

/**
 * A callback core profile implementation. Maybe this can be implemented in a generic way
 * using `java.util.concurrent.CompletionStage`?
 */
CoreProfileASync {
    /**
     * `onSuccess` will be called by the OCPP library when a response is received from the
     * charge point.
     * `onError` will be called by the OCPP library if an error or no response is received
     * from the charge point
     */
    override fun reset(
        request: ResetRequest,
        onSuccess: (ResetConfirmation) -> Unit,
        onError: (CallError) -> Unit
    )
}
```

Messages initiated by us and sent to the charge point is a little bit more difficult.

1. The message should be send to an object that contains the WS session
2. Format the message to a OCPP json string
3. Send it over the WS connection
4. Synchronize so that when we receive the response object, we can parse it and either unblock the waiting thread or
   call the callback objects

A rough blocking implementation of this is today in `com.monta.ocpp.service.ChargePointConnection` which implements
`CoreProfile`

## OCPP 2.1 (`ocpp-v21`)

`com.monta.library.ocpp:ocpp-v21` holds the full OCPP 2.1 Edition 1 message model: all 91 actions, with the same layout
as `ocpp-v201` (`blocks/<functional block>/<Action>.kt` with `<Action>Feature`, `<Action>Request` and `<Action>Response`,
plus per-block `<Block>ServerDispatcher` / `<Block>ClientDispatcher`, `OcppServerV21Builder` and `OcppClientV21Builder`).

* **Generated, not hand-written.** `v21/codegen/generate_v21.py` reads the official OCA JSON schemas in `v21/schemas`
  and writes the Kotlin sources, the `V21Actions` registry (action, kebab-case slug, direction, block, feature) and
  schema-validated test samples. Run `python3 v21/codegen/generate_v21.py` from the repository root (needs the
  `jsonschema` package) and commit the result; never edit or reformat the generated files by hand.
  `SchemaRoundTripTest` checks that every request/response reads and writes its schema unchanged.
* **Types:** integer → `Long`, number → `BigDecimal`, date-time → `ZonedDateTime` (written with millisecond precision
  in UTC), optional fields are nullable without schema defaults, and wire names that Kotlin or Jackson would change
  (`setpoint_L2`, `ePriceLevel`, `isDefault`, enum values such as `Energy.Active.Import.Register`) carry `@JsonProperty`.
* **OCPP-J 2.1 frames:** `SerializationMode.OCPP_2_1` adds CALLRESULTERROR (`Message.CallResultError`, type 5) and SEND
  (`Message.Send`, type 6) and ignores any other unknown message type (`ParsingResult.Ignored`). SEND-only actions
  (`NotifyPeriodicEventStream`) implement `OcppUnconfirmedRequest`; they are dispatched but never answered, and are sent
  with `sendUnconfirmed`. The 1.6 and 2.0.1 modes are unchanged.
* **Version negotiation:** `OcppSession.OcppVersion.valueOf(protocols, default)` still only resolves 1.6 and 2.0.1, so
  existing servers keep their behaviour. Servers that speak 2.1 use
  `OcppSession.OcppVersion.negotiate(protocols, supportedVersions)`, which returns the first subprotocol the charging
  station offers that is supported (OCPP-J 2.1, part 4, 3.1).
