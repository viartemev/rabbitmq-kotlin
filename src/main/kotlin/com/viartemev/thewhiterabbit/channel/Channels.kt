package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.viartemev.thewhiterabbit.channel.Channels.localChannels
import com.viartemev.thewhiterabbit.channel.Channels.localConfirmChannels
import com.viartemev.thewhiterabbit.channel.Channels.localTxChannels
import com.viartemev.thewhiterabbit.consumer.ConfirmConsumer
import com.viartemev.thewhiterabbit.publisher.ConfirmPublisher
import com.viartemev.thewhiterabbit.publisher.TxPublisher
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

object Channels {
    internal val localChannels = ConcurrentHashMap<Thread, UncloseableChannel>()
    internal val localConfirmChannels = ConcurrentHashMap<Thread, UncloseableConfirmChannel>()
    internal val localTxChannels = ConcurrentHashMap<Thread, UnclosableTxChannel>()

    init {
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            sequenceOf(
                localChannels.values.asSequence(),
                localConfirmChannels.values.asSequence(),
                localTxChannels.values.asSequence()
            )
                .flatten()
                .forEach { it.close0() }
            localChannels.clear()
            localConfirmChannels.clear()
            localTxChannels.clear()
        })
    }

    private interface IAmUncloseableChannel : Channel {
        fun close0()
    }

    internal class UncloseableChannel(private val channel: Channel) : IAmUncloseableChannel, Channel by channel {
        override fun close() {}
        override fun close(closeCode: Int, closeMessage: String?) {}

        override fun close0() {
            if (channel.isOpen) channel.close()
        }
    }

    internal class UncloseableConfirmChannel(private val channel: ConfirmChannel) : ConfirmChannel(channel), IAmUncloseableChannel {
        override fun close() {}
        override fun close(closeCode: Int, closeMessage: String?) {}

        override fun close0() {
            if (channel.isOpen) channel.close()
        }
    }

    internal class UnclosableTxChannel(private val channel: TxChannel) : TxChannel(channel), IAmUncloseableChannel {
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

/**
 * Create a channel with transaction support.
 * @see com.rabbitmq.client.Channel.txSelect()
 */
fun Connection.createTxChannel(): TxChannel = TxChannel(this.createChannel())

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

suspend fun Connection.txChannel(block: suspend TxChannel.() -> Unit): TxChannel {
    var channel = Channels.localTxChannels[Thread.currentThread()]
    if (channel == null || !channel.isOpen) {
        channel = Channels.UnclosableTxChannel(createTxChannel())
        localTxChannels.put(Thread.currentThread(), channel)
    }
    return channel
        .also {
            block(it)
        }
}

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

open class ConfirmChannel internal constructor(private val channel: Channel) : Channel by channel {
    init {
        channel.confirmSelect()
    }

    fun publisher() = ConfirmPublisher(this)
}

open class TxChannel internal constructor(private val channel: Channel) : Channel by channel {
    init {
        channel.txSelect()
    }

    fun publisher() = TxPublisher(this)
}

fun Channel.consumer(queue: String, prefetchSize: Int) = ConfirmConsumer(this, queue, prefetchSize)

suspend fun ConfirmChannel.publish(block: suspend ConfirmPublisher.() -> Unit) = block(this.publisher())

suspend fun TxChannel.publish(block: suspend TxPublisher.() -> Unit) = block(this.publisher())

suspend fun Channel.consume(queue: String, prefetchSize: Int = 0, block: suspend ConfirmConsumer.() -> Unit) =
    this.consumer(queue, prefetchSize).use { block(it) }
