package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.AMQP.BasicProperties

data class OutboundMessage(
    val exchange: String,
    val routingKey: String,
    val properties: BasicProperties,
    val msg: String
)
