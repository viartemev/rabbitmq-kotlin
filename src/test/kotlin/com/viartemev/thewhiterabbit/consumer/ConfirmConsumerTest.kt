package com.viartemev.thewhiterabbit.consumer

import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.consume
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.utils.createMessage
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class ConfirmConsumerTest : AbstractTestContainersTest() {

    private val QUEUE_NAME = "test_queue"

    @Test
    fun `test message consuming`(): Unit = runBlocking {
        val counter = AtomicInteger()
        factory.newConnection().use { connection ->
            connection.confirmChannel {
                declareQueue(QueueSpecification(QUEUE_NAME))
                val timeInMillis = measureTimeMillis {
                    publish {
                        (1..10).map { createMessage(queue = QUEUE_NAME, body = "1") }
                            .map { m -> async { publishWithConfirm(m) } }.awaitAll()
                    }
                }
                //TODO run millions of coroutines and try to cancel them...system continue to waste resources
                println("(The operation took $timeInMillis ms)")
                val timeInMillis2 = measureTimeMillis {
                    consume(QUEUE_NAME, 100) {
//                        for (i in 1..5000) {
//                            consumeMessageWithConfirm {
//                                delay(100)
//                                counter.getAndAdd(String(it.body).toInt())
//                            }
//                        }
                        (1..10).map {
                            async(Dispatchers.IO) {
                                consumeMessageWithConfirm {
                                    delay(50000)
                                    counter.getAndAdd(String(it.body).toInt())
                                }
                            }
                        }.awaitAll()
                    }
                    assertEquals(10, counter.get())
                }
                println("(The operation2 took $timeInMillis2 ms)")
            }
        }
    }

    @Test
    fun `test infinite message consuming`(): Unit = runBlocking {
        val counter = AtomicInteger()
        factory.newConnection().use { connection ->
            connection.confirmChannel {
                declareQueue(QueueSpecification(QUEUE_NAME))
                val message = createMessage(queue = QUEUE_NAME, body = "1")
                val sender = launch {
                    publish {
                        while (isActive) {
                            delay(100)
                            publishWithConfirm(message)
                        }
                    }
                }
                consume(QUEUE_NAME, 2) {
                    consumeMessagesWithConfirm {
                        println("Consuming message: ${it.body}")
                        delay(5000)
                        counter.getAndAdd(String(it.body).toInt())
                    }
                }
                delay(50000)
                println("Shouting down...")
                sender.cancel()
            }
        }
    }
}
