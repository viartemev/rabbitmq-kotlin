package com.viartemev.thewhiterabbit.samples

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.createConfirmChannel
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import com.viartemev.thewhiterabbit.queue.Queue
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.system.measureNanoTime

fun main(args: Array<String>) {
    val queue = "test_queue"
    val times = 100

    val factory = ConnectionFactory()
    factory.useNio()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createConfirmChannel().use { channel ->
            runBlocking {
                Queue.declareQueue(channel, QueueSpecification(queue))
                //TODO difference between run in launch and without as an example for JPOINT
                val time = measureNanoTime {
                    (1..times)
                            .map {
                                async {
                                    channel.publisher().publish(
                                            OutboundMessage("", queue, MessageProperties.PERSISTENT_BASIC, "Hello #$it".toByteArray(charset("UTF-8"))))
                                }
                            }.awaitAll()
                }
                println("$time nanos")
            }
        }
    }
    println("Done")
}