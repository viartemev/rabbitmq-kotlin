package com.viartemev.thewhiterabbit.exchange

import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.utils.getExchange
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ExchangeTest : AbstractTestContainersTest() {
    val EXCHANGE_NAME = "name"

    @Test
    fun `an exchange declaration test`() {
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    channel.declareExchange(ExchangeSpecification(EXCHANGE_NAME))
                    val exchange = getExchange(EXCHANGE_NAME)
                    assertNotNull(exchange)
                }
            }
        }
    }

    @Test
    fun `an exchange deletion test`() {
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    channel.declareExchange(ExchangeSpecification(EXCHANGE_NAME))
                    val declaredExchange = getExchange(EXCHANGE_NAME)
                    assertNotNull(declaredExchange)
                    channel.deleteExchange(DeleteExchangeSpecification(EXCHANGE_NAME))
                    val exchangeAfterDeletion = getExchange(EXCHANGE_NAME)
                    assertNull(exchangeAfterDeletion)
                }
            }
        }
    }

}
