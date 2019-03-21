package com.viartemev.thewhiterabbit.example

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.rpc.RpcClient
import com.viartemev.thewhiterabbit.rpc.RpcOutboundMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

//TODO main is working after getting result
fun main() {
    val connectionFactory = ConnectionFactory().apply { useNio() }
    val connection = connectionFactory.newConnection()
    val channel = connection.createChannel()
    runBlocking {
        connection.channel {
            val requestQueueName = declareQueue(QueueSpecification("rpc_request")).queue
            val replyQueueName = declareQueue(QueueSpecification("rpc_reply")).queue
            thread(isDaemon = true) { RpcServer().run(connectionFactory, requestQueueName) }
            val message = RpcOutboundMessage("", requestQueueName, replyQueueName, MessageProperties.PERSISTENT_BASIC, "Slava".toByteArray())
            val rpcClient = RpcClient(channel)
            println("Asking for greeting request...")
            coroutineScope {
                val result = async { rpcClient.call(message) }
                async {
                    delay(5000)
                    println("Done job")
                }.await()
                println("Result: ${result.await()}")
            }
        }
    }
    connection.close()
}

class RpcServer {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()


    fun run(connectionFactory: ConnectionFactory, RPC_QUEUE_NAME: String) {
        connectionFactory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                channel.basicQos(1)

                println(" [x] Awaiting RPC requests")

                val deliverCallback = DeliverCallback { _, delivery ->
                    val replyProps = AMQP.BasicProperties.Builder()
                        .correlationId(delivery.properties.correlationId)
                        .build()
                    var response: String? = null
                    try {
                        val message = String(delivery.body)

                        println(" [.] message: ($message)")
                        response = "Hello, $message"
                    } catch (e: RuntimeException) {
                        println(" [.] $e")
                    } finally {
                        channel.basicPublish("", delivery.properties.replyTo, replyProps, response?.toByteArray())
                        channel.basicAck(delivery.envelope.deliveryTag, false)
                        // RabbitMq consumer worker thread notifies the RPC server owner thread
                        lock.withLock {
                            condition.signal()
                        }
                    }
                }

                channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, CancelCallback { })
                // Wait and be prepared to consume the message from RPC client.
                while (true) {
                    lock.withLock {
                        try {
                            condition.await()
                        } catch (e: InterruptedException) {
                            println("$e")
                        }
                    }
                }
            }
        }
    }
}
