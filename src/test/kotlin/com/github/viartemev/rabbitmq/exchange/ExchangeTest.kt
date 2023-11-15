package com.github.viartemev.rabbitmq.exchange

import com.github.viartemev.rabbitmq.AbstractTestContainersTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ExchangeTest : AbstractTestContainersTest() {
    private val exchangeName = "exchange_name"

    @Test
    fun `an exchange declaration test`() {
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    channel.declareExchange(ExchangeSpecification(exchangeName))
                    assertNotNull(httpRabbitMQClient.getExchange(DEFAULT_VHOST, exchangeName))
                }
            }
        }
    }

    @Test
    fun `an exchange deletion test`() {
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    channel.declareExchange(ExchangeSpecification(exchangeName))
                    assertNotNull(httpRabbitMQClient.getExchange(DEFAULT_VHOST, exchangeName))
                    channel.deleteExchange(DeleteExchangeSpecification(exchangeName))
                    assertNull(httpRabbitMQClient.getExchange(DEFAULT_VHOST, exchangeName))
                }
            }
        }
    }

}
