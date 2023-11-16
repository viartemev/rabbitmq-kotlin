package io.github.viartemev.rabbitmq.consumer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import mu.KotlinLogging
import java.io.Closeable
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class ConfirmConsumer internal constructor(
    private val amqpChannel: Channel, amqpQueue: String, private val prefetchSize: Int
) : Closeable {
    private val deliveries = KChannel<Delivery>(prefetchSize)

    private val consTag: String

    init {
        amqpChannel.basicQos(prefetchSize, false)
        consTag = amqpChannel.basicConsume(amqpQueue, false, ::handleDelivery, ::handleCancel)
    }

    private fun handleDelivery(consumerTag: String, message: Delivery) {
        try {
            logger.debug { "Trying to send a message from the consumer to the channel" }
            deliveries.trySendBlocking(message)
            logger.debug { "The message was successfully sent to the channel" }
        } catch (e: Exception) {
            logger.error(e) { "Can't send a message. Consumer $consumerTag has been cancelled" }
        }
    }

    private fun handleCancel(consumerTag: String) {
        logger.info { "Consumer $consumerTag has been cancelled for reasons other than by a call to Channel#basicCancel" }
        deliveries.close()
    }

    suspend fun consumeMessageWithConfirm(handler: suspend (Delivery) -> Unit) = coroutineScope {
        logger.debug { "Trying to receive a message from the channel" }
        val delivery = deliveries.receive()
        logger.debug { "The message was received from the channel" }
        val deliveryTag = delivery.envelope.deliveryTag
        try {
            handler(delivery)
            amqpChannel.basicAck(deliveryTag, false)
        } catch (e: Exception) {
            val errorMessage = "Can't ack a message with deliveryTag: $deliveryTag"
            logger.error(e) { errorMessage }
            cancel(errorMessage, e)
        }
    }

    suspend fun consumeMessagesWithConfirm(handler: suspend (Delivery) -> Unit) = coroutineScope {
        val semaphore = Semaphore(prefetchSize)
        while (isActive) {
            semaphore.acquire()
            launch {
                try {
                    consumeMessageWithConfirm(handler)
                } finally {
                    semaphore.release()
                }
            }
        }
    }

    override fun close() {
        try {
            logger.debug { "Cancelling consumer#$consTag" }
            amqpChannel.basicCancel(consTag)
        } catch (e: Exception) {
            logger.error(e) { "Can't cancel consumer#$consTag" }
            deliveries.close()
        }
    }
}
