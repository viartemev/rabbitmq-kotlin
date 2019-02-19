package com.viartemev.thewhiterabbit.exchange

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExchangeTest : AbstractTestContainersTest() {

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
        val (_, _, response) = Fuel.get("http://${rabbitmq.containerIpAddress}:${rabbitmq.managementPort()}/api/exchanges").authenticate(
            "guest",
            "guest"
        ).responseObject<List<ExchangesHttpResponse>>()
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
