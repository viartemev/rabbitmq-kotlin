package com.viartemev.thewhiterabbit.rpc

import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.common.RabbitMqMessage
import com.viartemev.thewhiterabbit.common.cancelOnIOException
import com.viartemev.thewhiterabbit.queue.declareQueue
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume


private val logger = KotlinLogging.logger {}

class RpcClient(val channel: Channel) {

    suspend fun call(exchangeName: String = "", requestQueueName: String, message: RabbitMqMessage): RabbitMqMessage {
        val replyQueueName = channel.declareQueue(RpcQueueSpecification).queue

        val corrId = UUID.randomUUID().toString()

        val props = message
            .properties
            .builder()
            .correlationId(corrId)
            .replyTo(replyQueueName)
            .build()

        //TODO channel.basicQos and channel.basicPublish can throw an exception
        channel.basicQos(1)
        channel.basicPublish(exchangeName, requestQueueName, props, message.body)

        var consumerTag: String? = null
        try {
            return suspendCancellableCoroutine { continuation ->
                cancelOnIOException(continuation) {
                    consumerTag = channel.basicConsume(replyQueueName, true, { consumerTag, delivery ->
                        if (corrId == delivery.properties.correlationId) {
                            continuation.resume(RabbitMqMessage(delivery.properties, delivery.body))
                        }
                    }, { consumerTag ->
                        logger.debug { "Consumer $consumerTag has been cancelled for reasons other than by a call to Channel#basicCancel" }
                    })
                }
            }
        } finally {
            try {
                consumerTag?.let { channel.basicCancel(it) }
            } catch (e: IOException) {
                logger.warn { "Can't cancel consumer with consumerTag: $consumerTag" }
            }
        }
    }
}
