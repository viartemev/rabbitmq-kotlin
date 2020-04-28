package com.viartemev.thewhiterabbit.rpc

import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.common.RabbitMqMessage
import com.viartemev.thewhiterabbit.common.cancelOnIOException
import com.viartemev.thewhiterabbit.queue.DeleteQueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.queue.deleteQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

        withContext(Dispatchers.IO) { channel.basicPublish(exchangeName, requestQueueName, props, message.body) }

        var consumerTag: String? = null
        try {
            return suspendCancellableCoroutine { continuation ->
                cancelOnIOException(continuation) {
                    consumerTag = channel.basicConsume(replyQueueName, true, { _, delivery ->
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
                withContext(Dispatchers.IO) {
                    consumerTag?.let { channel.basicCancel(it) }
                    channel.deleteQueue(DeleteQueueSpecification(replyQueueName))
                }
            } catch (e: IOException) {
                logger.warn(e) { "Can't finalyze RPC for consumer tag: $consumerTag" }
            }
        }
    }
}
