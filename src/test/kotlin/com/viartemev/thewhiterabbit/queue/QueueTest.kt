package com.viartemev.thewhiterabbit.queue

import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.channel.createConfirmChannel
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import com.viartemev.thewhiterabbit.utils.checkQueue
import com.viartemev.thewhiterabbit.utils.getQueue
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
        assertNotNull(getQueue(queueName))
    }

    @Test
    fun `delete a queue test`() {
        val queueName = "delete_queue_test"
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    channel.declareQueue(QueueSpecification(queueName))
                    checkQueue(queueName)
                    channel.deleteQueue(DeleteQueueSpecification(queueName))
                }
            }
        }
        assertNull(getQueue(queueName))
    }

    @Test
    fun `purge a queue test`() {
        val queueName = "purge_queue_test"
        factory.newConnection().use { connection ->
            connection.createConfirmChannel().use { channel ->
                runBlocking {
                    val publisher = channel.publisher()
                    channel.declareQueue(QueueSpecification(queueName))
                    checkQueue(queueName)
                    val messages = (1..100).map { OutboundMessage("", queueName, MessageProperties.PERSISTENT_BASIC, "Hello#$it") }
                    publisher.asyncPublishWithConfirm(messages).awaitAll()
                    val messagesBeforePurge = getQueue(queueName)?.messages
                    assertNotNull(messagesBeforePurge)
                    assertTrue { messagesBeforePurge != 0L }
                    channel.purgeQueue(PurgeQueueSpecification(queueName))
                    val messagesAfterPurge = getQueue(queueName)?.messages
                    assertNotNull(messagesAfterPurge)
                    assertTrue { messagesAfterPurge == 0L }
                }
            }
        }
    }
}
