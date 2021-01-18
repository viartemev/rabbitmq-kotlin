package com.viartemev.thewhiterabbit.exchange

/**
 * An exchange binding specification
 */
data class BindExchangeSpecification(
    /**
     * The name of the exchange from which messages flow across the binding
     */
    val source: String,
    /**
     * The name of the exchange to which messages flow across the binding
     */
    val destination: String,
    /**
     * The routing key to use for the binding
     */
    val routingKey: String,
    /**
     * True to don't wait for a response from the server
     */
    val noWait: Boolean = false,
    /**
     * Other properties (binding parameters)
     */
    val arguments: Map<String, Any> = emptyMap()
)
