package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.consumer.ConfirmConsumer
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

@DslMarker
annotation class TxChannelDslMarker

@TxChannelDslMarker
open class TxChannel internal constructor(private val channel: Channel) : Channel by channel {

    private val tx: Transaction // the only current tx associated with a channel

    init {
        channel.txSelect()
        tx = Transaction()
    }

    @TxChannelDslMarker
    inner class Transaction {

        internal var active = AtomicBoolean(false)
        internal var markForRollback = AtomicBoolean(false)

        internal fun rollback() {
            logger.debug { "marking for rolling back" }
            markForRollback.set(true)
        }

        fun publish(message: OutboundMessage) {
            message.apply {
                this@TxChannel.channel.basicPublish(exchange, routingKey, properties, body)
            }
        }

        fun consumer(queue: String, prefetchSize: Int) =
            ConfirmConsumer(this@TxChannel.channel, queue, prefetchSize)
    }

    suspend fun transaction(block: suspend Tx.() -> Unit) {

        try {
            if (!tx.active.compareAndSet(
                    false,
                    true
                )
            ) throw TransactionException("another active tx on the channel is detected")
            block(tx)
        } catch (rollbackException: RuntimeException) {
            if (tx.active.get()) {
                logger.info("rolling back tx on runtime exception", rollbackException)
                tx.rollback()
            }
            throw rollbackException
        } finally {
            if (tx.active.compareAndSet(true, false)) {
                if (tx.markForRollback.get()) {
                    logger.debug { "rolling back tx" }
                    this@TxChannel.channel.txRollback()
                    //since we've decided to keep channels open, messages are still in unack state after rollback
                    this@TxChannel.channel.basicRecover()
                } else {
                    logger.debug { "committing tx" }
                    this@TxChannel.channel.txCommit()
                }
                tx.markForRollback.set(false)
            } else {
                logger.debug { "finalize closed tx" }
            }
        }
    }
}

typealias Tx = TxChannel.Transaction

class TransactionException(message: String) : RuntimeException(message)

suspend fun Tx.consume(queue: String, prefetchSize: Int = 0, block: suspend ConfirmConsumer.() -> Unit) =
    this.consumer(queue, prefetchSize).use { block(it) }
