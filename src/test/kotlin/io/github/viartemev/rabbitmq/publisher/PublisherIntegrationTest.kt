package io.github.viartemev.rabbitmq.publisher

import io.github.viartemev.rabbitmq.AbstractTestContainersTest
import io.github.viartemev.rabbitmq.channel.confirmChannel
import io.github.viartemev.rabbitmq.channel.publish
import io.github.viartemev.rabbitmq.queue.QueueSpecification
import io.github.viartemev.rabbitmq.queue.declareQueue
import io.github.viartemev.rabbitmq.utils.createMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PublisherIntegrationTest : AbstractTestContainersTest() {

    @Test
    fun `test one message publishing`(): Unit = runBlocking {
        factory.newConnection().use {
            val connection = it
            connection.confirmChannel {
                val queue = declareQueue(QueueSpecification("")).queue
                publish {
                    val message = createMessage(queue = queue, body = "Hello")
                    val ack = publishWithConfirm(message)
                    assertTrue { ack }
                }
                delay(5000)
                val info = httpRabbitMQClient.getQueue(DEFAULT_VHOST, queue)
                assertEquals(queue, info.name)
                assertEquals(1, info.messagesReady)
            }
        }
    }
}
