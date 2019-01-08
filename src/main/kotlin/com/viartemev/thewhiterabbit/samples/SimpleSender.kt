package com.viartemev.thewhiterabbit.samples

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.createConfirmChannel
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import com.viartemev.thewhiterabbit.queue.Queue
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.LongAdder

fun main(args: Array<String>) {
    val queue = "test_queue"
    val times = 999L

    val factory = ConnectionFactory()
    factory.useNio()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createConfirmChannel().use { channel ->
            val counter = LongAdder()
            var ack: List<Boolean> = emptyList()
            runBlocking {
                Queue.declareQueue(channel, QueueSpecification(queue))
                //TODO difference between run in launch and without as an example for JPOINT
                val messages = (1..times).map { OutboundMessage("", queue, MessageProperties.PERSISTENT_BASIC, "Hello #$it".toByteArray(charset("UTF-8"))) }
                ack = channel.publisher().publish(messages)
                if (ack.all { b -> b }) {
                    counter.increment()
                }
            }
            println(ack)
            assert(times == counter.sumThenReset())
        }
    }
    println("Done")
}