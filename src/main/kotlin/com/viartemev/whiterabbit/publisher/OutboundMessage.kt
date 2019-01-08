package com.viartemev.whiterabbit.publisher

import com.rabbitmq.client.AMQP.BasicProperties

//FIXME array in equals / hashcode
data class OutboundMessage(
        val exchange: String,
        val routingKey: String,
        val properties: BasicProperties,
        val body: ByteArray
)