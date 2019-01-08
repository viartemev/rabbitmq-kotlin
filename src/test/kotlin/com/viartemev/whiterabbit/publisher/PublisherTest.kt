package com.viartemev.whiterabbit.publisher

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.whiterabbit.queue.Queue
import com.viartemev.whiterabbit.queue.QueueSpecification
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PublisherTest {

    @Test
    fun `test a`() {
        val queue = "test_queue"
        val factory = ConnectionFactory()
        factory.useNio()
        factory.host = "localhost"
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                channel.confirmSelect()
                val sender = Publisher(channel)

                runBlocking {
                    Queue.declareQueue(channel, QueueSpecification(queue))
                    val message = OutboundMessage("", queue, MessageProperties.PERSISTENT_BASIC, "Hello 1".toByteArray(charset("UTF-8")))
                    sender.publish(message)
                    sender.publish(listOf(message, message, message))
                    println("Done")
                }
            }
        }
    }
}