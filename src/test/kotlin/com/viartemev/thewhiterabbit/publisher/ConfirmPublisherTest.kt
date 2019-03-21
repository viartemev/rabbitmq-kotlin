package com.viartemev.thewhiterabbit.publisher

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.mock
import com.rabbitmq.client.Channel
import com.rabbitmq.client.MessageProperties
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.mockito.AdditionalMatchers
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger


class ConfirmPublisherTest {
    private val poisonMessage = OutboundMessage("", "", MessageProperties.PERSISTENT_BASIC, "poisonMessage")
    private val validMessage = OutboundMessage("", "", MessageProperties.PERSISTENT_BASIC, "validMessage")

    @Test
    fun `publish several messages with one poison message`() {
        val channel = mock<Channel>()
        doNothing().`when`(channel).basicPublish(any(), any(), any(), AdditionalMatchers.aryEq("validMessage".toByteArray()))
        doThrow(IOException("Boom")).`when`(channel).basicPublish(any(), any(), any(), AdditionalMatchers.aryEq("poisonMessage".toByteArray()))

        val counter = AtomicInteger()
        val confirmPublisher = ConfirmPublisher(channel)

        runBlocking {
            try {
                coroutineScope {
                    val task1 = async {
                        println("Task1 has started...")
                        delay(50)
                        confirmPublisher.publishWithConfirm(poisonMessage)
                        counter.getAndAdd(1)
                        println("Task1 finished")
                    }
                    val task2 = async {
                        println("Task2 has started...")
                        confirmPublisher.publishWithConfirm(validMessage)
                        println("Task2 finished")
                    }
                    val task3 = async {
                        println("Task3 has started...")
                        confirmPublisher.publishWithConfirm(validMessage)
                        counter.getAndAdd(1)
                        println("Task3 finished")
                    }
                    awaitAll(task1, task2, task3)
                }
                fail("The method didn't throw when I expected it to")
            } catch (e: IOException) {
                println("CancellationException caught: $e")
            }
        }
        assertEquals(0, counter.get())
        assertTrue(confirmPublisher.continuations.isEmpty())
    }

    @Test
    fun `publish batch of messages with one poison message`() {
        val channel = mock<Channel>()
        doNothing().`when`(channel).basicPublish(any(), any(), any(), AdditionalMatchers.aryEq("validMessage".toByteArray()))
        doThrow(IOException("Boom")).`when`(channel).basicPublish(any(), any(), any(), AdditionalMatchers.aryEq("poisonMessage".toByteArray()))

        val counter = AtomicInteger()
        val confirmPublisher = ConfirmPublisher(channel)

        runBlocking {
            try {
                coroutineScope {
                    confirmPublisher.publishWithConfirmAsync(this.coroutineContext, messages = listOf(validMessage, poisonMessage)).awaitAll()
                }
                fail("The method didn't throw when I expected it to")
            } catch (e: CancellationException) {
                println("CancellationException caught: $e")
            }
        }
        assertEquals(0, counter.get())
        assertTrue(confirmPublisher.continuations.isEmpty())
    }
}
