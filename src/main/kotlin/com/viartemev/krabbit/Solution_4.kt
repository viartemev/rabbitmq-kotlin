package com.viartemev.krabbit

import awaitString
import com.github.kittinunf.fuel.Fuel
import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.LongAdder

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.useNio()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            val queue = "test"
            val sender = CoroutineSender(queue, channel)
            channel.confirmSelect()
            channel.queueDeclare(queue, false, false, false, null)
            val counter = LongAdder()
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
            println(counter.sumThenReset())
        }
    }
    println("Done")
}