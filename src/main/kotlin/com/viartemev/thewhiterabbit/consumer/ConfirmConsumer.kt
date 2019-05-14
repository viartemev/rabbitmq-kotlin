package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import com.viartemev.thewhiterabbit.exception.AcknowledgeException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.sendBlocking
import mu.KotlinLogging
import java.io.Closeable
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class ConfirmConsumer internal constructor(private val amqpChannel: Channel, amqpQueue: String, prefetchSize: Int) : Closeable {
    private val deliveries = KChannel<Delivery>()

    private val consTag: String

    init {
        amqpChannel.basicQos(prefetchSize, false)
        consTag = amqpChannel.basicConsume(amqpQueue, false,
            { consumerTag, message ->
                try {
                    deliveries.sendBlocking(message)
                } catch (e: ClosedSendChannelException) {
                    logger.debug { "Can't receive a message. Consumer $consumerTag has been cancelled" }
                }
            },
            { consumerTag ->
                logger.debug { "Consumer $consumerTag has been cancelled for reasons other than by a call to Channel#basicCancel" }
                deliveries.cancel()
            }
        )
    }

    /**
     * Consume a message and handle it.
     * @throws com.viartemev.thewhiterabbit.exception.AcknowledgeException if can't send ack
     */
    suspend fun consumeMessageWithConfirm(handler: suspend (Delivery) -> Unit) {
        try {
            val delivery = deliveries.receive()
            val deliveryTag = delivery.envelope.deliveryTag
            handler(delivery)
            try {
                amqpChannel.basicAck(deliveryTag, false)
            } catch (e: IOException) {
                val errorMessage = "Can't ack a message with deliveryTag: $deliveryTag"
                logger.error { errorMessage }
                throw AcknowledgeException(errorMessage)
            }
        } catch (e: Exception) {
            when (e) {
                is ClosedReceiveChannelException -> throw CancellationException()
                else -> throw e
            }
        }
    }

    /**
     * Consume a message and handle it with timeout.
     * @throws kotlinx.coroutines.TimeoutCancellationException if timeout expired
     */
    suspend fun consumeMessageWithConfirmAndTimeout(handler: suspend (Delivery) -> Unit, timeMillis: Long) {
        withTimeout(timeMillis) { consumeMessageWithConfirm(handler) }
    }

    /**
     * Asynchronously consume and handle numberOfMessages.
     */
    suspend fun consumeMessagesWithConfirm(
        numberOfMessages: Int,
        handler: suspend (Delivery) -> Unit,
        coroutineContext: CoroutineContext = EmptyCoroutineContext
    ) = coroutineScope {
        (1..numberOfMessages).map { async(coroutineContext) { consumeMessageWithConfirm(handler) } }
    }

    /**
     * @todo Infinite consuming and handling messages
     * THIS FUNCTION IS NOT PRODUCTION READY.
     * Infinite consuming and handling messages.
     */
    suspend fun consumeMessagesWithConfirm1(parallelism: Int, handler: suspend (Delivery) -> Unit) = coroutineScope {
        val channel = KChannel<Unit>(parallelism)
        while (true) {
            channel.send(Unit)
            launch {
                consumeMessageWithConfirm(handler)
                channel.receive()
            }
        }
    }

    override fun close() {
        logger.debug { "closing ConfirmConsumer" }
        amqpChannel.basicCancel(consTag)
        deliveries.cancel()
    }
}
