package com.viartemev.thewhiterabbit.queue

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
class QueueTest {
    private val QUEUE_NAME = "name"
    lateinit var factory: ConnectionFactory


    @BeforeAll
    fun setUp() {
        factory = ConnectionFactory()
        factory.host = "localhost"
    }

    @Test
    fun `queue declaration test`() = runBlocking {
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                channel.declareQueue(QueueSpecification(QUEUE_NAME))
                val (_, _, response) = Fuel.get("http://localhost:8080/api/queues").authenticate("guest", "guest").responseObject<List<QueuesHttpResponse>>()
                val queues = response.component1()
                assertNotNull(queues)
                assertTrue { queues.isNotEmpty() }
                assertTrue { queues.find { it.name == QUEUE_NAME } != null }
            }
        }
    }
}


data class QueuesHttpResponse(val name: String)
