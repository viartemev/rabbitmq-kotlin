package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import com.viartemev.thewhiterabbit.consumer.ConfirmConsumer
import com.viartemev.thewhiterabbit.rpc.RpcClient

/**
 * Creates a ConfirmConsumer for the given Channel, queue, and prefetchSize.
 *
 * @param queue The name of the queue to consume messages from.
 * @param prefetchSize The maximum number of messages to prefetch.
 * @return A ConfirmConsumer object.
 */
fun Channel.consumer(queue: String, prefetchSize: Int) = ConfirmConsumer(this, queue, prefetchSize)

/**
 * Suspends the execution and consumes messages from the channel.
 *
 * @param queue The name of the queue to consume messages from.
 * @param prefetchSize The maximum number of messages to prefetch.
 * @param block The code to be executed for each consumed message.
 *              It takes an instance of [ConfirmConsumer] as a receiver, allowing access to its methods and properties.
 *
 * @throws Exception if an error occurs during message consumption.
 */
suspend fun Channel.consume(queue: String, prefetchSize: Int = 0, block: suspend ConfirmConsumer.() -> Unit) =
    this.consumer(queue, prefetchSize).use { block(it) }


/**
 * Creates a new `RpcClient` instance associated with the given `Channel`.
 *
 * @param channel The channel to associate with the `RpcClient`.
 * @return An instance of `RpcClient`.
 */
fun Channel.rpcClient() = RpcClient(this)

/**
 * Executes a remote procedure call (RPC) by suspending the current coroutine until the RPC is completed.
 *
 * @param block A lambda that represents the RPC operation to be executed.
 *              The lambda takes an instance of [RpcClient] as its receiver and returns a [Delivery] object.
 *              The [RpcClient] provides methods to interact with the RPC server.
 * @throws IOException If an I/O error occurs during the RPC operation.
 * @throws RpcException If an error occurs during the RPC operation.
 */
suspend fun Channel.rpc(block: suspend RpcClient.() -> Delivery) = this.rpcClient().run { block() }
