package com.viartemev.thewhiterabbit.rpc

data class RpcOutboundMessage(
    val exchangeName: String,
    val requestQueueName: String,
    val replyQueueName: String,
    val message: String
)
