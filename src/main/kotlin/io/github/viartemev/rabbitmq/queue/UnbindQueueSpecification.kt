package io.github.viartemev.rabbitmq.queue

/**
 * Represents the specifications for unbinding a queue from an exchange.
 *
 * @property queue The name of the queue to unbind.
 * @property exchange The name of the exchange to unbind from.
 * @property routingKey The routing key used for the unbinding.
 * @property arguments Additional arguments for the unbinding operation.
 */
data class UnbindQueueSpecification(
        val queue: String,
        val exchange: String,
        val routingKey: String,
        val arguments: Map<String, Any> = emptyMap()
)
