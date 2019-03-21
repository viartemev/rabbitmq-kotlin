package com.viartemev.thewhiterabbit.rpc

import com.rabbitmq.client.AMQP

class RpcInboundMessage(
    val properties: AMQP.BasicProperties,
    val body: ByteArray
)
