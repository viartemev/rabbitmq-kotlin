package com.viartemev.thewhiterabbit.example.rpc

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery

fun main() {
    val connectionFactory = ConnectionFactory().apply { useNio() }
    val connection = connectionFactory.newConnection()
    val channel = connection.createChannel()
    val rpcQueueName = "rpc_request"
    val rpcServer = object : com.rabbitmq.client.RpcServer(channel, rpcQueueName) {
        override fun handleCall(request: Delivery?, replyProperties: AMQP.BasicProperties?): ByteArray {
            return request?.body?.let {
                ("Hello, " + String(it)).toByteArray()
            } ?: "Body is empty".toByteArray()
        }
    }
    rpcServer.mainloop()
}
