package com.viartemev.thewhiterabbit.rpc

import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.common.cancelOnIOException
import com.viartemev.thewhiterabbit.queue.DeleteQueueSpecification
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.queue.deleteQueue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume


private val logger = KotlinLogging.logger {}

//TODO channel.basicPublish can throw an exception
class RpcClient(val channel: Channel) {

    suspend fun call(exchangeName: String = "", message: RabbitMqMessage): RabbitMqMessage = coroutineScope {
        val requestQueue = async { channel.declareQueue(QueueSpecification(name = "", exclusive = true, autoDelete = true)).queue }
        val replyQueue = async { channel.declareQueue(QueueSpecification(name = "", exclusive = true, autoDelete = true)).queue }
        val requestQueueName = requestQueue.await()
        val replyQueueName = replyQueue.await()

        val result = call(exchangeName, requestQueueName, replyQueueName, message)

        val requestQueueDeletion = async { channel.deleteQueue(DeleteQueueSpecification(requestQueueName)) }
        val replyQueueDeletion = async { channel.deleteQueue(DeleteQueueSpecification(replyQueueName)) }
        awaitAll(replyQueueDeletion, requestQueueDeletion)

        return@coroutineScope result
    }

    suspend fun callWithTimeout(
        exchangeName: String = "",
        message: RabbitMqMessage,
        timeout: Long
    ): RabbitMqMessage = withTimeout(timeout) {
        call(exchangeName, message)
    }

    suspend fun call(exchangeName: String, requestQueueName: String, replyQueueName: String, message: RabbitMqMessage): RabbitMqMessage {
        val corrId = UUID.randomUUID().toString()

        val props = message
            .properties
            .builder()
            .correlationId(corrId)
            .replyTo(replyQueueName)
            .build()

        channel.basicPublish(exchangeName, replyQueueName, props, message.body)

        return suspendCancellableCoroutine { continuation ->
            cancelOnIOException(continuation) {
                channel.basicConsume(requestQueueName, true, { consumerTag, delivery ->
                    if (corrId == delivery.properties.correlationId) {
                        try {
                            continuation.resume(RabbitMqMessage(delivery.properties, delivery.body))
                        } finally {
                            try {
                                channel.basicCancel(consumerTag)
                            } catch (e: IOException) {
                                logger.warn { "Can't cancel consumer with consumerTag: $consumerTag" }
                            }
                        }
                    }
                }, { consumerTag ->
                    logger.debug { "Consumer $consumerTag has been cancelled for reasons other than by a call to Channel#basicCancel" }
                })
            }
        }
    }

    suspend fun callWithTimeout(exchangeName: String,
                                requestQueueName: String,
                                replyQueueName: String,
                                message: RabbitMqMessage,
                                timeout: Long
    ): RabbitMqMessage = withTimeout(timeout) {
        call(exchangeName, requestQueueName, replyQueueName, message)
    }

}
