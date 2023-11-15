package com.github.viartemev.rabbitmq.exchange

/**
 * Represents the specification of an exchange in a messaging system.
 *
 * @param name the name of the exchange
 * @param type the type of the exchange (default: ExchangeType.DIRECT)
 * @param durable specifies if the exchange should survive server restarts (default: false)
 * @param autoDelete specifies if the exchange should be automatically deleted when no longer in use (default: false)
 * @param internal specifies if the exchange should be used only for internal purposes (default: false)
 * @param arguments additional arguments for the exchange (default: empty map)
 */
data class ExchangeSpecification(
        val name: String,
        val type: ExchangeType = ExchangeType.DIRECT,
        val durable: Boolean = false,
        val autoDelete: Boolean = false,
        val internal: Boolean = false,
        val arguments: Map<String, Any> = emptyMap()
)
