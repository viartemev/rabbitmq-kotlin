package com.viartemev.thewhiterabbit.example.rpc

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

fun main() {
    val connectionFactory = ConnectionFactory().apply { useNio() }
    RpcServer().run(connectionFactory, "rpc_request")
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
