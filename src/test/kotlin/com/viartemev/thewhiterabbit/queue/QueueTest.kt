package com.viartemev.thewhiterabbit.queue

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import com.rabbitmq.client.Channel
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

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueueTest {

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
    fun `declare queue test`() {
        val queueName = "declare_queue_test"
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    channel.declareQueue(QueueSpecification(queueName))
                }
            }
        }
        assertTrue { getQueues().find { it.name == queueName } != null }
    }

    @Test
    @Disabled("FIXME")
    fun `delete queue test`() {
        val queueName = "delete_queue_test"
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    declareQueue(queueName, channel)
                    channel.deleteQueue(DeleteQueueSpecification(queueName))
                }
            }
        }
        assertTrue { getQueues().isEmpty() }
    }

    private fun getQueues(): List<QueuesHttpResponse> {
        val (_, _, response) = Fuel.get("http://localhost:${rabbitmq.managementPort()}/api/queues").authenticate("guest", "guest").responseObject<List<QueuesHttpResponse>>()
        val queues = response.get()
        assertNotNull(queues)
        return queues
    }

    private suspend fun declareQueue(queueName: String, channel: Channel) {
        channel.declareQueue(QueueSpecification(queueName))
        val (_, _, response) = Fuel.get("http://localhost:${rabbitmq.managementPort()}/api/queues").authenticate("guest", "guest").responseObject<List<QueuesHttpResponse>>()
        val queues = response.get()
        assertNotNull(queues)
        assertTrue { queues.isNotEmpty() }
        assertTrue { queues.find { it.name == queueName } != null }
    }
}

data class QueuesHttpResponse(val name: String, val messages: Long)
