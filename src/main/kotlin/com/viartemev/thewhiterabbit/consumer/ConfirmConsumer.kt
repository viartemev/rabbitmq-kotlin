package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import com.viartemev.thewhiterabbit.exception.AcknowledgeException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.IOException
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

class ConfirmConsumer internal constructor(private val amqpChannel: Channel, amqpQueue: String, prefetchSize: Int = 0) {
    private val deliveries = KChannel<Delivery>()
    private val consTag: String

    init {
        amqpChannel.basicQos(prefetchSize, false)
        consTag = amqpChannel.basicConsume(amqpQueue, false,
            { consumerTag, message ->
                try {
                    deliveries.sendBlocking(message)
                } catch (e: Exception) {
                    logger.info { "Consumer $consumerTag has been cancelled" }
                }
            },
            { consumerTag ->
                logger.info { "Consumer $consumerTag has been cancelled" }
                deliveries.cancel()
            }
        )
    }

    /**
     * Asynchronously consume one message.
     * @throws IOException if an error is encountered
     */
    suspend fun consumeWithConfirm(handler: suspend (Delivery) -> Unit, handlerDispatcher: CoroutineDispatcher = Dispatchers.Default) {
        val delivery = deliveries.receive()
        val deliveryTag = delivery.envelope.deliveryTag
        withContext(handlerDispatcher) { handler(delivery) }
        try {
            amqpChannel.basicAck(deliveryTag, false)
        } catch (e: IOException) {
            val errorMessage = "Can't ack a message with deliveryTag: $deliveryTag"
            logger.error { errorMessage }
            throw AcknowledgeException(errorMessage)
        }
    }

    fun cancel() {
        amqpChannel.basicCancel(consTag)
        deliveries.cancel()
    }
}
