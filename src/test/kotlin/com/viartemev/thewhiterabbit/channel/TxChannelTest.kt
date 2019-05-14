package com.viartemev.thewhiterabbit.channel

import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.utils.createMessage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.lang.Thread.sleep
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class TxChannelTest : AbstractTestContainersTest() {

    private lateinit var oneTimeQueue: String

    @BeforeEach
    fun setUpEach() {
        oneTimeQueue = randomQueue()
    }

    private fun randomQueue() = "test-queue-" + RandomStringUtils.randomNumeric(5)

    @Test
    fun `parallel transactions are not supported`() {
        assertThrows(TransactionException::class.java) {
            factory.newConnection().use { conn ->
                runBlocking {
                    conn.txChannel {
                        declareQueue(QueueSpecification(oneTimeQueue))

                        launch(CoroutineName("tx1")) {
                            transaction {
                                delay(1000)
                            }
                        }

                        launch(CoroutineName("tx2")) {
                            transaction {
                                // should throw TransactionException, because parallel transactions on the channel are prohibited
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `test message publishing with tx implicit commit`() {
        factory.newConnection().use { conn ->
            runBlocking {
                conn.txChannel {
                    declareQueue(QueueSpecification(oneTimeQueue))
                    transaction {
                        val message = createMessage(queue = oneTimeQueue, body = "Hello from tx")
                        publish(message)
                    }
                }
            }
        }

        sleep(7000)
        val info = httpRabbitMQClient.getQueue(DEFAULT_VHOST, oneTimeQueue)
        assertEquals(1, info.messagesReady)
    }

    @Test
    fun `test message publishing with tx implicit rollback`() {
        assertThrows(RuntimeException::class.java) {
            factory.newConnection().use { conn ->
                runBlocking {
                    conn.txChannel {
                        declareQueue(QueueSpecification(oneTimeQueue))
                        transaction {
                            val message = createMessage(queue = oneTimeQueue, body = "Hello from tx")
                            publish(message)
                            throw RuntimeException("sth bad happened")
                        }
                    }
                }
            }
        }

        sleep(7000)
        val info = httpRabbitMQClient.getQueue(DEFAULT_VHOST, oneTimeQueue)
        assertEquals(0, info.messagesReady)
    }

    @Test
    fun `test 2 successful tx series`() {
        factory.newConnection().use { conn ->
            runBlocking {
                conn.txChannel {
                    declareQueue(QueueSpecification(oneTimeQueue))
                    transaction {
                        val message = createMessage(queue = oneTimeQueue, body = "Hello from tx 1")
                        publish(message)
                    }
                    transaction {
                        val message = createMessage(queue = oneTimeQueue, body = "Hello from tx 2")
                        publish(message)
                    }
                }
            }
        }

        sleep(7000)
        val info = httpRabbitMQClient.getQueue(DEFAULT_VHOST, oneTimeQueue)
        assertEquals(2, info.messagesReady)
    }

    @Test
    fun `test consume message within successful tx`() {

        factory.newConnection().use { conn ->

            val count = 10
            val latch = CountDownLatch(count)

            runBlocking(CoroutineName("root")) {
                conn.txChannel {
                    declareQueue(QueueSpecification(oneTimeQueue))

                    transaction {
                        (1..count).map {
                            publish(createMessage(queue = oneTimeQueue, body = "message #$it"))
                        }
                    }

                    transaction {
                        consume(oneTimeQueue) {
                            for (i in 1..count)
                                consumeMessageWithConfirm {
                                    logger.info { "processing msg:" + String(it.body) }
                                    latch.countDown()
                                }
                        }
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))
            sleep(7000)
            assertEquals(0, httpRabbitMQClient.getQueue(DEFAULT_VHOST, oneTimeQueue).messagesReady)
        }
    }

    @Test
    fun `test consume several messages with implicit rollback`() {


        factory.newConnection().use { conn ->

            val count = 100
            val latch = CountDownLatch(count)

            assertThrows(RuntimeException::class.java) {
                runBlocking {
                    conn.txChannel {
                        declareQueue(QueueSpecification(oneTimeQueue))

                        transaction {
                            (1..count).map {
                                publish(createMessage(queue = oneTimeQueue, body = "message #$it"))
                            }
                        }

                        delay(7000)
                        assertEquals(
                            count,
                            httpRabbitMQClient.getQueue(DEFAULT_VHOST, oneTimeQueue).messagesReady.toInt()
                        )

                        transaction {
                            consume(oneTimeQueue) {
                                for (i in 1..count) {
                                    delay(50)
                                    if (ThreadLocalRandom.current().nextBoolean())
                                        throw RuntimeException("sth wrong while processing msg #$i")
                                    consumeMessageWithConfirm {
                                        logger.info { "processing msg:" + String(it.body) }
                                        latch.countDown()
                                    }
                                }
                            }
                            throw RuntimeException("sth bad happened")
                        }
                    }
                }
            }

            sleep(7000)
            assertEquals(count, httpRabbitMQClient.getQueue(DEFAULT_VHOST, oneTimeQueue).messagesReady.toInt())
        }
    }
}
