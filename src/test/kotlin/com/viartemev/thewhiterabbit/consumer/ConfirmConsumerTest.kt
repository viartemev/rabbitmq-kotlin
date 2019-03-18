package com.viartemev.thewhiterabbit.consumer

import com.viartemev.thewhiterabbit.AbstractTestContainersTest
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.consume
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.utils.createMessage
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class ConfirmConsumerTest : AbstractTestContainersTest() {

    private val QUEUE_NAME = "test_queue"

    @Test
    fun `test message consuming`() {
        val counter = AtomicInteger()
        factory.newConnection().use { connection ->
            runBlocking {
                connection.confirmChannel {
                    declareQueue(QueueSpecification(QUEUE_NAME))
                    publish {
                        val messages = (1..10).map { createMessage(queue = QUEUE_NAME, body = "1") }
                        publishWithConfirmAsync(messages = messages)
                    }
                    consume(QUEUE_NAME, 2) {
                        for (i in 1..10) consumeMessageWithConfirm {
                            delay(100)
                            counter.getAndAdd(String(it.body).toInt())
                        }
                    }
                    assertEquals(10, counter.get())
                }
            }
        }
    }
}
