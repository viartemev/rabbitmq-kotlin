package io.github.viartemev.rabbitmq.common

import com.rabbitmq.client.AMQP.BasicProperties

/**
 * Represents an outbound message to be sent to a RabbitMQ exchange.
 *
 * @property exchange the exchange to which the message should be sent
 * @property routingKey the routing key to be used for the message
 * @property properties additional properties of the message
 * @property msg the message data as a byte array
 * @constructor Creates a new instance of [OutboundMessage] with the specified parameters.
 *
 * @param exchange the exchange to which the message should be sent
 * @param routingKey the routing key to be used for the message
 * @param properties additional properties of the message
 * @param msg the message data as a byte array
 *
 * @see BasicProperties
 */
data class OutboundMessage(
    val exchange: String,
    val routingKey: String,
    val properties: BasicProperties,
    val msg: ByteArray
) {
    constructor(
        exchange: String,
        routingKey: String,
        properties: BasicProperties,
        msg: String
    ) : this(exchange, routingKey, properties, msg.toByteArray())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OutboundMessage

        if (exchange != other.exchange) return false
        if (routingKey != other.routingKey) return false
        if (properties != other.properties) return false
        if (!msg.contentEquals(other.msg)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = exchange.hashCode()
        result = 31 * result + routingKey.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + msg.contentHashCode()
        return result
    }
}
