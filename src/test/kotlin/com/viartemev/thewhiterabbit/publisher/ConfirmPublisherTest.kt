package com.viartemev.thewhiterabbit.publisher

import com.nhaarman.mockito_kotlin.*
import com.rabbitmq.client.Channel
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.common.RabbitMqDispatchers
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.mockito.AdditionalMatchers
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume


class ConfirmPublisherTest {
    private val logger = KotlinLogging.logger {}
    private val poisonMessage = OutboundMessage("", "", MessageProperties.PERSISTENT_BASIC, "poisonMessage")
    private val validMessage = OutboundMessage("", "", MessageProperties.PERSISTENT_BASIC, "validMessage")

    @Test
    fun `publish several messages with one poison message coroutineScope`() {
        val messageCounter = AtomicLong()
        val channel = mock<Channel>() {
            on { nextPublishSeqNo } doAnswer { messageCounter.addAndGet(1) }
        }
        doNothing().`when`(channel)
            .basicPublish(any(), any(), any(), AdditionalMatchers.aryEq("validMessage".toByteArray()))
        doThrow(IOException("Boom")).`when`(channel)
            .basicPublish(any(), any(), any(), AdditionalMatchers.aryEq("poisonMessage".toByteArray()))

        val confirmPublisher = ConfirmPublisher(channel)
        runBlocking {
            try {
                val acks: List<Boolean> = coroutineScope {
                    val task1 = async(RabbitMqDispatchers.SingleThreadDispatcher) {
                        logger.info { "Task1 has started..." }
                        val ack = confirmPublisher.publishWithConfirm(poisonMessage)
                        logger.info { "Task1 has finished" }
                        ack
                    }
                    val task2 = async(RabbitMqDispatchers.SingleThreadDispatcher) {
                        logger.info { "Task2 has started..." }
                        val ack = confirmPublisher.publishWithConfirm(validMessage)
                        logger.info { "Task2 finished" }
                        ack
                    }
                    val task3 = async(RabbitMqDispatchers.SingleThreadDispatcher) {
                        logger.info { "Task3 has started..." }
                        val ack = confirmPublisher.publishWithConfirm(validMessage)
                        logger.info { "Task3 finished" }
                        ack
                    }
                    listOf(task1, task2, task3).map { saveHandleDeferred(it) }
                }
                fail("This is unreachable")
            } catch (e: Exception) {
                logger.error(e) { "The exception was caught" }
            }
        }
    }

    @Test
    fun `publish several messages with one poison message supervisorScope`() {
        val messageCounter = AtomicLong()
        val channel = mock<Channel>() {
            on { nextPublishSeqNo } doAnswer { messageCounter.addAndGet(1) }
        }
        doNothing().`when`(channel)
            .basicPublish(any(), any(), any(), AdditionalMatchers.aryEq("validMessage".toByteArray()))
        doThrow(IOException("Boom")).`when`(channel)
            .basicPublish(any(), any(), any(), AdditionalMatchers.aryEq("poisonMessage".toByteArray()))

        val confirmPublisher = ConfirmPublisher(channel)
        runBlocking {
            try {
                launch {
                    delay(1000)
                    confirmPublisher.continuations
                        .forEach { entry: Map.Entry<Long, Continuation<Boolean>> -> entry.value.resume(true) }
                    confirmPublisher.continuations.clear()
                }
                val acks: List<Boolean> = supervisorScope {
                    val task1 = async(RabbitMqDispatchers.SingleThreadDispatcher) {
                        logger.info { "Task1 has started..." }
                        val ack = confirmPublisher.publishWithConfirm(poisonMessage)
                        logger.info { "Task1 has finished" }
                        ack
                    }
                    val task2 = async(RabbitMqDispatchers.SingleThreadDispatcher) {
                        logger.info { "Task2 has started..." }
                        val ack = confirmPublisher.publishWithConfirm(validMessage)
                        logger.info { "Task2 finished" }
                        ack
                    }
                    val task3 = async(RabbitMqDispatchers.SingleThreadDispatcher) {
                        logger.info { "Task3 has started..." }
                        val ack = confirmPublisher.publishWithConfirm(validMessage)
                        logger.info { "Task3 finished" }
                        ack
                    }
                    listOf(task1, task2, task3).map { saveHandleDeferred(it) }
                }
                val (acked, naked) = acks.partition { it }
                assertTrue(acked.size == 2)
                assertTrue(naked.size == 1)
                assertTrue(confirmPublisher.continuations.isEmpty())
            } catch (e: Exception) {
                logger.error(e) { "The exception was caught" }
            }
        }
    }

    private suspend fun saveHandleDeferred(deferred: Deferred<Boolean>): Boolean {
        return try {
            deferred.await()
        } catch (e: Throwable) {
            logger.error(e) { "Failed to send the message" }
            false
        }
    }
}
