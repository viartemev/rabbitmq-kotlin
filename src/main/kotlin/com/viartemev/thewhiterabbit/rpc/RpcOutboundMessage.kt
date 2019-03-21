package com.viartemev.thewhiterabbit.rpc

import com.rabbitmq.client.AMQP

class RpcOutboundMessage(
    val exchangeName: String,
    val requestQueueName: String,
    val replyQueueName: String,
    val properties: AMQP.BasicProperties,
    val body: ByteArray
)
