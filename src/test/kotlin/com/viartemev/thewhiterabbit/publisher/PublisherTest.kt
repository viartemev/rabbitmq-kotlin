package com.viartemev.thewhiterabbit.publisher

import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.consume
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.channel.txChannel
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.lang.RuntimeException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PublisherTest : AbstractTestContainersTest() {

    private val QUEUE_NAME = "test_queue"
    private val EXCHANGE_NAME = ""

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
                        val acks = publishWithConfirmAsync(messages = messages).awaitAll()
                        assertTrue { acks.all { it } }
                    }
                }
            }
        }
    }

    @Test
    fun `test one message publishing with tx implicit commit`() {
        val queue = randomQueue()
        factory.apply {
            host = "172.17.0.2"
            port = 5672
        }
        factory.newConnection().use { conn ->

            val latch = CountDownLatch(1)

            runBlocking {
                conn.txChannel {

                    declareQueue(QueueSpecification(queue))

// won't compile, good
//                    transaction {
//                        declareQueue(QueueSpecification(queue))
//                    }
// won't compile, good
//                    transaction {
//                        transaction {
//                        }
//                    }

                    transaction {
                        val message = createMessage("Hello from tx", "", queue)
                        publish(message)
                        commit()

                        consume(queue, 1) {
                            consumeMessageWithConfirm({ latch.countDown() })
                        }
                    }
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))
        }
    }


    @Test
    fun `test one message publishing with tx implicit rollback`() {
        val queue = randomQueue()
        factory.apply {
            host = "172.17.0.2"
            port = 5672
        }
        factory.newConnection().use { conn ->
            runBlocking {
                conn.txChannel {
                    declareQueue(QueueSpecification(queue))
                    transaction {
                        val message = createMessage("Hello from tx", "", queue)
                        publish(message)
                        throw RuntimeException("sth happened")
                    }
                }
            }
        }
    }

    @Test
    fun `test a message publishing with tx explicit rollback`() {
        val queue = randomQueue()
        factory.apply {
            host = "172.17.0.2"
            port = 5672
        }
        factory.newConnection().use { conn ->
            runBlocking {
                conn.txChannel {
                    declareQueue(QueueSpecification(queue))
                    transaction {
                        val message = createMessage("Hello from tx", "", queue)
                        publish(message)
                        rollback()
                    }
                }
            }
        }
    }


//    @Disabled("fix consume on tx channel")
//    @Test
//    fun `test one message publishing with tx rollback`() {
//
//        val queue = randomQueue()
//        factory.newConnection().use { conn ->
//            runBlocking {
//                conn.txChannel {
//                    declareQueue(QueueSpecification(queue))
//                    publish {
//                        val message = createMessage("Hello", "", queue)
//                        publishInTx(message)
//                        rollback()
//
//                        // consume ain't work properly on tx channel
//                        val latch = CountDownLatch(1)
//                        consume(queue) {
//                            consumeMessageWithConfirm({ latch.countDown() })
//                        }
//                        assertTrue(latch.await(1, TimeUnit.SECONDS))
//                    }
//                }
//            }
//        }
//    }


    private fun randomQueue() = QUEUE_NAME + "_" + RandomStringUtils.randomNumeric(3)

    private fun createMessage(body: String, exchange: String = EXCHANGE_NAME, queue: String = QUEUE_NAME) =
        OutboundMessage(exchange, queue, MessageProperties.PERSISTENT_BASIC, body)
}
