package com.viartemev.thewhiterabbit.consumer.flow

import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.utils.createMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ConsumerFlowTest : AbstractTestContainersTest() {
    private val QUEUE_NAME = "test_queue"

    @Test
    fun testAutoAckFlow(): Unit = runBlocking {
        factory.newConnection().use { connection ->
            connection.confirmChannel {
                declareQueue(QueueSpecification(QUEUE_NAME))
                publish {
                    (1..10).map { createMessage(queue = QUEUE_NAME, body = "1") }
                        .map { m -> async { publishWithConfirm(m) } }.awaitAll()
                }
                ConsumerFlow(this, QUEUE_NAME).consumerAutoAckFlow(2).take(10)
                    .collect { delivery -> println(String(delivery.body)) }
            }
        }
    }

    @Test
    fun testConfirmAckFlow(): Unit = runBlocking {
        factory.newConnection().use { connection ->
            connection.confirmChannel {
                declareQueue(QueueSpecification(QUEUE_NAME))
                publish {
                    (1..10).map { i -> createMessage(queue = QUEUE_NAME, body = i.toString()) }
                        .map { m -> async { publishWithConfirm(m) } }.awaitAll()
                }
                val delivery = ConsumerFlow(this, QUEUE_NAME)
                    .consumerConfirmAckFlow(2)
                    .flowOn(Dispatchers.IO)
                    .catch { e -> println("Caught exception: $e") }
                    .single()
                println("Got the message: ${delivery.body}")
            }
        }
    }
}
