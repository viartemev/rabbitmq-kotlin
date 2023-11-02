package com.viartemev.thewhiterabbit.publisher

import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.utils.createMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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

    @Test
    fun `test n-messages publishing manually`(): Unit = runBlocking {
        factory.newConnection().use { connection ->
            connection.confirmChannel {
                val queue = declareQueue(QueueSpecification("")).queue
                val publisher = this.publisher()
                val res1 = async(Dispatchers.IO) {
                    publisher.publishWithConfirm(createMessage(queue = queue, body = "Hello #1"))
                }
                val res2 = async(Dispatchers.IO) {
                    publisher.publishWithConfirm(createMessage(queue = queue, body = "Hello #2"))
                }
            }
        }
    }
}
