package com.viartemev.thewhiterabbit.example

import com.rabbitmq.client.ConnectionFactory
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.exchange.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors


fun main() {
    val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    singleThreadDispatcher.use { dispatcher ->
        val connectionFactory = ConnectionFactory().apply { useNio() }
        connectionFactory.newConnection().use { connection ->
            runBlocking {
                connection.channel {
                    declareExchange(ExchangeSpecification("exchange-1"), dispatcher)
                    declareExchange(ExchangeSpecification("exchange-2"), dispatcher)

                    bindExchange(BindExchangeSpecification("exchange-1", "exchange-2", "key"))
                    unbindExchange(UnbindExchangeSpecification("exchange-1", "exchange-2", "key"))

                    deleteExchange(DeleteExchangeSpecification("exchange-1"), dispatcher)
                    deleteExchange(DeleteExchangeSpecification("exchange-2"), dispatcher)
                }
            }
        }
    }
}
