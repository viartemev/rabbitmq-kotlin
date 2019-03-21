package com.viartemev.thewhiterabbit.rpc

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.common.cancelOnIOException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume


private val logger = KotlinLogging.logger {}

//TODO channel.basicPublish can throw an exception
class RpcClient(val channel: Channel) {

    suspend fun call(message: RpcOutboundMessage): RpcInboundMessage {
        val corrId = UUID.randomUUID().toString()

        val props = AMQP.BasicProperties.Builder()
            .correlationId(corrId)
            .replyTo(message.replyQueueName)
            .build()

        channel.basicPublish(message.exchangeName, message.requestQueueName, props, message.body)

        return suspendCancellableCoroutine { continuation ->
            cancelOnIOException(continuation) {
                channel.basicConsume(message.replyQueueName, true, { consumerTag, delivery ->
                    if (corrId == delivery.properties.correlationId) {
                        try {
                            continuation.resume(RpcInboundMessage(delivery.properties, delivery.body))
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

    suspend fun callWithTimeout(message: RpcOutboundMessage, timeout: Long): RpcInboundMessage = withTimeout(timeout) {
        call(message)
    }
}
