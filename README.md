# RabbitMQ Kotlin
[![CI](https://github.com/viartemev/rabbitmq-kotlin/actions/workflows/gradle.yml/badge.svg?branch=master)](https://github.com/viartemev/rabbitmq-kotlin/actions/workflows/gradle.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

The RabbitMQ Kotlin Coroutine Library is designed to provide Kotlin developers with an efficient, coroutine-based approach to interact with RabbitMQ.
This library simplifies message queue operations by integrating seamlessly with Kotlin's coroutines, offering a modern and reactive way to handle asynchronous messaging in Kotlin applications.
It supports a variety of advanced features including queue and exchange manipulations, message publishing with confirmation, message consuming with acknowledgment, and transactional operations.

## Features

- **Queue and Exchange Manipulations**: Easily create, delete, and configure queues and exchanges. Supports all RabbitMQ exchange types (direct, topic, headers, fanout) and offers flexible options for queue bindings and attributes.
- **Message Publishing with Confirmation**: Publish messages to queues with the option to receive confirmations, ensuring reliable delivery and handling of messages.
- **Message Consuming with Acknowledgment**: Consume messages from queues with acknowledgment support, allowing for precise control over message processing and acknowledging.
- **Transactional Publishing and Consuming**: Support for transactional operations, enabling the grouping of publish and consume actions into atomic units, ensuring data consistency and reliability.

## Getting Started
You need to have [Java 8](https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html) installed.

### Snapshots
```gradle
repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("io.github.viartemev:rabbitmq-kotlin:0.7.0-SNAPSHOT")
}
```

## Examples
Full list of examples could be found [here](rabbitmq-kotlin-example/src/main)

### Asynchronous message publishing with confirmation

This library provides a robust and coroutine-friendly way to publish messages and wait for broker confirmations (ACK/NACK), ensuring message delivery reliability. It uses the `ConfirmPublisher` internally, which manages message correlation and limits the number of in-flight messages.

**Basic Usage:**

Obtain a channel in confirm mode (using `confirmChannel` extension) and then use the `publish` extension which gives access to `ConfirmPublisher` methods like `publishWithConfirm`.

```kotlin
import com.rabbitmq.client.ConnectionFactory
import io.github.viartemev.rabbitmq.channel.confirmChannel
import io.github.viartemev.rabbitmq.channel.publish
import io.github.viartemev.rabbitmq.message.OutboundMessage // Assuming OutboundMessage is defined elsewhere
import io.github.viartemev.rabbitmq.queue.QueueSpecification
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val connectionFactory = ConnectionFactory().apply { /* configure */ }
    val queueName = "my-confirm-queue"

    try {
        connectionFactory.newConnection().use { connection ->
            connection.confirmChannel { // Enables confirm mode and provides a ConfirmChannel
                // Ensure queue exists
                declareQueue(QueueSpecification(queueName))

                // Use the publish scope which gives access to ConfirmPublisher methods
                publish { 
                    val message = OutboundMessage(exchange = "", routingKey = queueName, body = "Hello Confirm!")

                    try {
                        // Publish and wait for confirmation (indefinitely by default)
                        val acked: Boolean = publishWithConfirm(message)

                        if (acked) {
                            println("Message successfully ACKed by broker.")
                        } else {
                            println("Message NACKed by broker. Needs retry or handling.")
                            // Implement retry logic or error handling here
                        }

                    } catch (e: Exception) {
                        println("Failed to publish or confirm message: ${e.message}")
                        // Handle exceptions like IOException, CancellationException etc.
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("Connection or channel error: ${e.message}")
    }
}

// Helper function to create a message (replace with your actual implementation)
// fun createMessage(queue: String, body: String): OutboundMessage { ... }
```

**Usage with Timeout:**

You can specify a timeout to avoid waiting indefinitely for a confirmation.

```kotlin
import kotlinx.coroutines.TimeoutCancellationException
// ... other imports ...

// Inside the publish { ... } block:
val messageWithTimeout = OutboundMessage(exchange = "", routingKey = queueName, body = "Hello with Timeout!")
val confirmationTimeout = 5000L // 5 seconds

try {
    val acked: Boolean = publishWithConfirm(messageWithTimeout, timeoutMillis = confirmationTimeout)
    if (acked) {
        println("Message (with timeout) successfully ACKed.")
    } else {
        println("Message (with timeout) NACKed.")
    }
} catch (e: TimeoutCancellationException) {
    println("Confirmation timed out after $confirmationTimeout ms.")
    // Handle timeout scenario (e.g., retry, log)
} catch (e: Exception) {
    println("Failed to publish or confirm message (with timeout): ${e.message}")
}
```

**Handling Multiple Messages:**

You can easily publish multiple messages concurrently using coroutines. The `ConfirmPublisher` limits the number of concurrent unconfirmed messages using the `maxInFlightMessages` parameter (default 1000, configurable when creating the publisher if needed, though typically managed by the `confirmChannel` extension).

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
// ... other imports ...

// Inside the publish { ... } block:
val messages = (1..10).map {
    OutboundMessage(exchange = "", routingKey = queueName, body = "Message #$it")
}

try {
    val results: List<Boolean> = messages.map { msg ->
        // Launch each publish call asynchronously
        async { publishWithConfirm(msg, timeoutMillis = 10000L) } 
    }.awaitAll() // Wait for all confirmations (or timeouts/errors)

    val ackCount = results.count { it }
    val nackCount = results.size - ackCount
    println("Published ${messages.size} messages. ACKs: $ackCount, NACKs: $nackCount")

} catch (e: Exception) {
    // Handle exceptions that might occur during awaitAll or within async blocks
    println("Error publishing multiple messages: ${e.message}")
}

```

Remember to handle potential exceptions like `IOException` (network issues), `TimeoutCancellationException` (if using timeouts), and `CancellationException` (if the coroutine scope is cancelled). The boolean return value indicates whether the broker acknowledged (`true`) or negatively acknowledged (`false`) the message.

### Asynchronous message consuming with acknowledgement
Consume only n-messages:
```kotlin
val connectionFactory = ConnectionFactory().apply { useNio() }
    connectionFactory.newConnection().use { connction ->
        connction.channel {
            consume(CONSUMER_QUEUE_NAME, 1) {
                (1..CONSUME_TIMES).map { async(Dispatchers.IO) { consumeMessageWithConfirm(handler) } }.awaitAll()
            }
        }
    }
```

### Transactional publishing and consuming

RabbitMQ and AMQP itself offer rather scarce support for transaction. When considering using transactions you should be aware that:
* a transaction could only span one channel and one queue;
* `com.rabbitmq.client.Channel` is not thread-safe;
* channel can be either in confirm mode or in transaction mode at a time;
* transactions cannot be nested into each other;

 The library provides a convenient way to perform transactional publishing and receiving based on `transaction` extension function. This function commits a transaction upon normal execution of the block and rolls it back if a `RuntimeException` occurs. Exceptions are always propagated further. Coroutines are not used for publishing though, since there are no any asynchronous operations involved.

```kotlin
connection.txChannel {
    transaction {
        val message = createMessage(queue = oneTimeQueue, body = "Hello from tx")
        publish(message)
    }
}
```

## Links
* [Benchmarks](https://github.com/viartemev/the-white-rabbit/issues/88#issuecomment-470461937)
