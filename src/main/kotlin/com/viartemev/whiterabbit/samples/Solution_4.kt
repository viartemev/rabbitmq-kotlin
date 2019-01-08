package com.viartemev.whiterabbit.samples

import awaitString
import com.github.kittinunf.fuel.Fuel
import com.rabbitmq.client.ConnectionFactory
import com.viartemev.whiterabbit.sender.Sender
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
            val sender = Sender(queue, channel)
            channel.confirmSelect()
            channel.queueDeclare(queue, false, false, false, null)
            val counter = LongAdder()
            val measureTimeMillis = measureTimeMillis {
                runBlocking {
                    repeat(999) {
                        launch {
                            val message = Fuel.get("http://localhost:8081/message").awaitString() + " $it"
                            val ack = sender.sendWithAck(message)
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