package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.consumer.ConfirmConsumer

fun Channel.consumer(queue: String) = ConfirmConsumer(this, queue)