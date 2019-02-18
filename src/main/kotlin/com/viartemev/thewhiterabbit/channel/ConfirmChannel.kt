package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.viartemev.thewhiterabbit.publisher.ConfirmPublisher
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

open class ConfirmChannel internal constructor(private val channel: Channel) : Channel by channel {
    init {
        channel.confirmSelect()
    }

    fun publisher() = ConfirmPublisher(this)
}

private val localChannels = ConcurrentHashMap<Thread, UncloseableConfirmChannel>()
private val c = ConfirmCleaner

/**
 * Create a channel with enabled publisher acknowledgements on it.
 * @see com.rabbitmq.client.Channel.confirmSelect()
 */
fun Connection.createConfirmChannel(): ConfirmChannel = ConfirmChannel(this.createChannel())

suspend fun Connection.confirmChannel(block: suspend ConfirmChannel.() -> Unit): ConfirmChannel =
    localChannels
        .computeIfAbsent(Thread.currentThread()) { UncloseableConfirmChannel(this.createConfirmChannel()) }
        .also { block(it) }

suspend fun ConfirmChannel.publish(block: suspend ConfirmPublisher.() -> Unit) {
    val publisher = this.publisher()
    block(publisher)
}

private class UncloseableConfirmChannel(private val channel: ConfirmChannel) : ConfirmChannel(channel) {
    override fun close() {}

    override fun close(closeCode: Int, closeMessage: String?) {}

    internal fun close0() {
        if (channel.isOpen) channel.close()
    }
}

private object ConfirmCleaner {
    init {
        Runtime.getRuntime().addShutdownHook(thread {
            localChannels.values.forEach { it.close0() }
            localChannels.clear()
        })
    }
}
