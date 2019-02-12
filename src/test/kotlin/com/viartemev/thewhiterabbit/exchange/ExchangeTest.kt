package com.viartemev.thewhiterabbit.exchange

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import com.rabbitmq.client.ConnectionFactory
import com.viartemev.thewhiterabbit.utils.RabbitMQContainer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Disabled("FIXME")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExchangeTest {
    @Container
    private val rabbitmq = RabbitMQContainer()
    lateinit var factory: ConnectionFactory

    @BeforeAll
    fun setUp() {
        rabbitmq.start()
        factory = ConnectionFactory()
        factory.host = rabbitmq.containerIpAddress.toString()
        factory.port = rabbitmq.connectionPort()
    }

    @Test
    fun `exchange declaration test`() {
        val EXCHANGE_NAME = "name"
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    channel.declareExchange(ExchangeSpecification(EXCHANGE_NAME))
                }
            }
        }
        val (_, _, response) = Fuel.get("http://localhost:8080/api/exchanges").authenticate("guest", "guest").responseObject<List<ExchangesHttpResponse>>()
        val exchanges = response.component1()
        assertNotNull(exchanges)
        if (exchanges == null) {
            throw RuntimeException("There are no exchanges")
        }
        assertTrue { exchanges.isNotEmpty() }
        assertTrue { exchanges.find { it.name == EXCHANGE_NAME } != null }
    }
}


data class ExchangesHttpResponse(val name: String)
