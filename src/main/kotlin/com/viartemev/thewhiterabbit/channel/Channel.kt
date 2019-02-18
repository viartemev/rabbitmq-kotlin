package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.viartemev.thewhiterabbit.consumer.ConfirmConsumer
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

private val localChannels = ConcurrentHashMap<Thread, UncloseableChannel>()
private val c = Cleaner
fun Channel.consumer(queue: String, prefetchSize: Int) = ConfirmConsumer(this, queue, prefetchSize)

suspend fun Connection.channel(block: suspend Channel.() -> Unit): Channel =
    localChannels
        .computeIfAbsent(Thread.currentThread()) { UncloseableChannel(this.createChannel()) }
        .also { block(it) }

suspend fun Channel.consume(queue: String, prefetchSize: Int = 0, block: suspend ConfirmConsumer.() -> Unit) {
    val consumer = this.consumer(queue, prefetchSize)
    try {
        block(consumer)
    } finally {
        consumer.cancel()
    }
}

private class UncloseableChannel(private val channel: Channel) : Channel by channel {
    override fun close() {}

    override fun close(closeCode: Int, closeMessage: String?) {}

    internal fun close0() = channel.close()
}

private object Cleaner {
    init {
        Runtime.getRuntime().addShutdownHook(thread {
            localChannels.values.forEach { it.close0() }
        })
    }
}
