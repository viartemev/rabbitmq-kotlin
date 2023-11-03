package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import com.viartemev.thewhiterabbit.exception.AcknowledgeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.io.Closeable
import java.io.IOException
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

/**
 *
 */
class ConfirmConsumer internal constructor(
    private val amqpChannel: Channel, amqpQueue: String, private val prefetchSize: Int
) : Closeable {
    private val deliveries = KChannel<Delivery>(prefetchSize)

    private val consTag: String

    init {
        amqpChannel.basicQos(prefetchSize, false)
        consTag = amqpChannel.basicConsume(amqpQueue, false, { consumerTag, message ->
            try {
                logger.debug { "Trying to send a message from the consumer to the channel" }
                deliveries.trySendBlocking(message)
                logger.debug { "The message was successfully sent to the channel" }
            } catch (e: Exception) {
                logger.error(e) { "Can't send a message. Consumer $consumerTag has been cancelled" }
            }
        }, { consumerTag ->
            logger.info { "Consumer $consumerTag has been cancelled for reasons other than by a call to Channel#basicCancel" }
            //FIXME do we need to cancel the channel?
            deliveries.cancel()
        })
    }

    suspend fun consumeMessageWithConfirm(handler: suspend (Delivery) -> Unit) {
        try {
            logger.debug { "Trying to receive a message from the channel" }
            val delivery = deliveries.receive()
            logger.debug { "The message was received from the channel" }
            val deliveryTag = delivery.envelope.deliveryTag
            handler(delivery)
            try {
                //TODO with context?
                amqpChannel.basicAck(deliveryTag, false)
                //TODO fix exception handling
            } catch (e: IOException) {
                val errorMessage = "Can't ack a message with deliveryTag: $deliveryTag"
                logger.error { errorMessage }
                throw AcknowledgeException(errorMessage)
            }
            //TODO exception handling
        } catch (e: Exception) {
            when (e) {
                is ClosedReceiveChannelException -> throw CancellationException()
                else -> throw e
            }
        }
    }

    //TODO context + dispatcher
    //TODO test cancellation & exception handling
    suspend fun consumeMessagesWithConfirm(handler: suspend (Delivery) -> Unit) = coroutineScope {
        val channel = KChannel<Unit>(prefetchSize)
        while (isActive) {
            channel.send(Unit)
            launch {
                try {
                    consumeMessageWithConfirm(handler)
                } finally {
                    channel.receive()
                }
            }
        }
    }

    override fun close() {
        logger.debug { "closing ConfirmConsumer" }
        amqpChannel.basicCancel(consTag)
        deliveries.cancel()
    }
}
