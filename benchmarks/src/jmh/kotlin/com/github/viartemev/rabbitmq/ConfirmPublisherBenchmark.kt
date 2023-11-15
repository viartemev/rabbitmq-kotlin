package com.github.viartemev.rabbitmq

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.ConfirmChannel
import com.viartemev.thewhiterabbit.channel.createConfirmChannel
import com.viartemev.thewhiterabbit.publisher.ConfirmPublisher
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
@State(Scope.Benchmark)
open class ConfirmPublisherBenchmark {

    @Param("1", "10", "100", "1000", "10000", "100000")
    private var numberOfMessages: Int = 0
    private val testQueueName = "jmh_test_queue"
    private val factory = ConnectionFactory().apply { useNio() }
    private lateinit var connection: Connection
    private lateinit var channel: ConfirmChannel
    private lateinit var publisher: ConfirmPublisher
    private lateinit var messages: List<OutboundMessage>

    @Setup(Level.Iteration)
    fun setup() {
        connection = factory.newConnection()
        channel = connection.createConfirmChannel()
        channel.queueDeclare(testQueueName, false, false, false, mapOf())
        publisher = channel.publisher()
        messages = (1..numberOfMessages).map { OutboundMessage("", testQueueName, MessageProperties.MINIMAL_BASIC, "") }
    }

    @TearDown(Level.Iteration)
    fun tearDownPublisher() {
        channel.queueDelete(testQueueName)
        channel.close()
        connection.close()
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    fun avgTimeSendWithPublishConfirm(blackhole: Blackhole) = runBlocking {
        blackhole.consume(messages.map { async(Dispatchers.IO) { publisher.publishWithConfirm(it) } }.awaitAll())
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    fun throughputSendWithPublishConfirm(blackhole: Blackhole) = runBlocking {
        blackhole.consume(messages.map { async(Dispatchers.IO) { publisher.publishWithConfirm(it) } }.awaitAll())
    }
}
