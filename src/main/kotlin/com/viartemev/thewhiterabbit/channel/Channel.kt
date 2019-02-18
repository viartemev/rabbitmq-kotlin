package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.viartemev.thewhiterabbit.consumer.ConfirmConsumer

fun Channel.consumer(queue: String, prefetchSize: Int) = ConfirmConsumer(this, queue, prefetchSize)

suspend fun Connection.channel(block: suspend Channel.() -> Unit): Channel {
    val channel = this.createChannel()
    channel.use { block(it) }
    return channel
}

suspend fun Channel.consume(queue: String, prefetchSize: Int = 0, block: suspend ConfirmConsumer.() -> Unit) {
    val consumer = this.consumer(queue, prefetchSize)
    try {
        block(consumer)
    } finally {
        consumer.cancel()
    }
}
