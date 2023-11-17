# RabbitMQ Kotlin
[![CI](https://github.com/viartemev/rabbitmq-kotlin/actions/workflows/gradle.yml/badge.svg?branch=master)](https://github.com/viartemev/rabbitmq-kotlin/actions/workflows/gradle.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

The RabbitMQ Kotlin Coroutine Library is designed to provide Kotlin developers with an efficient, coroutine-based approach to interact with RabbitMQ.  
This library simplifies message queue operations by integrating seamlessly with Kotlin's coroutines, offering a modern and reactive way to handle asynchronous messaging in Kotlin applications.   
It supports a variety of advanced features including queue and exchange manipulations, message publishing with confirmation, message consuming with acknowledgment, transactional operations, and the Remote Procedure Call (RPC) pattern.  

## Features

- **Queue and Exchange Manipulations**: Easily create, delete, and configure queues and exchanges. Supports all RabbitMQ exchange types (direct, topic, headers, fanout) and offers flexible options for queue bindings and attributes.
- **Message Publishing with Confirmation**: Publish messages to queues with the option to receive confirmations, ensuring reliable delivery and handling of messages.
- **Message Consuming with Acknowledgment**: Consume messages from queues with acknowledgment support, allowing for precise control over message processing and acknowledging.
- **Transactional Publishing and Consuming**: Support for transactional operations, enabling the grouping of publish and consume actions into atomic units, ensuring data consistency and reliability.
- **RPC Pattern Implementation**: Facilitates the implementation of the RPC pattern, allowing for easy setup of request-response message flows, suitable for service-oriented architectures.

## Getting Started
You need to have [Java 8](https://www.oracle.com/technetwork/java/javase/downloads/index.html) installed.

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
Full list of examples could be found [here](https://github.com/viartemev/rabbitmq-kotlin/tree/master/rabbitmq-kotlin-example/src/main)

### Asynchronous message publishing with confirmation
```kotlin
    val connectionFactory = ConnectionFactory().apply { useNio() }
    connectionFactory.newConnection().use { connection ->
        connection.confirmChannel {
            declareQueue(QueueSpecification(PUBLISHER_QUEUE_NAME)).queue
            publish {
                (1..TIMES).map { createMessage("") }.map { async(Dispatchers.IO) { publishWithConfirm(it) } }.awaitAll()
                    .forEach { println(it) }
            }
        }
    }
```

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

### RPC pattern
```kotlin
ConnectionFactory().apply { useNio() }.newConnection().use { conn ->
        conn.channel {
            logger.info { "Asking for greeting request..." }
            val response = withTimeoutOrNull(1000) {
                async(Dispatchers.IO) {
                    rpc {
                        val result = call(message)
                        logger.info { "Got a message: ${String(result.body)}" }
                        result
                    }
                }.await()
            }
            if (response == null) {
                logger.info { "Timeout is exeeded" }
            } else {
                logger.info { "Result: ${String(response.body)}" }
            }
        }
    }
```

## Links
* [Benchmarks](https://github.com/viartemev/the-white-rabbit/issues/88#issuecomment-470461937)
