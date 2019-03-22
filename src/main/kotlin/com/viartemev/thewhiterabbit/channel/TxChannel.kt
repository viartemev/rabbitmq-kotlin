package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.consumer.ConfirmConsumer
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

@DslMarker
annotation class TxChannelDslMarker

@TxChannelDslMarker
open class TxChannel internal constructor(private val channel: Channel) : Channel by channel {

    private val tx: Transaction // the only current tx associated with channel

    init {
        channel.txSelect()
        tx = Transaction()
    }

    /**
     * TODO in RabbitMQ transactions could span only one queue, is it worth implementing this constraint at API level?
     */
    @TxChannelDslMarker
    inner class Transaction {

        var active: AtomicBoolean = AtomicBoolean(false)

        // todo behaviour inside transaction {} is undefined after commit
        fun commit() {
            logger.debug { "committing tx" }
            this@TxChannel.channel.txCommit()
            active.set(false)

        }

        // todo behaviour inside transaction {} is undefined after rollback
        fun rollback() {
            logger.debug { "rolling back tx" }
            this@TxChannel.channel.txRollback()
            // since we've decided to keep channels open, messages are still in unack state after rollback
            this@TxChannel.channel.basicRecover()
            active.set(false)
        }

        suspend fun publish(message: OutboundMessage) {
            withContext(Dispatchers.IO) {
                message.apply {
                    this@TxChannel.channel.basicPublish(exchange, routingKey, properties, msg.toByteArray())
                }
            }
        }

        fun consumer(queue: String, prefetchSize: Int) = ConfirmConsumer(this@TxChannel.channel, queue, prefetchSize)
    }

    suspend fun transaction(block: suspend Tx.() -> Unit) {

        if (tx.active.compareAndExchange(false, true))
            throw IllegalStateException("active tx is detected")

        try {
            block(tx)
            if (tx.active.get()) {
                logger.debug { "committing tx on graceful completion" }
                tx.commit()
            }
        } catch (e: RuntimeException) {
            if (tx.active.get()) {
                logger.info("rolling back tx on exception", e)
                tx.rollback();
            }
        }
    }
}

typealias Tx = TxChannel.Transaction

suspend fun Tx.consume(queue: String, prefetchSize: Int = 0, block: suspend ConfirmConsumer.() -> Unit) =
    this.consumer(queue, prefetchSize).use { block(it) }
