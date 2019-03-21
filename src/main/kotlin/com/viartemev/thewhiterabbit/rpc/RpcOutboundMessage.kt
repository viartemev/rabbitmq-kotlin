package com.viartemev.thewhiterabbit.rpc

class RpcOutboundMessage(
    val exchangeName: String,
    val requestQueueName: String,
    val replyQueueName: String,
    val body: ByteArray
)
