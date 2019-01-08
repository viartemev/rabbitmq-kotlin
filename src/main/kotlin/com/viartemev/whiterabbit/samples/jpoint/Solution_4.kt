package com.viartemev.whiterabbit.samples.jpoint

import awaitString
import com.github.kittinunf.fuel.Fuel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.whiterabbit.publisher.OutboundMessage
import com.viartemev.whiterabbit.publisher.Publisher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.LongAdder
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.useNio()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            val queue = "test"
            val sender = Publisher(channel)
            channel.confirmSelect()
            channel.queueDeclare(queue, false, false, false, null)
            val counter = LongAdder()
            val measureTimeMillis = measureTimeMillis {
                runBlocking {
                    repeat(999) {
                        launch {
                            val message = Fuel.get("http://localhost:8081/message").awaitString() + " $it"
                            val outboundMessage = OutboundMessage("", queue, MessageProperties.PERSISTENT_BASIC, message.toByteArray(charset("UTF-8")))
                            val ack = sender.publishWithConfirm(outboundMessage)
                            counter.increment()
                            println(" [x] Sent '$message' ack: $ack")
                        }
                    }
                }
            }
            println(measureTimeMillis)
            println(counter.sumThenReset())
        }
    }
    println("Done")
}