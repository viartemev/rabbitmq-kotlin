# The White Rabbit

[![Build Status](https://travis-ci.org/viartemev/the-white-rabbit.svg?branch=master)](https://travis-ci.org/viartemev/the-white-rabbit)
[![Download](https://api.bintray.com/packages/viartemev/Maven/the-white-rabbit/images/download.svg) ](https://bintray.com/viartemev/Maven/the-white-rabbit/_latestVersion)
[![Open Source Helpers](https://www.codetriage.com/viartemev/the-white-rabbit/badges/users.svg)](https://www.codetriage.com/viartemev/the-white-rabbit)
[![codecov](https://codecov.io/gh/viartemev/the-white-rabbit/branch/master/graph/badge.svg)](https://codecov.io/gh/viartemev/the-white-rabbit)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Gitter](https://badges.gitter.im/kotlin-the-white-rabbit/community.svg)](https://gitter.im/kotlin-the-white-rabbit/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

The White Rabbit is a [fast](https://github.com/viartemev/the-white-rabbit/issues/88#issuecomment-470461937) and
asynchronous RabbitMQ (AMQP) client library based on Kotlin coroutines. Currently the following features are supported:

* Queue and exchange manipulations
* Message publishing with confirmation
* Message consuming with acknowledgment
* Transactional publishing and consuming
* RPC pattern

## Motivation

TODO

## JMH Benchmarks
The code can be found [here](https://github.com/viartemev/the-white-rabbit/tree/master/benchmarks/src/jmh). <br/>
Results of the benchmark and comparison with [reactor-rabbitmq](https://github.com/reactor/reactor-rabbitmq) are presented below, for details you can check the [discussion](https://github.com/viartemev/the-white-rabbit/issues/88#issuecomment-470461937).
```kotlin
The White Rabbit

Benchmark                                         (numberOfMessages)  Mode  Cnt        Score   Error  Units
ConfirmPublisherBenchmark.sendWithPublishConfirm                   1  avgt    2      104.172          us/op
ConfirmPublisherBenchmark.sendWithPublishConfirm                  10  avgt    2      598.625          us/op
ConfirmPublisherBenchmark.sendWithPublishConfirm                 100  avgt    2     3845.833          us/op
ConfirmPublisherBenchmark.sendWithPublishConfirm                1000  avgt    2    36108.709          us/op
ConfirmPublisherBenchmark.sendWithPublishConfirm               10000  avgt    2   392132.353          us/op
ConfirmPublisherBenchmark.sendWithPublishConfirm              100000  avgt    2  4098567.349          us/op

Reactor RabbitMQ

Benchmark                                (nbMessages)  Mode  Cnt        Score   Error  Units
SenderBenchmark.sendWithPublishConfirms             1  avgt    2      697.424          us/op
SenderBenchmark.sendWithPublishConfirms            10  avgt    2     1306.490          us/op
SenderBenchmark.sendWithPublishConfirms           100  avgt    2     4819.441          us/op
SenderBenchmark.sendWithPublishConfirms          1000  avgt    2    39597.671          us/op
SenderBenchmark.sendWithPublishConfirms         10000  avgt    2   373226.865          us/op
SenderBenchmark.sendWithPublishConfirms        100000  avgt    2  3900685.520          us/op
```

## Building applications using The White Rabbit

You need to have Java 8 installed.
<details><summary>Gradle</summary>

```groovy
repositories {
    jcenter()
}

compile 'com.viartemev:the-white-rabbit:$version'
```
</details>

<details><summary>Maven</summary>

```xml
<repositories>
    <repository>
        <id>jcenter</id>
        <url>https://jcenter.bintray.com/</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.viartemev</groupId>
    <artifactId>the-white-rabbit</artifactId>
    <version>${version}</version>
</dependency>
```

</details>

## Usage notes and examples

### Important note
Channel instances must not be shared between threads.
> As a rule of thumb, sharing Channel instances between threads is something to be avoided.
> Applications should prefer using a Channel per thread instead of sharing the same Channel across multiple threads.
> While some operations on channels are safe to invoke concurrently, some are not and will result in incorrect frame interleaving on the wire, double acknowledgements and so on.

[From the RabbitMQ docs](https://www.rabbitmq.com/api-guide.html#concurrency)

The White Rabbit connection DSL **is thread-safe**.<br/>
One channel per a thread approach is used.<br/>
You should take care about thread-safety, if you don't use The White Rabbit DSL.

### Connection extension methods

Use one of the extension methods on `com.rabbitmq.client.Connection` to get a channel you need:
```kotlin
connection.channel {
    /**
     * The plain channel with consumer acknowledgments, supports:
     *   -- queue and exchange manipulations
     *   -- asynchronous consuming
     *   -- RPC pattern
     */
}

connection.confirmChannel {
    /**
     * Channel with publisher confirmations, additionally supports:
     *   -- asynchronous message publishing
     */
}

connection.txChannel {
    /**
     *  Supports transactional publishing and consuming.
     */
}
```

### An exchange manipulation

```kotlin
fun main() {
    val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    singleThreadDispatcher.use { dispatcher ->
        val connectionFactory = ConnectionFactory().apply { useNio() }
        connectionFactory.newConnection().use { connection ->
            runBlocking {
                connection.channel {
                    declareExchange(ExchangeSpecification("exchange-1"), dispatcher)
                    declareExchange(ExchangeSpecification("exchange-2"), dispatcher)
                    bindExchange(BindExchangeSpecification("exchange-1", "exchange-2", "key"))
                    unbindExchange(UnbindExchangeSpecification("exchange-1", "exchange-2", "key"))
                    deleteExchange(DeleteExchangeSpecification("exchange-1"), dispatcher)
                    deleteExchange(DeleteExchangeSpecification("exchange-2"), dispatcher)
                }
            }
        }
    }
}
```
TODO explain the code snippet

### A queue manipulation

```kotlin
fun main() {
    val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    singleThreadDispatcher.use { dispatcher ->
        val connectionFactory = ConnectionFactory().apply { useNio() }
        connectionFactory.newConnection().use { connection ->
            runBlocking {
                connection.channel {
                    declareQueue(QueueSpecification("queue-1"), dispatcher)
                    bindQueue(BindQueueSpecification("queue-1", "exchange-1", "key"))
                    purgeQueue(PurgeQueueSpecification("queue-1"))
                    unbindQueue(UnbindQueueSpecification("queue-1", "exchange-1", "key"))
                    deleteQueue(DeleteQueueSpecification("queue-1"), dispatcher)
                }
            }
        }
    }
}
```

TODO explain the code snippet

### Publishing with confirmation

#### Single message

```kotlin
fun main() {
    val logger = KotlinLogging.logger {}
    val connectionFactory = ConnectionFactory().apply { useNio() }
    connectionFactory.newConnection().use { connection ->
        runBlocking {
            connection.confirmChannel {
                publish {
                    val message = OutboundMessage("", "test_queue", PERSISTENT_BASIC, "hello world")
                    val ack: Boolean = publishWithConfirm(message)
                    logger.info { "Ack: $ack" }
                }
            }
        }
    }
}
```

TODO explain the code snippet
?? - you need to care about concurrent access by yourself

#### Batch of messages

```kotlin
fun main() {
    val logger = KotlinLogging.logger {}
    val times = 50
    val message = OutboundMessage("", "test_queue", MessageProperties.PERSISTENT_BASIC, "hello world")
    val connectionFactory = ConnectionFactory().apply { useNio() }
    try {
        connectionFactory.newConnection().use { connection ->
            runBlocking {
                connection.confirmChannel {
                    publish {
                        try {
                            supervisorScope {
                                val messages: List<Boolean> = (1..times)
                                    .map { message }
                                    .map { async(RabbitMqDispatchers.SingleThreadDispatcher) { publishWithConfirm(it) } }
                                    .map {
                                        try {
                                            it.await()
                                        } catch (e: Throwable) {
                                            logger.error(e) { "Failed to send the message" }
                                            false
                                        }
                                    }
                                val partitions: Pair<List<Boolean>, List<Boolean>> = messages.partition { it }
                                logger.info { "$times messages were sent, delivered: ${partitions.first.size}, not delivered: ${partitions.second.size}" }
                            }
                        } catch (e: Throwable) {
                            logger.error(e) { "Got an error while sending the batch of messages" }
                        }
                    }
                }
            }
        }
    } finally {
        RabbitMqDispatchers.SingleThreadDispatcher.close()
    }
}
```

TODO explain the code snippet

### Asynchronous message consuming with acknowledgement

Consume only n-messages:

```kotlin
connection.channel {
    consume(QUEUE_NAME, PREFETCH_COUNT) {
        (1..n).map { async { consumeMessageWithConfirm({ println(it) }) } }.awaitAll()
    }
}
```

### Transactional publishing and consuming

RabbitMQ and AMQP itself offer rather scarce support for transaction. When considering using transactions you should be
aware that:

* a transaction could only span one channel and one queue;
* `com.rabbitmq.client.Channel` is not thread-safe;
* channel can be either in confirm mode or in transaction mode at a time;
* transactions cannot be nested into each other;

The library provides a convenient way to perform transactional publishing and receiving based on `transaction` extension
function. This function commits a transaction upon normal execution of the block and rolls it back if
a `RuntimeException` occurs. Exceptions are always propagated further. Coroutines are not used for publishing though,
since there are no any asynchronous operations involved.

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
connection.channel {
    val message = RabbitMqMessage(MessageProperties.PERSISTENT_BASIC, "Hello world".toByteArray())
    coroutineScope {
        (1..10).map {
            async {
                rpc {
                    call(requestQueueName = "rpc_request", message = message)
                        .also { println("Reply: ${String(it.body)}") }
                }
            }
        }.awaitAll()
    }
}
```

TODO explain the code snippet
