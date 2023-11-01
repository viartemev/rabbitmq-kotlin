package com.viartemev.thewhiterabbit.publisher

import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.utils.createMessage
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PublisherIntegrationTest : AbstractTestContainersTest() {

    @Test
    fun `test one message publishing`() {
        factory.newConnection().use {
            val connection = it
            runBlocking {
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

    @Test
    fun `test n-messages publishing manually`() {
        val times = 10
        factory.newConnection().use { connection ->
            runBlocking {
                connection.confirmChannel {
                    val queue = declareQueue(QueueSpecification("")).queue
                    publish {
                        val acks = coroutineScope {
                            (1..times).map {
                                async {
                                    publishWithConfirm(createMessage(queue = queue, body = "Hello #$it"))
                                }
                            }.awaitAll()
                        }
                        assertTrue { acks.all { it } }
                    }
                }
            }
        }
    }
}
