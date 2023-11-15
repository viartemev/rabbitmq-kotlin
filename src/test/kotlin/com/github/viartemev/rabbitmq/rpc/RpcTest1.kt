package com.github.viartemev.rabbitmq.rpc

import com.github.viartemev.rabbitmq.AbstractTestContainersTest
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.*

class RpcTest1 : AbstractTestContainersTest() {

    suspend fun sendRpcRequest(requestQueueName: String, message: String, timeoutMillis: Long): String? {
        val responseChannel = Channel<String>()

        val connection = factory.newConnection()
        val channel = connection.createChannel()

        try {
            channel.queueDeclare(requestQueueName, true, false, false, null)
            val replyQueueName = channel.queueDeclare().queue
            val correlationId = UUID.randomUUID().toString()

            val props = AMQP.BasicProperties.Builder().correlationId(correlationId).replyTo(replyQueueName).build()

            channel.basicPublish("", requestQueueName, props, message.toByteArray(StandardCharsets.UTF_8))

            val consumer = object : DefaultConsumer(channel) {
                override fun handleDelivery(
                    consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: ByteArray
                ) {
                    if (properties.correlationId == correlationId) {
                        val response = String(body, StandardCharsets.UTF_8)
                        responseChannel.trySend(response)
                    }
                }
            }

            channel.basicConsume(replyQueueName, true, consumer)

            // Wait for the response with a timeout
            return withTimeoutOrNull(timeoutMillis) {
                responseChannel.receive()
            }
        } finally {
            responseChannel.close()
            channel.close()
            connection.close()
        }
    }

    @Test
    fun `test-test`(): Unit = runBlocking {
        val requestQueueName = "rpc_queue"
        val message = "Hello RPC Server!"

        val response = sendRpcRequest(requestQueueName, message, 50000)
        if (response != null) {
            println("Received RPC response: $response")
        } else {
            println("No RPC response received in the given timeout.")
        }
    }
}
