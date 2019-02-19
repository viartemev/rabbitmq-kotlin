package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.viartemev.thewhiterabbit.channel.Channels.localChannels
import com.viartemev.thewhiterabbit.channel.Channels.localConfirmChannels
import com.viartemev.thewhiterabbit.consumer.ConfirmConsumer
import com.viartemev.thewhiterabbit.publisher.ConfirmPublisher
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

object Channels {
    internal val localConfirmChannels = ConcurrentHashMap<Thread, UncloseableConfirmChannel>()
    internal val localChannels = ConcurrentHashMap<Thread, UncloseableChannel>()

    init {
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            sequenceOf(localChannels.values.asSequence(), localConfirmChannels.values.asSequence())
                .flatten()
                .forEach { it.close0() }
            localChannels.clear()
            localConfirmChannels.clear()
        })
    }


    private interface IAmUncloseableChannel : Channel {
        fun close0()
    }

    internal class UncloseableChannel(private val channel: Channel) : IAmUncloseableChannel, Channel by channel {
        override fun close() {

        }

        override fun close(closeCode: Int, closeMessage: String?) {}

        override fun close0() {
            if (channel.isOpen) channel.close()
        }
    }

    internal class UncloseableConfirmChannel(private val channel: ConfirmChannel) : ConfirmChannel(channel),
        IAmUncloseableChannel {
        override fun close() {}

        override fun close(closeCode: Int, closeMessage: String?) {}

        override fun close0() {
            if (channel.isOpen) channel.close()
        }
    }
}

/**
 * Create a channel with enabled publisher acknowledgements on it.Channel by channel
 * @see com.rabbitmq.client.Channel.confirmSelect()
 */
fun Connection.createConfirmChannel(): ConfirmChannel = ConfirmChannel(this.createChannel())

suspend fun Connection.confirmChannel(block: suspend ConfirmChannel.() -> Unit): ConfirmChannel {
    var channel = Channels
        .localConfirmChannels[Thread.currentThread()]
    if (channel == null || !channel.isOpen) {
        channel = Channels.UncloseableConfirmChannel(createConfirmChannel())
        localConfirmChannels[Thread.currentThread()] = channel
    }
    return channel
        .also { block(it) }
}

suspend fun ConfirmChannel.publish(block: suspend ConfirmPublisher.() -> Unit) {
    val publisher = this.publisher()
    block(publisher)
}

fun Channel.consumer(queue: String, prefetchSize: Int) = ConfirmConsumer(this, queue, prefetchSize)

suspend fun Connection.channel(block: suspend Channel.() -> Unit): Channel {
    var channel = Channels
        .localChannels[Thread.currentThread()]
    if (channel == null || !channel.isOpen) {
        channel = Channels.UncloseableChannel(createConfirmChannel())
        localChannels[Thread.currentThread()] = channel
    }
    return channel
        .also { block(it) }
}

suspend fun Channel.consume(queue: String, prefetchSize: Int = 0, block: suspend ConfirmConsumer.() -> Unit) {
    val consumer = this.consumer(queue, prefetchSize)
    try {
        block(consumer)
    } finally {
        consumer.cancel()
    }
}

open class ConfirmChannel internal constructor(private val channel: Channel) : Channel by channel {
    init {
        channel.confirmSelect()
    }

    fun publisher() = ConfirmPublisher(this)
}
