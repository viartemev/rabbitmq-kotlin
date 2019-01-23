# The White Rabbit
<p align="center">
    <img src ="https://github.com/viartemev/the-white-rabbit/blob/master/art/the-white-rabbit.png?raw=true"/>
</p>

The White Rabbit is a library for RabbitMQ based on Kotlin coroutines.

##### *IMPORTANT: This project is still under development, if you are planning to use it, please contact me.*

### Status
[![Build Status](https://travis-ci.org/viartemev/the-white-rabbit.svg?branch=master)](https://travis-ci.org/viartemev/the-white-rabbit)

### Usage:
- Start RabbitMQ: 
```docker
docker run -d --hostname my-rabbit --name some-rabbit -p 8080:15672 -p 5672:5672 rabbitmq:3-management
```

- Declare an exchange/queue:
```kotlin
ConnectionFactory().apply {
    host = "localhost"
    useNio()
}.newConnection().use { connection ->
    connection.createChannel().use { channel ->
        runBlocking {
            channel.apply {
                declareExchange(ExchangeSpecification("test_exchange"))
                declareQueue(QueueSpecification("test_queue"))
                bindQueue(BindQueueSpecification("test_queue", "test_exchange", "test_queue"))
            }
        }
    }
}
```
- Publish: 
```kotlin
val times = 10
val time = measureNanoTime {
    ConnectionFactory().apply {
        host = "localhost"
        useNio()
    }.newConnection().use { connection ->
        connection.createConfirmChannel().use { channel ->
            val publisher = channel.publisher()
            runBlocking {
                Queue.declareQueue(channel, QueueSpecification(QUEUE_NAME))
                val acks = (1..times).map {
                    async {
                        publisher.publishWithConfirm(createMessage("Hello #$it"))
                    }
                }.awaitAll()
                assertTrue { acks.all { true } }
            }
        }
    }
}
println("Time: $time")

fun createMessage(body: String) = OutboundMessage(EXCHANGE_NAME, QUEUE_NAME, MessageProperties.PERSISTENT_BASIC, body.toByteArray(charset("UTF-8")))
```

- Consume:
```kotlin
ConnectionFactory().apply {
    host = "localhost"
    useNio()
}.newConnection().use { connection ->
    connection.createChannel().use { channel ->
        val consumer = channel.consumer(QUEUE_NAME)
        runBlocking {
            channel.declareQueue(QueueSpecification(QUEUE_NAME))
            val consumer = channel.consumer(QUEUE_NAME)
            
            //consume 3 messages
            for (i in 1..3) consumer.consumeWithConfirm({ handleDelivery(it) })
            
            //infinite consuming
            consumer.consumeWithConfirm(parallelism = 3, handler = { handleDelivery(it) })
        }
    }
}

suspend fun handleDelivery(message: Delivery) {
    println("Got a message: ${String(message.body)}. Let's do some async work...")
    delay(100)
    println("Work is done")
}
```
