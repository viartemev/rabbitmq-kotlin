package io.github.viartemev.rabbitmq.rpc

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import io.github.viartemev.rabbitmq.common.OutboundMessage
import io.github.viartemev.rabbitmq.queue.DeleteQueueSpecification
import io.github.viartemev.rabbitmq.queue.declareQueue
import io.github.viartemev.rabbitmq.queue.deleteQueue
import kotlinx.coroutines.*
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

        runInterruptible(Dispatchers.IO) {
            channel.basicPublish(outboundMessage.exchange, outboundMessage.routingKey, properties, outboundMessage.msg)
        }

        var consumerTag: String? = null
        try {
            return suspendCancellableCoroutine { continuation ->
                val deliverCallback: (consumerTag: String, message: Delivery) -> Unit = { _, delivery ->
                    if (corrId == delivery.properties.correlationId) {
                        channel.basicAck(delivery.envelope.deliveryTag, false)
                        continuation.resume(delivery)
                    }
                }
                val cancelCallback: (consumerTag: String) -> Unit = { consumerTag ->
                    logger.debug { "Consumer $consumerTag has been cancelled for reasons other than by a call to Channel#basicCancel" }
                }

                try {
                    consumerTag = channel.basicConsume(replyQueueName, false, deliverCallback, cancelCallback)
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
