package io.github.viartemev.rabbitmq.exchange

/**
 * Represents the specification for unbinding an exchange from a queue.
 *
 * @param source The name of the exchange to unbind from.
 * @param destination The name of the queue to unbind the exchange from.
 * @param noWait Determines if the operation should be performed synchronously or not.
 * @param routingKey The routing key used for the unbinding operation.
 * @param arguments Additional arguments for the unbinding operation.
 */
data class UnbindExchangeSpecification(
    val source: String,
    val destination: String,
    val noWait: Boolean = false,
    val routingKey: String,
    val arguments: Map<String, Any> = emptyMap()
)
