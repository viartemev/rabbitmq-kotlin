# The White Rabbit 
[![Build Status](https://travis-ci.org/viartemev/the-white-rabbit.svg?branch=master)](https://travis-ci.org/viartemev/the-white-rabbit)
[ ![Download](https://api.bintray.com/packages/viartemev/Maven/the-white-rabbit/images/download.svg) ](https://bintray.com/viartemev/Maven/the-white-rabbit/_latestVersion)
[![Open Source Helpers](https://www.codetriage.com/viartemev/the-white-rabbit/badges/users.svg)](https://www.codetriage.com/viartemev/the-white-rabbit)
[![codecov](https://codecov.io/gh/viartemev/the-white-rabbit/branch/master/graph/badge.svg)](https://codecov.io/gh/viartemev/the-white-rabbit)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)


The White Rabbit is an asynchronous RabbitMQ (AMQP) client based on Kotlin coroutines  
### [Benchmarks](https://github.com/viartemev/the-white-rabbit/issues/88#issuecomment-470461937)
### Adding to project:
##### Gradle:
```
repositories {
    jcenter()
}

compile 'com.viartemev:the-white-rabbit:$version'
```
##### Maven:
```
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

### Usage:
##### - Async message publishing with confirmation: 
```kotlin
connection.confirmChannel {
     publish {
        coroutineScope {
            (1..n).map { asyncPublishWithConfirm(createMessage("Hello #$it")) }.awaitAll()
        }
    }
}
```
or
```kotlin
connection.confirmChannel {
    publish {
        val messages = (1..n).map { createMessage("Hello #$it") }
        asyncPublishWithConfirm(messages).awaitAll()
    }
}
```

##### - Async message consuming with acknowledge: 
Consume only n-messages:
```kotlin
connection.channel {
    consume(QUEUE_NAME, PREFETCH_COUNT) {
        (1..n).map { asyncConsumeWithConfirm({ println(it) }) }.awaitAll()
    }
}
```

##### - Async exchange declaration:
```kotlin
channel.declareExchange(ExchangeSpecification(EXCHANGE_NAME))
```
##### - Async queue declaration:
```kotlin
channel.declareQueue(QueueSpecification(QUEUE_NAME))
```
##### - Async queue binding to an exchange:
```kotlin
channel.bindQueue(BindQueueSpecification(EXCHANGE_NAME, QUEUE_NAME))
```
