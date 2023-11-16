package io.github.viartemev.rabbitmq.utils

import com.rabbitmq.client.MessageProperties
import io.github.viartemev.rabbitmq.publisher.OutboundMessage

fun createMessage(exchange: String = "", queue: String = "test_queue", body: String) =
    OutboundMessage(exchange, queue, MessageProperties.PERSISTENT_BASIC, body)
