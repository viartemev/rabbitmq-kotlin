# The White Rabbit

The Whit Rabbit is a library for RabbitMQ based on Kotlin coroutines.

### Usage:
- Start RabbitMQ: 
```docker
docker run -d --hostname my-rabbit --name some-rabbit -p 8080:15672 -p 5672:5672 rabbitmq:3-management
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
            runBlocking {
                Queue.declareQueue(channel, QueueSpecification(QUEUE_NAME))
                val sender = ConfirmPublisher(channel)
                val acks = (1..times).map {
                    async {
                        sender.publish(createMessage("Hello #$it"))
                    }
                }.awaitAll()
                assertTrue { acks.all { true } }
            }
        }
    }
}
println("Time: $time")

```

- Consume:
```kotlin
ConnectionFactory().apply {
    host = "localhost"
    useNio()
}.newConnection().use { connection ->
    connection.createChannel().use { channel ->
        runBlocking {
            Queue.declareQueue(channel, QueueSpecification(QUEUE_NAME))
            val consumer = Consumer(channel, QUEUE_NAME, Dispatchers.IO)
            for (i in 1..3) {
                launch {
                    consumer.consume {
                        println("Got a message: ${String(it.body)}. Let's do some async work...")
                        delay(100)
                        println("Work is done")
                    }
                }
            }
        }
    }
}
```