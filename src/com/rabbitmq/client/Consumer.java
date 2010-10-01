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

/**
 * Interface for application callback objects to receive notifications and messages from
 * a queue by subscription.
 * Most consumers will subclass {@link DefaultConsumer}.
 *
 * Note: all methods of this interface are invoked in a dispatch
 * thread which is separate from the {@link Connection}'s thread. This
 * allows <code>Consumers</code> to call {@link Channel} or {@link
 * Connection} methods without causing a deadlock.
 *
 * The dispatch thread is shared by multiple <code>Consumers</code>.
 * <code>Consumers</code> should avoid executing long-running code
 * inside the dispatch thread because this may hold up dispatch of
 * messages to other <code>Consumers</code>.
 *
 * @see Channel#basicConsume
 * @see Channel#basicCancel
 */
public interface Consumer {
    /**
     * Called when the consumer is first registered by a call to {@link Channel#basicConsume}.
     *
     * Note: if the consumer was registered with an empty string for its consumerTag, the server will have
     * autogenerated and replied with a fresh consumerTag.
     * @param consumerTag the defined consumerTag (either client- or server-generated)
     */
    void handleConsumeOk(String consumerTag);

    /**
     * Called when the consumer is deregistered by a call to {@link Channel#basicCancel}.
     * @param consumerTag the defined consumerTag (either client- or server-generated)
     */
    void handleCancelOk(String consumerTag);

    /**
     * Called to the consumer that either the channel or the undelying connection has been shut down.
     * @param consumerTag the defined consumerTag (either client- or server-generated)
     * @param sig an exception object encapsulating the reason for shutdown
     */
    void handleShutdownSignal(String consumerTag, ShutdownSignalException sig);

    /**
     * Called to notify the consumer that we've received a basic.recover-ok
     * in reply to a basic.recover some other thread sent. All messages
     * received before this is invoked that haven't been ack'ed will be
     * redelivered. All messages received afterwards won't be.
     *
     * This method exists since all the Consumer callbacks are invoked by the
     * connection main loop thread - so it's sometimes useful to allow that
     * thread to know that the recover-ok has been received, rather than the
     * thread which invoked basicRecover().
     */
    void handleRecoverOk();

    /**
     * Called when a delivery appears for this consumer.
     * @param consumerTag the defined consumerTag (either client- or server-generated)
     * @param envelope packaging data for the message
     * @param properties content header data for the message
     * @param body the message body (opaque client-specific byte array)
     * @throws IOException if the consumer hits an I/O error while processing the message
     */
    void handleDelivery(String consumerTag,
                        Envelope envelope,
                        AMQP.BasicProperties properties,
                        byte[] body)
        throws IOException;
}
