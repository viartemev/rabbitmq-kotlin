package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.Connection
import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.common.RabbitMqDispatchers
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.utils.createMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class PublisherIntegrationTest : AbstractTestContainersTest() {

    @Test
    fun `test one message publishing`() {
        factory.newConnection().use { connection: Connection ->
            runBlocking {
                connection.confirmChannel {
                    val queue = declareQueue(QueueSpecification("")).queue
                    publish {
                        val message = createMessage(queue = queue, body = "Hello")
                        val ack = publishWithConfirm(message)
                        assertTrue { ack }
                    }
                    await withPollInterval Duration.ofMillis(1000) untilAsserted {
                        val info = httpRabbitMQClient.getQueue(DEFAULT_VHOST, queue)
                        assertEquals(queue, info.name)
                        assertEquals(1, info.messagesReady)
                    }
                }
            }
        }
    }

    @Test
    fun `test n-messages publishing manually`() {
        val times = 10L
        factory.newConnection().use { connection ->
            runBlocking {
                connection.confirmChannel {
                    val queue = declareQueue(QueueSpecification("")).queue
                    publish {
                        val acks = coroutineScope {
                            (1..times).map {
                                async(RabbitMqDispatchers.SingleThreadDispatcher) {
                                    publishWithConfirm(createMessage(queue = queue, body = "Hello #$it"))
                                }
                            }.awaitAll()
                        }
                        assertTrue { acks.all { it } }
                    }
                    await withPollInterval Duration.ofMillis(1000) untilAsserted {
                        val info = httpRabbitMQClient.getQueue(DEFAULT_VHOST, queue)
                        assertEquals(queue, info.name)
                        assertEquals(times, info.messagesReady)
                    }
                }
            }
        }
    }
}
