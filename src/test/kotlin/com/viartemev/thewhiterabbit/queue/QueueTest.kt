package com.viartemev.thewhiterabbit.queue

import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.utils.getQueue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertNull

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
        assertNotNull(getQueue(queueName))
    }

    @Test
    fun `delete a queue test`() {
        val queueName = "delete_queue_test"
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    channel.declareQueue(QueueSpecification(queueName))
                    assertNotNull(getQueue(queueName))
                    channel.deleteQueue(DeleteQueueSpecification(queueName))
                    assertNull(getQueue(queueName))
                }
            }
        }
    }
}

data class QueuesHttpResponse(val name: String, val messages: Long)
