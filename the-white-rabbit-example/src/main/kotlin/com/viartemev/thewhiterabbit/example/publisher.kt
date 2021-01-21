package com.viartemev.thewhiterabbit.example

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import kotlinx.coroutines.*
import java.util.concurrent.Executors

const val PUBLISHER_EXCHANGE_NAME = ""
const val PUBLISHER_QUEUE_NAME = "test_queue"
const val TIMES = 5

fun main() {
    val dispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    val connectionFactory = ConnectionFactory().apply { useNio() }
    val connection = connectionFactory.newConnection()
    runBlocking {
        connection.confirmChannel {
            publish {
                val publ = this
                try {
                    supervisorScope {
                        val msg = (1..TIMES)
                            .map { createMessage("") }
                            .plus(createMessage("boom"))
                            .plus(createMessage("boom"))
                            .plus((1..TIMES).map { createMessage("") })
                        val map1 = msg
                            .map { async(dispatcher) { publishWithConfirm(it) } }
                        println("Sended async")
                        val messages = map1
                            .map {
                                try {
                                    it.await()
                                } catch (e: Throwable) {
                                    println("Yeeeah: $e")
                                    false
                                }
                            }
                        delay(2000)
                        println("Confirms list size: ${messages.size}")
                        val partitions: Pair<List<Boolean>, List<Boolean>> = messages.partition { it }
                        println("Delivered: ${partitions.first.size}")
                        println("Undelivered: ${partitions.second.size}")
                    }
                } catch (e: Throwable) {
                    println("Here an error: $e")
                }
            }
        }
    }
    connection.close()
    dispatcher.close()
}

private fun createMessage(body: String) =
    OutboundMessage(PUBLISHER_EXCHANGE_NAME, PUBLISHER_QUEUE_NAME, MessageProperties.PERSISTENT_BASIC, body)
