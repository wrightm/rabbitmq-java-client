//   The contents of this file are subject to the Mozilla Public License
//   Version 1.1 (the "License"); you may not use this file except in
//   compliance with the License. You may obtain a copy of the License at
//   http://www.mozilla.org/MPL/
//
//   Software distributed under the License is distributed on an "AS IS"
//   basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//   License for the specific language governing rights and limitations
//   under the License.
//
//   The Original Code is RabbitMQ.
//
//   The Initial Developers of the Original Code are LShift Ltd,
//   Cohesive Financial Technologies LLC, and Rabbit Technologies Ltd.
//
//   Portions created before 22-Nov-2008 00:00:00 GMT by LShift Ltd,
//   Cohesive Financial Technologies LLC, or Rabbit Technologies Ltd
//   are Copyright (C) 2007-2008 LShift Ltd, Cohesive Financial
//   Technologies LLC, and Rabbit Technologies Ltd.
//
//   Portions created by LShift Ltd are Copyright (C) 2007-2010 LShift
//   Ltd. Portions created by Cohesive Financial Technologies LLC are
//   Copyright (C) 2007-2010 Cohesive Financial Technologies
//   LLC. Portions created by Rabbit Technologies Ltd are Copyright
//   (C) 2007-2010 Rabbit Technologies Ltd.
//
//   All Rights Reserved.
//
//   Contributor(s): ______________________________________.
//
package com.rabbitmq.client;

import java.io.IOException;
import java.util.Map;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Exchange;
import com.rabbitmq.client.AMQP.Queue;
import com.rabbitmq.client.AMQP.Tx;
import com.rabbitmq.client.AMQP.Basic;
import com.rabbitmq.client.AMQP.Channel.FlowOk;

/**
 * Public API: Interface to an AMQ channel. See the <a href="http://www.amqp.org/">spec</a> for details.
 *
 * <p>
 * To open a channel,
 * <pre>
 * {@link Connection} conn = ...;
 * {@link Channel} channel = conn.{@link Connection#createChannel createChannel}();
 * </pre>
 * <p>
 * Public API:
 * <ul>
 *  <li> getChannelNumber
 *  <li> close
 * </ul>
 * <p>
 *
 * While a Channel can be used by multiple threads, it's important to ensure
 * that only one thread executes a command at once. Concurrent execution of
 * commands will likely cause an UnexpectedFrameError to be thrown.
 *
 */

public interface Channel extends ShutdownNotifier {
    /**
     * Retrieve this channel's channel number.
     * @return the channel number
     */
    int getChannelNumber();

    /**
     * Retrieve the connection which carries this channel.
     * @return the underlying {@link Connection}
     */
    Connection getConnection();

    /**
     * Close this channel with the {@link com.rabbitmq.client.AMQP#REPLY_SUCCESS} close code
     * and message 'OK'.
     *
     * @throws java.io.IOException if an error is encountered
     */
    void close() throws IOException;

    /**
     * Close this channel.
     *
     * @param closeCode the close code (See under "Reply Codes" in the AMQP specification)
     * @param closeMessage a message indicating the reason for closing the connection
     * @throws java.io.IOException if an error is encountered
     */
    void close(int closeCode, String closeMessage) throws IOException;

    /**
     * Set flow on the channel
     *
     * @param active if true, the server is asked to start sending. If false, the server is asked to stop sending.
     * @throws IOException
     */
    FlowOk flow(boolean active) throws IOException;

    /**
     * Return the current Channel.Flow settings.
     */
    FlowOk getFlow();

    /**
     * Abort this channel with the {@link com.rabbitmq.client.AMQP#REPLY_SUCCESS} close code
     * and message 'OK'.
     *
     * Forces the channel to close and waits for the close operation to complete.
     * Any encountered exceptions in the close operation are silently discarded.
     */
    void abort() throws IOException;

    /**
     * Abort this channel.
     *
     * Forces the channel to close and waits for the close operation to complete.
     * Any encountered exceptions in the close operation are silently discarded.
     */
    void abort(int closeCode, String closeMessage) throws IOException;

    /**
     * Return the current {@link ReturnListener}.
     * @return an interface to the current return listener
     */
    ReturnListener getReturnListener();

    /**
     * Set the current {@link ReturnListener}.
     * @param listener the listener to use, or null indicating "don't use one".
     */
    void setReturnListener(ReturnListener listener);

    /**
     * Return the current {@link FlowListener}.
     * @return an interface to the current flow listener.
     */
    FlowListener getFlowListener();

    /**
     * Set the current {@link FlowListener}.
     * @param listener the listener to use, or null indicating "don't use one".
     */
    void setFlowListener(FlowListener listener);

    /**
     * Get the current default consumer. @see setDefaultConsumer for rationale.
     * @return an interface to the current default consumer.
     */
    Consumer getDefaultConsumer();

    /**
     * Set the current default consumer.
     *
     * Under certain circumstances it is possible for a channel to receive a
     * message delivery which does not match any consumer which is currently
     * set up via basicConsume(). This will occur after the following sequence
     * of events:
     *
     * ctag = basicConsume(queue, consumer); // i.e. with explicit acks
     * // some deliveries take place but are not acked
     * basicCancel(ctag);
     * basicRecover(false);
     *
     * Since requeue is specified to be false in the basicRecover, the spec
     * states that the message must be redelivered to "the original recipient"
     * - i.e. the same channel / consumer-tag. But the consumer is no longer
     * active.
     *
     * In these circumstances, you can register a default consumer to handle
     * such deliveries. If no default consumer is registered an
     * IllegalStateException will be thrown when such a delivery arrives.
     *
     * Most people will not need to use this.
     *
     * @param consumer the consumer to use, or null indicating "don't use one".
     */
    void setDefaultConsumer(Consumer consumer);

    /**
     * Request specific "quality of service" settings.
     *
     * These settings impose limits on the amount of data the server
     * will deliver to consumers before requiring the receipt of
     * acknowledgements.
     * Thus they provide a means of consumer-initiated flow control.
     * @see com.rabbitmq.client.AMQP.Basic.Qos
     * @param prefetchSize maximum amount of content (measured in
     * octets) that the server will deliver, 0 if unlimited
     * @param prefetchCount maximum number of messages that the server
     * will deliver, 0 if unlimited
     * @param global true if the settings should be applied to the
     * entire connection rather than just the current channel
     * @throws java.io.IOException if an error is encountered
     */
    void basicQos(int prefetchSize, int prefetchCount, boolean global) throws IOException;

    /**
     * Request a specific prefetchCount "quality of service" settings
     * for this channel.
     *
     * @see #basicQos(int, int, boolean)
     * @param prefetchCount maximum number of messages that the server
     * will deliver, 0 if unlimited
     * @throws java.io.IOException if an error is encountered
     */
    void basicQos(int prefetchCount) throws IOException;

    /**
     * Publish a message with both "mandatory" and "immediate" flags set to false
     * @see com.rabbitmq.client.AMQP.Basic.Publish
     * @param exchange the exchange to publish the message to
     * @param routingKey the routing key
     * @param props other properties for the message - routing headers etc
     * @param body the message body
     * @throws java.io.IOException if an error is encountered
     */
    void basicPublish(String exchange, String routingKey, BasicProperties props, byte[] body) throws IOException;

    /**
     * Publish a message
     * @see com.rabbitmq.client.AMQP.Basic.Publish
     * @param exchange the exchange to publish the message to
     * @param routingKey the routing key
     * @param mandatory true if we are requesting a mandatory publish
     * @param immediate true if we are requesting an immediate publish
     * @param props other properties for the message - routing headers etc
     * @param body the message body
     * @throws java.io.IOException if an error is encountered
     */
    void basicPublish(String exchange, String routingKey, boolean mandatory, boolean immediate, BasicProperties props, byte[] body)
            throws IOException;

    /**
     * Actively declare a non-autodelete, non-durable exchange with no extra arguments
     * @see com.rabbitmq.client.AMQP.Exchange.Declare
     * @see com.rabbitmq.client.AMQP.Exchange.DeclareOk
     * @param exchange the name of the exchange
     * @param type the exchange type
     * @return a declaration-confirm method to indicate the exchange was successfully declared
     * @throws java.io.IOException if an error is encountered
     */
    Exchange.DeclareOk exchangeDeclare(String exchange, String type) throws IOException;

    /**
     * Actively declare a non-autodelete exchange with no extra arguments
     * @see com.rabbitmq.client.AMQP.Exchange.Declare
     * @see com.rabbitmq.client.AMQP.Exchange.DeclareOk
     * @param exchange the name of the exchange
     * @param type the exchange type
     * @param durable true if we are declaring a durable exchange (the exchange will survive a server restart)
     * @throws java.io.IOException if an error is encountered
     * @return a declaration-confirm method to indicate the exchange was successfully declared
     */
    Exchange.DeclareOk exchangeDeclare(String exchange, String type, boolean durable) throws IOException;

    /**
     * Declare an exchange, via an interface that allows the complete set of arguments.
     * @see com.rabbitmq.client.AMQP.Exchange.Declare
     * @see com.rabbitmq.client.AMQP.Exchange.DeclareOk
     * @param exchange the name of the exchange
     * @param type the exchange type
     * @param durable true if we are declaring a durable exchange (the exchange will survive a server restart)
     * @param autoDelete true if the server should delete the exchange when it is no longer in use
     * @param arguments other properties (construction arguments) for the exchange
     * @return a declaration-confirm method to indicate the exchange was successfully declared
     * @throws java.io.IOException if an error is encountered
     */
    Exchange.DeclareOk exchangeDeclare(String exchange, String type, boolean durable, boolean autoDelete,
                                       Map<String, Object> arguments) throws IOException;

    /**
     * Declare an exchange passively; that is, check if the named exchange exists.
     * @param name check the existence of an exchange named this
     * @throws IOException the server will raise a 404 channel exception if the named exchange does not exist.
     */
    Exchange.DeclareOk exchangeDeclarePassive(String name) throws IOException;

    /**
     * Delete an exchange
     * @see com.rabbitmq.client.AMQP.Exchange.Delete
     * @see com.rabbitmq.client.AMQP.Exchange.DeleteOk
     * @param exchange the name of the exchange
     * @param ifUnused true to indicate that the exchange is only to be deleted if it is unused
     * @return a deletion-confirm method to indicate the exchange was successfully deleted
     * @throws java.io.IOException if an error is encountered
     */
    Exchange.DeleteOk exchangeDelete(String exchange, boolean ifUnused) throws IOException;

    /**
     * Delete an exchange, without regard for whether it is in use or not
     * @see com.rabbitmq.client.AMQP.Exchange.Delete
     * @see com.rabbitmq.client.AMQP.Exchange.DeleteOk
     * @param exchange the name of the exchange
     * @return a deletion-confirm method to indicate the exchange was successfully deleted
     * @throws java.io.IOException if an error is encountered
     */
    Exchange.DeleteOk exchangeDelete(String exchange) throws IOException;

    /**
     * Actively declare a server-named exclusive, autodelete, non-durable queue.
     * The name of the new queue is held in the "queue" field of the {@link com.rabbitmq.client.AMQP.Queue.DeclareOk} result.
     * @see com.rabbitmq.client.AMQP.Queue.Declare
     * @see com.rabbitmq.client.AMQP.Queue.DeclareOk
     * @return a declaration-confirm method to indicate the queue was successfully declared
     * @throws java.io.IOException if an error is encountered
     */
    Queue.DeclareOk queueDeclare() throws IOException;

    /**
     * Declare a queue
     * @see com.rabbitmq.client.AMQP.Queue.Declare
     * @see com.rabbitmq.client.AMQP.Queue.DeclareOk
     * @param queue the name of the queue
     * @param durable true if we are declaring a durable queue (the queue will survive a server restart)
     * @param exclusive true if we are declaring an exclusive queue (restricted to this connection)
     * @param autoDelete true if we are declaring an autodelete queue (server will delete it when no longer in use)
     * @param arguments other properties (construction arguments) for the queue
     * @return a declaration-confirm method to indicate the queue was successfully declared
     * @throws java.io.IOException if an error is encountered
     */
    Queue.DeclareOk queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete,
                                 Map<String, Object> arguments) throws IOException;

    /**
     * Declare a queue passively; i.e., check if it exists.  In AMQP
     * 0-9-1, all arguments aside from nowait are ignored; and sending
     * nowait makes this method a no-op, so we default it to false.
     * @see com.rabbitmq.client.AMQP.Queue.Declare
     * @see com.rabbitmq.client.AMQP.Queue.DeclareOk
     * @param queue the name of the queue
     * @return a declaration-confirm method to indicate the queue exists
     * @throws java.io.IOException if an error is encountered,
     * including if the queue does not exist and if the queue is
     * exclusively owned by another connection.
     */
    Queue.DeclareOk queueDeclarePassive(String queue) throws IOException;

    /**
     * Delete a queue, without regard for whether it is in use or has messages on it
     * @see com.rabbitmq.client.AMQP.Queue.Delete
     * @see com.rabbitmq.client.AMQP.Queue.DeleteOk
     * @param queue the name of the queue
     * @return a deletion-confirm method to indicate the queue was successfully deleted
     * @throws java.io.IOException if an error is encountered
     */
    Queue.DeleteOk queueDelete(String queue) throws IOException;

    /**
     * Delete a queue
     * @see com.rabbitmq.client.AMQP.Queue.Delete
     * @see com.rabbitmq.client.AMQP.Queue.DeleteOk
     * @param queue the name of the queue
     * @param ifUnused true if the queue should be deleted only if not in use
     * @param ifEmpty true if the queue should be deleted only if empty
     * @return a deletion-confirm method to indicate the queue was successfully deleted
     * @throws java.io.IOException if an error is encountered
     */
    Queue.DeleteOk queueDelete(String queue, boolean ifUnused, boolean ifEmpty) throws IOException;

    /**
     * Bind a queue to an exchange, with no extra arguments.
     * @see com.rabbitmq.client.AMQP.Queue.Bind
     * @see com.rabbitmq.client.AMQP.Queue.BindOk
     * @param queue the name of the queue
     * @param exchange the name of the exchange
     * @param routingKey the routine key to use for the binding
     * @return a binding-confirm method if the binding was successfully created
     * @throws java.io.IOException if an error is encountered
     */
    Queue.BindOk queueBind(String queue, String exchange, String routingKey) throws IOException;

    /**
     * Bind a queue to an exchange.
     * @see com.rabbitmq.client.AMQP.Queue.Bind
     * @see com.rabbitmq.client.AMQP.Queue.BindOk
     * @param queue the name of the queue
     * @param exchange the name of the exchange
     * @param routingKey the routine key to use for the binding
     * @param arguments other properties (binding parameters)
     * @return a binding-confirm method if the binding was successfully created
     * @throws java.io.IOException if an error is encountered
     */
    Queue.BindOk queueBind(String queue, String exchange, String routingKey, Map<String, Object> arguments) throws IOException;

    /**
     * Unbinds a queue from an exchange, with no extra arguments.
     * @see com.rabbitmq.client.AMQP.Queue.Unbind
     * @see com.rabbitmq.client.AMQP.Queue.UnbindOk
     * @param queue the name of the queue
     * @param exchange the name of the exchange
     * @param routingKey the routine key to use for the binding
     * @return an unbinding-confirm method if the binding was successfully deleted
     * @throws java.io.IOException if an error is encountered
     */
    Queue.UnbindOk queueUnbind(String queue, String exchange, String routingKey) throws IOException;

    /**
     * Unbind a queue from an exchange.
     * @see com.rabbitmq.client.AMQP.Queue.Unbind
     * @see com.rabbitmq.client.AMQP.Queue.UnbindOk
     * @param queue the name of the queue
     * @param exchange the name of the exchange
     * @param routingKey the routine key to use for the binding
     * @param arguments other properties (binding parameters)
     * @return an unbinding-confirm method if the binding was successfully deleted
     * @throws java.io.IOException if an error is encountered
     */
    Queue.UnbindOk queueUnbind(String queue, String exchange, String routingKey, Map<String, Object> arguments) throws IOException;

    /**
     * Purges the contents of the given queue and awaits a completion.
     * @see com.rabbitmq.client.AMQP.Queue.Purge
     * @see com.rabbitmq.client.AMQP.Queue.PurgeOk
     * @param queue the name of the queue
     * @return a purge-confirm method if the purge was executed succesfully
     * @throws java.io.IOException if an error is encountered
     */
    Queue.PurgeOk queuePurge(String queue) throws IOException;

    /**
     * Purges the contents of the given queue.
     * @see com.rabbitmq.client.AMQP.Queue.Purge
     * @see com.rabbitmq.client.AMQP.Queue.PurgeOk
     * @param queue the name of the queue
     * @param nowait whether to await completion of the purge
     * @return a purge-confirm method if the purge was executed succesfully
     * @throws java.io.IOException if an error is encountered
     */
    Queue.PurgeOk queuePurge(String queue, boolean nowait) throws IOException;

    /**
     * Retrieve a message from a queue using {@link com.rabbitmq.client.AMQP.Basic.Get}
     * @see com.rabbitmq.client.AMQP.Basic.Get
     * @see com.rabbitmq.client.AMQP.Basic.GetOk
     * @see com.rabbitmq.client.AMQP.Basic.GetEmpty
     * @param queue the name of the queue
     * @param noAck true if no handshake is required
     * @return a {@link GetResponse} containing the retrieved message data
     * @throws java.io.IOException if an error is encountered
     */
    GetResponse basicGet(String queue, boolean noAck) throws IOException;

    /**
     * Acknowledge one or several received
     * messages. Supply the deliveryTag from the {@link com.rabbitmq.client.AMQP.Basic.GetOk}
     * or {@link com.rabbitmq.client.AMQP.Basic.Deliver} method
     * containing the received message being acknowledged.
     * @see com.rabbitmq.client.AMQP.Basic.Ack
     * @param deliveryTag the tag from the received {@link com.rabbitmq.client.AMQP.Basic.GetOk} or {@link com.rabbitmq.client.AMQP.Basic.Deliver}
     * @param multiple true if we are acknowledging multiple messages with the same delivery tag
     * @throws java.io.IOException if an error is encountered
     */
    void basicAck(long deliveryTag, boolean multiple) throws IOException;

    /**
     * Start a non-nolocal, non-exclusive consumer, with
     * explicit acknowledgements required and a server-generated consumerTag.
     * @param queue the name of the queue
     * @param callback an interface to the consumer object
     * @return the consumerTag generated by the server
     * @throws java.io.IOException if an error is encountered
     * @see com.rabbitmq.client.AMQP.Basic.Consume
     * @see com.rabbitmq.client.AMQP.Basic.ConsumeOk
     * @see #basicAck
     * @see #basicConsume(String,boolean, String,boolean,boolean, Map, Consumer)
     */
    String basicConsume(String queue, Consumer callback) throws IOException;

    /**
     * Start a non-nolocal, non-exclusive consumer, with
     * a server-generated consumerTag.
     * @param queue the name of the queue
     * @param noAck true if no handshake is required
     * @param callback an interface to the consumer object
     * @return the consumerTag generated by the server
     * @throws java.io.IOException if an error is encountered
     * @see com.rabbitmq.client.AMQP.Basic.Consume
     * @see com.rabbitmq.client.AMQP.Basic.ConsumeOk
     * @see #basicConsume(String,boolean, String,boolean,boolean, Map, Consumer)
     */
    String basicConsume(String queue, boolean noAck, Consumer callback) throws IOException;

    /**
     * Start a non-nolocal, non-exclusive consumer.
     * @param queue the name of the queue
     * @param noAck true if no handshake is required
     * @param consumerTag a client-generated consumer tag to establish context
     * @param callback an interface to the consumer object
     * @return the consumerTag associated with the new consumer
     * @throws java.io.IOException if an error is encountered
     * @see com.rabbitmq.client.AMQP.Basic.Consume
     * @see com.rabbitmq.client.AMQP.Basic.ConsumeOk
     * @see #basicConsume(String,boolean, String,boolean,boolean, Map, Consumer)
     */
    String basicConsume(String queue, boolean noAck, String consumerTag, Consumer callback) throws IOException;

    /**
     * Start a consumer. Calls the consumer's {@link Consumer#handleConsumeOk}
     * method before returning.
     * @param queue the name of the queue
     * @param noAck true if no handshake is required
     * @param consumerTag a client-generated consumer tag to establish context
     * @param noLocal flag set to true unless server local buffering is required
     * @param exclusive true if this is an exclusive consumer
     * @param callback an interface to the consumer object
     * @return the consumerTag associated with the new consumer
     * @throws java.io.IOException if an error is encountered
     * @see com.rabbitmq.client.AMQP.Basic.Consume
     * @see com.rabbitmq.client.AMQP.Basic.ConsumeOk
     */
    String basicConsume(String queue, boolean noAck, String consumerTag, boolean noLocal, boolean exclusive, Map<String, Object> filter, Consumer callback) throws IOException;

    /**
     * Cancel a consumer. Calls the consumer's {@link Consumer#handleCancelOk}
     * method before returning.
     * @param consumerTag a client- or server-generated consumer tag to establish context
     * @throws java.io.IOException if an error is encountered
     * @see com.rabbitmq.client.AMQP.Basic.Cancel
     * @see com.rabbitmq.client.AMQP.Basic.CancelOk
     */
    void basicCancel(String consumerTag) throws IOException;

    /**
     * Ask the broker to resend unacknowledged messages.  In 0-8
     * basic.recover is asynchronous; in 0-9-1 it is synchronous, and
     * the new, deprecated method basic.recover_async is asynchronous.
     * @param requeue If true, messages will be requeued and possibly
     * delivered to a different consumer. If false, messages will be
     * redelivered to the same consumer.
     */
    Basic.RecoverOk basicRecover(boolean requeue) throws IOException;

    /**
     * Ask the broker to resend unacknowledged messages.  In 0-8
     * basic.recover is asynchronous; in 0-9-1 it is synchronous, and
     * the new, deprecated method basic.recover_async is asynchronous
     * and deprecated.
     * @param requeue If true, messages will be requeued and possibly
     * delivered to a different consumer. If false, messages will be
     * redelivered to the same consumer.
     */
    @Deprecated
    void basicRecoverAsync(boolean requeue) throws IOException;

    /**
     * Enables TX mode on this channel.
     * @see com.rabbitmq.client.AMQP.Tx.Select
     * @see com.rabbitmq.client.AMQP.Tx.SelectOk
     * @return a transaction-selection method to indicate the transaction was successfully initiated
     * @throws java.io.IOException if an error is encountered
     */
    Tx.SelectOk txSelect() throws IOException;

    /**
     * Commits a TX transaction on this channel.
     * @see com.rabbitmq.client.AMQP.Tx.Commit
     * @see com.rabbitmq.client.AMQP.Tx.CommitOk
     * @return a transaction-commit method to indicate the transaction was successfully committed
     * @throws java.io.IOException if an error is encountered
     */
    Tx.CommitOk txCommit() throws IOException;

    /**
     * Rolls back a TX transaction on this channel.
     * @see com.rabbitmq.client.AMQP.Tx.Rollback
     * @see com.rabbitmq.client.AMQP.Tx.RollbackOk
     * @return a transaction-rollback method to indicate the transaction was successfully rolled back
     * @throws java.io.IOException if an error is encountered
     */
    Tx.RollbackOk txRollback() throws IOException;
}
