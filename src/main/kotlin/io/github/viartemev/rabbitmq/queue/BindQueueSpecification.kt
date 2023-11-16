package io.github.viartemev.rabbitmq.queue

/**
 * Represents the specifications for binding a queue to an exchange.
 *
 * @property queue The name of the queue to bind.
 * @property exchange The name of the exchange to bind the queue to.
 * @property routingKey The routing key to use for binding. Default is an empty string.
 * @property noWait Determines whether to wait for a confirmation from the server. Default is `false`.
 * @property arguments The additional arguments to use for binding. Default is an empty map.
 */
data class BindQueueSpecification(
    val queue: String,
    val exchange: String,
    val routingKey: String = "",
    val noWait: Boolean = false,
    val arguments: Map<String, Any> = emptyMap()
)
