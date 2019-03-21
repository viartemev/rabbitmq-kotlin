package com.viartemev.thewhiterabbit.rpc

import com.rabbitmq.client.AMQP

class RabbitMqMessage(
    val properties: AMQP.BasicProperties,
    val body: ByteArray
)
