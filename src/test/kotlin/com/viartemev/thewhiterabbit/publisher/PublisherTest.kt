package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@Disabled("FIXME add testcontainers and split local and CI env")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublisherTest {

    private val QUEUE_NAME = "test_queue"
    private val EXCHANGE_NAME = ""
    lateinit var factory: ConnectionFactory


    @BeforeAll
    fun setUp() {
        factory = ConnectionFactory()
        factory.host = "localhost"
        factory.useNio()
    }

    @Test
    fun `test one message publishing`() {
        factory.newConnection().use {
            val connection = it
            runBlocking {
                connection.confirmChannel {
                    declareQueue(QueueSpecification(QUEUE_NAME))
                    publish {
                        val message = createMessage("Hello")
                        val ack = publishWithConfirm(message)
                        assertTrue { ack }
                    }
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
                    declareQueue(QueueSpecification(QUEUE_NAME))
                    publish {
                        val acks = coroutineScope {
                            (1..times).map {
                                async {
                                    publishWithConfirm(createMessage("Hello #$it"))
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
                        val messages = (1..times).map { createMessage("Hello #$it") }
                        val acks = asyncPublishWithConfirm(messages).awaitAll()
                        assertTrue { acks.all { it } }
                    }
                }
            }
        }
    }

    private fun createMessage(body: String) = OutboundMessage(EXCHANGE_NAME, QUEUE_NAME, MessageProperties.PERSISTENT_BASIC, body)
}
