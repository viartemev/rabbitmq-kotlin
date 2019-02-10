# The White Rabbit
The White Rabbit is an asynchronous RabbitMQ library based on Kotlin coroutines.

### Status
[![Build Status](https://travis-ci.org/viartemev/the-white-rabbit.svg?branch=master)](https://travis-ci.org/viartemev/the-white-rabbit)

### Usage:
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
Consume only n-messages:
```kotlin
val channel = connection.createConfirmChannel()
val consumer = channel.consumer(QUEUE_NAME)
for (i in 1..n) consumer.consumeWithConfirm({ println(it) })
```

##### - Async exchange declaration:
```kotlin
channel.declareExchange(ExchangeSpecification(EXCHANGE_NAME))
```
##### - Async queue declaration:
```kotlin
channel.declareQueue(QueueSpecification(QUEUE_NAME))
```
##### - Async queue bindging to an exchange:
```kotlin
channel.bindQueue(BindQueueSpecification(EXCHANGE_NAME, QUEUE_NAME))
```
