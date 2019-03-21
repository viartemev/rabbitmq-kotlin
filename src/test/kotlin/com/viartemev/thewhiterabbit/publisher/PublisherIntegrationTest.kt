package com.viartemev.thewhiterabbit.publisher

import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.utils.createMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PublisherIntegrationTest : AbstractTestContainersTest() {

    private val QUEUE_NAME = "test_queue"

    @Test
    fun `test one message publishing`() {
        factory.newConnection().use {
            val connection = it
            runBlocking {
                connection.confirmChannel {
                    declareQueue(QueueSpecification(QUEUE_NAME))
                    publish {
                        val message = createMessage(queue = QUEUE_NAME, body = "Hello")
                        val ack = publishWithConfirm(message)
                        assertTrue { ack }
                    }
                }
            }
        }
        val info = httpRabbitMQClient.getQueue(DEFAULT_VHOST, QUEUE_NAME)
        assertEquals(QUEUE_NAME, info.name)
        assertEquals(1, info.messagesReady)
    }

    @Test
    fun `test n-messages publishing manually`() {
        val times = 10
        factory.newConnection().use { connection ->
            runBlocking {
                connection.confirmChannel {
                    declareQueue(QueueSpecification(QUEUE_NAME))
                    publish {
                        val acks = coroutineScope {
                            (1..times).map {
                                async {
                                    publishWithConfirm(createMessage(queue = QUEUE_NAME, body = "Hello #$it"))
                                }
                            }.awaitAll()
                        }
                        assertTrue { acks.all { it } }
                    }
                }
            }
        }
    }

    @Test
    fun `test n-messages publishing`() {
        val times = 10
        factory.newConnection().use { connection ->
            runBlocking {
                connection.confirmChannel {
                    declareQueue(QueueSpecification(QUEUE_NAME))
                    publish {
                        val messages = (1..times).map { createMessage(queue = QUEUE_NAME, body = "Hello #$it") }
                        val acks = publishWithConfirmAsync(messages = messages).awaitAll()
                        assertTrue { acks.all { it } }
                    }
                }
            }
        }
    }
}
