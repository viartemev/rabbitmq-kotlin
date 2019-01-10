package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.createConfirmChannel
import com.viartemev.thewhiterabbit.queue.Queue
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.system.measureNanoTime
import kotlin.test.assertTrue

//FIXME add testcontainers
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
        factory.newConnection().use { connection ->
            connection.createConfirmChannel().use { channel ->
                runBlocking {
                    Queue.declareQueue(channel, QueueSpecification(QUEUE_NAME))
                    val sender = ConfirmPublisher(channel)
                    val message = createMessage("Hello")
                    val ack = sender.publish(message)
                    assertTrue { ack }
                }
            }
        }
    }

    @Test
    fun `test n-messages publishing`() {
        val times = 10
        val time = measureNanoTime {
            factory.newConnection().use { connection ->
                connection.createConfirmChannel().use { channel ->
                    runBlocking {
                        Queue.declareQueue(channel, QueueSpecification(QUEUE_NAME))
                        val sender = ConfirmPublisher(channel)
                        val acks = (1..times).map {
                            async {
                                sender.publish(createMessage("Hello #$it"))
                            }
                        }.awaitAll()
                        assertTrue { acks.all { true } }
                    }
                }
            }
        }
        println("Time: $time")
    }

    private fun createMessage(body: String) = OutboundMessage(EXCHANGE_NAME, QUEUE_NAME, MessageProperties.PERSISTENT_BASIC, body.toByteArray(charset("UTF-8")))
}