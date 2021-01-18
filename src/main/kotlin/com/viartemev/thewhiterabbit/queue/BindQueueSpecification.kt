package com.viartemev.thewhiterabbit.queue

/**
 * A queue declaration specification
 */
data class BindQueueSpecification(
    /**
     * The name of the queue
     */
    val queue: String,
    /**
     * The name of the exchange
     */
    val exchange: String,
    /**
     * The routing key to use for the binding
     */
    val routingKey: String = "",
    /**
     * True to don't wait for a response from the server
     */
    val noWait: Boolean = false,
    /**
     * Other properties (binding parameters)
     */
    val arguments: Map<String, Any> = emptyMap()
)
