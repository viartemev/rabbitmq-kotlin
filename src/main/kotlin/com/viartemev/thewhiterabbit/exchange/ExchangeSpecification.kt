package com.viartemev.thewhiterabbit.exchange

import com.rabbitmq.client.BuiltinExchangeType

/**
 * An exchange declaration specification
 */
data class ExchangeSpecification(
    /**
     * The name of the exchange
     */
    val name: String,
    /**
     * The exchange type
     */
    val type: BuiltinExchangeType = BuiltinExchangeType.DIRECT,
    /**
     * True if we are declaring a durable exchange (the exchange will survive a server restart)
     */
    val durable: Boolean = false,
    /**
     * True if the server should delete the exchange when it is no longer in use
     */
    val autoDelete: Boolean = false,
    /**
     * True if the exchange is internal, i.e. can't be directly published to by a client
     */
    val internal: Boolean = false,
    /**
     * Other properties (construction arguments) for the exchange
     */
    val arguments: Map<String, Any> = emptyMap()
)
