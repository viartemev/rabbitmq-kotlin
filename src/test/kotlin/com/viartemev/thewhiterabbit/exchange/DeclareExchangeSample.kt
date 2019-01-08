package com.viartemev.thewhiterabbit.exchange

import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            runBlocking {
                Exchange.declareExchange(channel, ExchangeSpecification("custom_exchange"))
            }
        }
    }
    println("Done")
}