package io.github.viartemev.rabbitmq.exchange

/**
 * Represents the specification for binding an exchange to a destination with optional arguments.
 *
 * @property source The source exchange to bind.
 * @property destination The destination exchange where the binding will be created.
 * @property noWait Indicates whether the client should wait for the server's response.
 * @property routingKey The routing key for the binding.
 * @property arguments Additional arguments for the binding.
 */
data class BindExchangeSpecification(
    val source: String,
    val destination: String,
    val noWait: Boolean = false,
    val routingKey: String,
    val arguments: Map<String, Any> = emptyMap()
)
