package com.viartemev.thewhiterabbit.consumer

import com.rabbitmq.client.Delivery
import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.channel.consume
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("For demo purposes only")
class ConfirmConsumerTest : AbstractTestContainersTest() {

    private val QUEUE_NAME = "test_queue"

    @Test
    fun `test message consuming`() {
        factory.newConnection().use { connection ->
            runBlocking {
                connection.channel {
                    declareQueue(QueueSpecification(QUEUE_NAME))
                    consume(QUEUE_NAME, 2) {
                        for (i in 1..3) consumeMessageWithConfirm({ handleDelivery(it) })
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
