package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import kotlinx.coroutines.channels.Channel as KChannel

private val logger = KotlinLogging.logger {}

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

    suspend fun consumeWithConfirm(handler: suspend (Delivery) -> Unit, handlerDispatcher: CoroutineDispatcher = Dispatchers.Default) = coroutineScope {
        val delivery = continuations.receive()
        withContext(handlerDispatcher) { handler(delivery) }
        AMQPChannel.basicAck(delivery.envelope.deliveryTag, false)
    }

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
