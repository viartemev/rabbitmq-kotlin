package io.github.viartemev.rabbitmq.publisher

import com.rabbitmq.client.ConfirmListener
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private val logger = KotlinLogging.logger {}

/**
 * AckListener is an internal class that implements the [ConfirmListener] interface.
 * It is responsible for handling acknowledgment and negative acknowledgment events from the message broker.
 *
 * @property continuations A ConcurrentHashMap that stores the Continuation objects associated with delivery tags.
 * @property lowerBoundOfMultiple An AtomicLong that represents the lower bound of multiple acknowledgments.
 */
internal class AckListener(private val continuations: ConcurrentHashMap<Long, Continuation<Boolean>>) : ConfirmListener {

    private val lowerBoundOfMultiple = AtomicLong(1)

    /**
     * This method is responsible for handling an acknowledgment (ack) from the message broker.
     * It overrides the parent handle method and sets the parameters for acknowledgment.
     *
     * @param deliveryTag The delivery tag of the message to acknowledge.
     * @param multiple    Specifies whether to acknowledge multiple messages or not.
     */
    override fun handleAck(deliveryTag: Long, multiple: Boolean) {
        handle(deliveryTag, multiple, true)
    }

    /**
     * Handles the nack message acknowledgement for a specific delivery tag.
     *
     * @param deliveryTag The delivery tag of the message to be acknowledged.
     * @param multiple Indicates whether to acknowledge multiple messages with the same delivery tag.
     */
    override fun handleNack(deliveryTag: Long, multiple: Boolean) {
        handle(deliveryTag, multiple, false)
    }

    /**
     * Handles the delivery of a message.
     *
     * @param deliveryTag The delivery tag of the message.
     * @param multiple    Indicates whether multiple messages are being acknowledged.
     * @param ack         Indicates whether the message is acknowledged positively or negatively.
     */
    private fun handle(deliveryTag: Long, multiple: Boolean, ack: Boolean) {
        logger.debug { "deliveryTag = [$deliveryTag], multiple = [$multiple], positive = [$ack]" }
        val lowerBound = lowerBoundOfMultiple.get()
        if (multiple) {
            for (tag in lowerBound..deliveryTag) {
                continuations.remove(tag)?.resume(ack)
            }
            lowerBoundOfMultiple.compareAndSet(lowerBound, deliveryTag)
        } else {
            continuations.remove(deliveryTag)?.resume(ack)
            if (deliveryTag == lowerBound + 1) {
                lowerBoundOfMultiple.compareAndSet(lowerBound, deliveryTag)
            }
        }
    }
}
