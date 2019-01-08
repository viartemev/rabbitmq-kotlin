package com.viartemev.thewhiterabbit.queue

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import com.rabbitmq.client.ConnectionFactory
import com.viartemev.thewhiterabbit.utils.QueuesHttpResponse
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

//FIXME move connection factory to before all and add testcontainers
class QueueTest {

    @Test
    fun `declaration test`() {
        val factory = ConnectionFactory()
        val QUEUE_NAME = "name"
        factory.host = "localhost"
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    Queue.declareQueue(channel, QueueSpecification(QUEUE_NAME))
                    val (_, _, response) = Fuel.get("http://localhost:8080/api/queues").authenticate("guest", "guest").responseObject<List<QueuesHttpResponse>>()
                    val createdQueueName = response.component1()?.get(0)?.name
                    assertEquals(createdQueueName, QUEUE_NAME)
                }
            }
        }
    }
}
