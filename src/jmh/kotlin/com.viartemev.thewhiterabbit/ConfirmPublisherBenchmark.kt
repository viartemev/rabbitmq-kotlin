package com.viartemev.thewhiterabbit

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.ConfirmChannel
import com.viartemev.thewhiterabbit.channel.createConfirmChannel
import com.viartemev.thewhiterabbit.publisher.ConfirmPublisher
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import com.viartemev.thewhiterabbit.queue.DeleteQueueSpecification
import com.viartemev.thewhiterabbit.queue.QueueSpecification
import com.viartemev.thewhiterabbit.queue.declareQueue
import com.viartemev.thewhiterabbit.queue.deleteQueue
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class ConfirmPublisherBenchmark {

    private val testQueueName = "jmh_test_queue"
    private lateinit var connection: Connection
    private lateinit var channel: ConfirmChannel
    private lateinit var publisher: ConfirmPublisher
    private lateinit var messages: List<OutboundMessage>

    @Setup
    fun setupConnection() {
        val factory = ConnectionFactory().apply { useNio() }
        connection = factory.newConnection()
    }

    @Setup(Level.Iteration)
    fun setup() {
        channel = connection.createConfirmChannel()
        runBlocking { channel.declareQueue(QueueSpecification(testQueueName)) }
        publisher = channel.publisher()
        messages = (1..1000).map { createMessage() }
    }

    @TearDown(Level.Iteration)
    fun tearDown() {
        runBlocking { channel.deleteQueue(DeleteQueueSpecification(testQueueName)) }
        channel.close()
    }

    @Benchmark
    fun sendWithPublishConfirm(blackhole: Blackhole) = runBlocking {
        blackhole.consume(publisher.publishWithConfirm(messages))
    }

    private fun createMessage(): OutboundMessage = OutboundMessage("", testQueueName, MessageProperties.MINIMAL_BASIC, "")
}
