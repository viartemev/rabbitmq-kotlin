package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.consumer.ConfirmConsumer
import com.viartemev.thewhiterabbit.rpc.RabbitMqMessage
import com.viartemev.thewhiterabbit.rpc.RpcClient

fun Channel.consumer(queue: String, prefetchSize: Int) = ConfirmConsumer(this, queue, prefetchSize)

suspend fun Channel.consume(queue: String, prefetchSize: Int = 0, block: suspend ConfirmConsumer.() -> Unit) =
    this.consumer(queue, prefetchSize).use { block(it) }


fun Channel.rpcClient() = RpcClient(this)

suspend fun Channel.rpc(block: suspend RpcClient.() -> RabbitMqMessage) = this.rpcClient().run { block() }
