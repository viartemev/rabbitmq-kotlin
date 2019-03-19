package com.viartemev.thewhiterabbit.publisher

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.mock
import com.rabbitmq.client.Channel
import com.rabbitmq.client.MessageProperties
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger


class ConfirmPublisherTest {

    @Test
    fun test_1() {
        val channel = mock<Channel> {
            on { basicPublish(any(), any(), any(), any()) } doThrow IOException("Boom")
        }
        val counter = AtomicInteger()
        val confirmPublisher = ConfirmPublisher(channel)

        val failed = OutboundMessage("", "", MessageProperties.PERSISTENT_BASIC, "fail")
        runBlocking {
            try {
                coroutineScope {
                    val task1 = async {
                        println("Task1 has started...")
                        delay(100)
                        confirmPublisher.publishWithConfirm(failed)
                        counter.getAndAdd(1)
                        println("Task1 finished")
                    }
                    val task2 = async {
                        println("Task2 has started...")
                        delay(2000)
                        counter.getAndAdd(1)
                        println("Task2 finished")
                    }
                    val task3 = async {
                        println("Task3 has started...")
                        delay(10000)
                        counter.getAndAdd(1)
                        println("Task3 finished")
                    }
                    awaitAll(task1, task2, task3)
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
