# The White Rabbit

##### *IMPORTANT: This project is still under development, if you are planning to use it, please contact me.*

### Status
[![Build Status](https://travis-ci.org/viartemev/the-white-rabbit.svg?branch=master)](https://travis-ci.org/viartemev/the-white-rabbit)

### Usage:
##### - Async exchange declaration:
```kotlin
channel.declareExchange(ExchangeSpecification(EXCHANGE_NAME))
```
##### - Async queue declaration:
```kotlin
channel.declareQueue(QueueSpecification(QUEUE_NAME))
```
##### - Async message publishing with confirmation: 
```kotlin
val channel = connection.createConfirmChannel()
val publisher = channel.publisher()
val messages = (1..times).map { createMessage("Hello #$it") }
publisher.publishWithConfirm(messages).awaitAll()
```
or
```kotlin
val channel = connection.createConfirmChannel()
val publisher = channel.publisher()
coroutineScope {
    (1..times).map {
        async { publisher.publishWithConfirm(createMessage("Hello #$it")) }
    }.awaitAll()
}
```

##### - Async message consuming with acknowledge: 
Consume only 3 messages:
```kotlin
val channel = connection.createConfirmChannel()
val consumer = channel.consumer(QUEUE_NAME)
for (i in 1..3) consumer.consumeWithConfirm({ println(it) })
```
or Infinite consuming
```kotlin
val channel = connection.createConfirmChannel()
val consumer = channel.consumer(QUEUE_NAME)
consumer.consumeWithConfirm(parallelism = 3, handler = { println(it) })
```
