package com.viartemev.thewhiterabbit.queue

import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueueTest : AbstractTestContainersTest() {


    @Test
    fun `declare a queue test`() {
        val queueName = "declare_queue_test"
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    channel.declareQueue(QueueSpecification(queueName))
                }
            }
        }
        assertNotNull(httpRabbitMQClient.getQueue(DEFAULT_VHOST, queueName))
    }

    @Test
    fun `delete a queue test`() {
        val queueName = "delete_queue_test"
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    channel.declareQueue(QueueSpecification(queueName))
                    assertNotNull(httpRabbitMQClient.getQueue(DEFAULT_VHOST, queueName))
                    channel.deleteQueue(DeleteQueueSpecification(queueName))
                    assertNull(httpRabbitMQClient.getQueue(DEFAULT_VHOST, queueName))
                }
            }
        }
    }
}
