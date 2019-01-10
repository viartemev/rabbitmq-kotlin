package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.ConnectionFactory
import com.viartemev.thewhiterabbit.queue.Queue
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

//FIXME add testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsumerTest {

    private val QUEUE_NAME = "test_queue"
    lateinit var factory: ConnectionFactory

    @BeforeAll
    fun setUp() {
        factory = ConnectionFactory()
        factory.host = "localhost"
        factory.useNio()
    }

    @Test
    fun `test one message publishing`() {
        factory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                runBlocking {
                    Queue.declareQueue(channel, QueueSpecification(QUEUE_NAME))
                    val consumer = Consumer(channel, QUEUE_NAME)
                    for (i in 1..3) {
                        launch { consumer.consume { println("Delivered a message: ${String(it.body)}") } }
                    }
                }
            }
        }
    }
}