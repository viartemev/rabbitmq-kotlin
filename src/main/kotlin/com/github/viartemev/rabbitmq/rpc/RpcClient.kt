package com.github.viartemev.rabbitmq.rpc

import com.github.viartemev.rabbitmq.publisher.OutboundMessage
import com.github.viartemev.rabbitmq.queue.DeleteQueueSpecification
import com.github.viartemev.rabbitmq.queue.declareQueue
import com.github.viartemev.rabbitmq.queue.deleteQueue
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


private val logger = KotlinLogging.logger {}

/**
 * Represents an RPC client that can make method calls and receive responses.
 *
 * @param channel The channel to communicate with the RPC server.
 */
class RpcClient(val channel: Channel) {

    /**
     * Makes a method call with the provided `OutboundMessage` and waits for a response.
     *
     * @param outboundMessage The outbound message to send.
     * @return The delivery response received from the method call.
     */
    suspend fun call(outboundMessage: OutboundMessage): Delivery {
        val replyQueueName = channel.declareQueue(RpcQueueSpecification).queue
        val corrId = UUID.randomUUID().toString()
        val properties = outboundMessage.properties.builder().correlationId(corrId).replyTo(replyQueueName).build()

        withContext(Dispatchers.IO) {
            channel.basicPublish(
                outboundMessage.exchange, outboundMessage.routingKey, properties, outboundMessage.msg
            )
        }

        var consumerTag: String? = null
        try {
            return suspendCancellableCoroutine { continuation ->
                try {
                    consumerTag = channel.basicConsume(replyQueueName, false, { _, delivery ->
                        if (corrId == delivery.properties.correlationId) {
                            continuation.resume(delivery)
                            channel.basicAck(delivery.envelope.deliveryTag, false)
                        }
                    }, { consumerTag ->
                        logger.debug { "Consumer $consumerTag has been cancelled for reasons other than by a call to Channel#basicCancel" }
                    })
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        } finally {
            try {
                withContext(NonCancellable) {
                    consumerTag?.let { channel.basicCancel(it) }
                    channel.deleteQueue(DeleteQueueSpecification(replyQueueName))
                }
            } catch (e: IOException) {
                logger.error(e) { "Can't cancel consumer and delete the queue for RPC for consumer tag: $consumerTag" }
            }
        }
    }
}
