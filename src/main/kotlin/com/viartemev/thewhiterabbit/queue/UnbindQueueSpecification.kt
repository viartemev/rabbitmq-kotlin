package com.viartemev.thewhiterabbit.queue

/**
 * A queue unbinding specification
 */
data class UnbindQueueSpecification(
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
    val routingKey: String,
    /**
     * Other properties (binding parameters)
     */
    val arguments: Map<String, Any> = emptyMap()
)
