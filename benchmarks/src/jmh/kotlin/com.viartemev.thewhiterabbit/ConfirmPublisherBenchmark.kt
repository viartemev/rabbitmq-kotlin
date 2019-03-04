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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
open class ConfirmPublisherBenchmark {

    @Param("1")
    private var numberOfMessages: Int = 0
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
        messages = (1..numberOfMessages).map { createMessage() }
    }

    @TearDown(Level.Iteration)
    fun tearDownPublisher() {
        runBlocking { channel.deleteQueue(DeleteQueueSpecification(testQueueName)) }
        channel.close()
    }

    @TearDown
    fun tearDown() {
        connection.close()
    }

    @Benchmark
    fun sendWithPublishConfirm(blackhole: Blackhole) = runBlocking {
        blackhole.consume(publisher.asyncPublishWithConfirm(messages).awaitAll())
    }

    private fun createMessage(): OutboundMessage = OutboundMessage("", testQueueName, MessageProperties.MINIMAL_BASIC, "")
}
