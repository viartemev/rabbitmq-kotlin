package com.viartemev.whiterabbit.samples

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.whiterabbit.publisher.OutboundMessage
import com.viartemev.whiterabbit.publisher.Publisher
import com.viartemev.whiterabbit.queue.Queue
import com.viartemev.whiterabbit.queue.QueueSpecification
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.LongAdder

fun main(args: Array<String>) {
    val queue = "test_queue"
    val times = 999L

    val factory = ConnectionFactory()
    factory.useNio()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            channel.confirmSelect()
            val counter = LongAdder()
            val sender = Publisher(channel)

            runBlocking {
                Queue.declareQueue(channel, QueueSpecification(queue))
                repeat(times.toInt()) {
                    launch {
                        val message = OutboundMessage("", queue, MessageProperties.PERSISTENT_BASIC, "Hello #$it".toByteArray(charset("UTF-8")))
                        val ack = sender.publishWithConfirm(message)
                        if (ack) {
                            counter.increment()
                        }
                        println(" [x] Sent '${String(message.body)}' ack: $ack")
                    }
                }
            }
            assert(times == counter.sumThenReset())
        }
    }
    println("Done")
}