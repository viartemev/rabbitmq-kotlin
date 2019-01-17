package com.viartemev.thewhiterabbit.queue

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import com.rabbitmq.client.ConnectionFactory
import com.viartemev.thewhiterabbit.utils.RabbitMQContainer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueueTest {

    private val QUEUE_NAME = "test_queue"
    lateinit var factory: ConnectionFactory
    @Container
    private val rabbitmq = RabbitMQContainer()

    @BeforeAll
    fun setUp() {
        rabbitmq.start()
        factory = ConnectionFactory()
        factory.host = rabbitmq.containerIpAddress.toString()
        factory.port = rabbitmq.getMappedPort(rabbitmq.connectionPort)
    }

    @Test
    fun `queue declaration test`() = runBlocking {
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                channel.declareQueue(QueueSpecification(QUEUE_NAME))
                val (_, _, response) = Fuel.get("http://localhost:${rabbitmq.getMappedPort(rabbitmq.managementPort)}/api/queues").authenticate("guest", "guest").responseObject<List<QueuesHttpResponse>>()
                val queues = response.get()
                assertNotNull(queues)
                assertTrue { queues.isNotEmpty() }
                assertTrue { queues.find { it.name == QUEUE_NAME } != null }
            }
        }
    }
}

data class QueuesHttpResponse(val name: String)
