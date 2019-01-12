package com.viartemev.thewhiterabbit.exchange

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

//FIXME add testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExchangeTest {
    private val EXCHANGE_NAME = "name"
    lateinit var factory: ConnectionFactory


    @BeforeAll
    fun setUp() {
        factory = ConnectionFactory()
        factory.host = "localhost"
    }

    @Test
    fun `exchange declaration test`() = runBlocking {
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                channel.declareExchange(ExchangeSpecification(EXCHANGE_NAME))
                val (_, _, response) = Fuel.get("http://localhost:8080/api/exchanges").authenticate("guest", "guest").responseObject<List<ExchangesHttpResponse>>()
                val exchanges = response.component1()
                assertNotNull(exchanges)
                assertTrue { exchanges.isNotEmpty() }
                assertTrue { exchanges.find { it.name == EXCHANGE_NAME } != null }
            }
        }
    }
}


data class ExchangesHttpResponse(val name: String)
