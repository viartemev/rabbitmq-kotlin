package com.viartemev.whiterabbit.samples.exchange

import com.rabbitmq.client.ConnectionFactory
import com.viartemev.whiterabbit.exchange.Exchange
import com.viartemev.whiterabbit.exchange.ExchangeSpecification
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            runBlocking {
                Exchange().declareExchange(channel, ExchangeSpecification("custom_exchange"))
            }
        }
    }
    println("Done")
}