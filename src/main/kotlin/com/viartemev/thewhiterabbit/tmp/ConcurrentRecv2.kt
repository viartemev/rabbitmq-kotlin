package com.viartemev.thewhiterabbit.tmp

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


object ConcurrentRecv2 {

    private val QUEUE_NAME = "hello"

    @JvmStatic
    fun main(args: Array<String>) {
        val threadNumber = 2
        val threadPool = ThreadPoolExecutor(threadNumber, threadNumber,
                0L, TimeUnit.MILLISECONDS,
                LinkedBlockingQueue())

        val connectionFactory = ConnectionFactory()
        connectionFactory.host = "localhost"

        val connection = connectionFactory.newConnection()
        val channel = connection.createChannel()

        println(" [*] Waiting for messages. To exit press CTRL+C")

        registerConsumer(channel, 5000, threadPool)

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                println("Invoking shutdown hook...")
                println("Shutting down thread pool...")
                threadPool.shutdown()
                try {
                    while (!threadPool.awaitTermination(10, TimeUnit.SECONDS));
                } catch (e: InterruptedException) {
                    println("Interrupted while waiting for termination")
                }

                println("Thread pool shut down.")
                println("Done with shutdown hook.")
            }
        })
    }

    private fun registerConsumer(channel: Channel, timeout: Int, threadPool: ExecutorService) {
        channel.exchangeDeclare(QUEUE_NAME, "fanout")
        channel.queueDeclare(QUEUE_NAME, false, false, false, null)
        channel.queueBind(QUEUE_NAME, QUEUE_NAME, "")

        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(consumerTag: String?,
                                        envelope: Envelope?,
                                        properties: AMQP.BasicProperties?,
                                        body: ByteArray?) {
                try {
                    val deliveryTag = envelope?.deliveryTag
                    println(String.format("Received (channel %d) %s", channel.channelNumber, String(body!!)))

                    threadPool.submit {
                        try {
                            Thread.sleep(timeout.toLong())
                            println(String.format("Processed %s %s", Thread.currentThread().name, String(body)))
                            channel.basicAck(deliveryTag ?: 1, false)
                        } catch (e: InterruptedException) {
                            println(String.format("Interrupted %s", String(body)))
                        }
                    }
                } catch (e: Exception) {
                    println(e)
                }

            }
        }

        channel.basicConsume(QUEUE_NAME, false /* auto-ack */, consumer)
    }
}
