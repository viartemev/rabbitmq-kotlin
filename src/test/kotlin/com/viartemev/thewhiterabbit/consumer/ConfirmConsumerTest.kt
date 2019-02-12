package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.channel.consume
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@Disabled("FIXME add testcontainers and split local and CI env")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfirmConsumerTest {

    private val QUEUE_NAME = "test_queue"
    lateinit var factory: ConnectionFactory

    @BeforeAll
    fun setUp() {
        factory = ConnectionFactory()
        factory.host = "localhost"
        factory.useNio()
    }

    @Test
    fun `test message consuming`() {
        factory.newConnection().use { connection ->
            runBlocking {
                connection.channel {
                    declareQueue(QueueSpecification(QUEUE_NAME))
                    consume(QUEUE_NAME) {
                        for (i in 1..3) consumeWithConfirm({ handleDelivery(it) })
                    }
                }
            }
        }
    }

    suspend fun handleDelivery(message: Delivery) {
        println("Got a message: ${String(message.body)}. Let's do some async work...")
        delay(100)
        println("Work is done")
    }
}
