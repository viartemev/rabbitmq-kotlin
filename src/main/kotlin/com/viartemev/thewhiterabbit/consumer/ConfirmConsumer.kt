package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import com.viartemev.thewhiterabbit.exception.AcknowledgeException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.IOException
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

/**
 * @todo What will be if consumer gets huge amount of messages, but handling is very slow?
 */
class ConfirmConsumer internal constructor(private val AMQPChannel: Channel, AMQPQueue: String, prefetchSize: Int = 0) {
    private val continuations = KChannel<Delivery>()
    private lateinit var consTag: String

    init {
        AMQPChannel.basicQos(prefetchSize, false)
        consTag = AMQPChannel.basicConsume(AMQPQueue, false,
            { consumerTag, message -> if (consumerTag == consTag) continuations.sendBlocking(message) },
            { consumerTag ->
                if (consumerTag == consTag) {
                    logger.info { "Consumer $consumerTag has been cancelled" }
                    continuations.cancel()
                }
            }
        )
    }

    /**
     * Asynchronously consume one message.
     * @throws IOException if an error is encountered
     */
    suspend fun consumeWithConfirm(handler: suspend (Delivery) -> Unit, handlerDispatcher: CoroutineDispatcher = Dispatchers.Default) {
        val delivery = continuations.receive()
        val deliveryTag = delivery.envelope.deliveryTag
        withContext(handlerDispatcher) { handler(delivery) }
        try {
            AMQPChannel.basicAck(deliveryTag, false)
        } catch (e: IOException) {
            val errorMessage = "Can't ack a message with deliveryTag: $deliveryTag"
            logger.error { errorMessage }
            throw AcknowledgeException(errorMessage)
        }
    }

    /**
     * Infinitely asynchronously consume messages.
     * @param parallelism - number of parallel coroutines
     * @param handler - handler
     * @todo Refactor it!
     * @todo Do we need coroutineScope here?
     */
    suspend fun consumeWithConfirm(parallelism: Int = 3, handler: suspend (Delivery) -> Unit, handlerDispatcher: CoroutineDispatcher = Dispatchers.Default) = coroutineScope {
        val internalJobChannel = KChannel<Unit>(parallelism)

        suspend fun consumeWithConfirmNew(handler: suspend (Delivery) -> Unit, handlerDispatcher: CoroutineDispatcher = Dispatchers.Default) = coroutineScope {
            val delivery = continuations.receive()
            logger.debug { "Coroutine got the delivery" }
            internalJobChannel.receive()
            logger.debug { "Free space for new coroutine" }
            withContext(handlerDispatcher) { handler(delivery) }
            AMQPChannel.basicAck(delivery.envelope.deliveryTag, false)
        }

        while (true) {
            logger.debug { "Coroutine waiting for consuming" }
            internalJobChannel.send(Unit)
            logger.debug { "Coroutine started consuming" }
            launch { consumeWithConfirmNew(handler, handlerDispatcher) }
        }
    }
}
