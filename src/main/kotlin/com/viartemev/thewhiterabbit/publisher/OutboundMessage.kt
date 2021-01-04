package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.AMQP.BasicProperties

data class OutboundMessage(
    val exchange: String,
    val routingKey: String,
    val properties: BasicProperties,
    //todo rename to body
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
