package com.viartemev.thewhiterabbit.common

import com.rabbitmq.client.AMQP

class RabbitMqMessage(
    val properties: AMQP.BasicProperties,
    val body: ByteArray
)
