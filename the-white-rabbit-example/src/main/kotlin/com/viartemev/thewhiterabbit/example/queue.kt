package com.viartemev.thewhiterabbit.example

import com.rabbitmq.client.ConnectionFactory
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.exchange.*
import com.viartemev.thewhiterabbit.queue.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors


fun main() {
    val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    singleThreadDispatcher.use { dispatcher ->
        val connectionFactory = ConnectionFactory().apply { useNio() }
        connectionFactory.newConnection().use { connection ->
            runBlocking {
                connection.channel {
                    declareQueue(QueueSpecification("queue-1"), dispatcher)
                    bindQueue(BindQueueSpecification("queue-1", "exchange-1", "key"))
                    purgeQueue(PurgeQueueSpecification("queue-1"))
                    unbindQueue(UnbindQueueSpecification("queue-1", "exchange-1", "key"))
                    deleteQueue(DeleteQueueSpecification("queue-1"), dispatcher)
                }
            }
        }
    }
}
