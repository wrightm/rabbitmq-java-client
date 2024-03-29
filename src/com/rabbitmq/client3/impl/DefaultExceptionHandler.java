//  The contents of this file are subject to the Mozilla Public License
//  Version 1.1 (the "License"); you may not use this file except in
//  compliance with the License. You may obtain a copy of the License
//  at http://www.mozilla.org/MPL/
//
//  Software distributed under the License is distributed on an "AS IS"
//  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
//  the License for the specific language governing rights and
//  limitations under the License.
//
//  The Original Code is RabbitMQ.
//
//  The Initial Developer of the Original Code is GoPivotal, Inc.
//  Copyright (c) 2007-2013 GoPivotal, Inc.  All rights reserved.
//

package com.rabbitmq.client3.impl;

import java.io.IOException;

import com.rabbitmq.client3.AMQP;
import com.rabbitmq.client3.AlreadyClosedException;
import com.rabbitmq.client3.Channel;
import com.rabbitmq.client3.Connection;
import com.rabbitmq.client3.Consumer;

/**
 * Default implementation of {@link ExceptionHandler} used by {@link AMQConnection}.
 */
public class DefaultExceptionHandler implements ExceptionHandler {
    public void handleUnexpectedConnectionDriverException(Connection conn, Throwable exception) {
        // TODO: Log this somewhere, just in case we have a bug like
        // 16272 where exceptions aren't being propagated properly
        // again.

        //System.err.println("DefaultExceptionHandler:");
        //exception.printStackTrace();
    }

    public void handleReturnListenerException(Channel channel, Throwable exception) {
        handleChannelKiller(channel, exception, "ReturnListener.handleReturn");
    }

    public void handleFlowListenerException(Channel channel, Throwable exception) {
        handleChannelKiller(channel, exception, "FlowListener.handleFlow");
    }

    public void handleConfirmListenerException(Channel channel, Throwable exception) {
        handleChannelKiller(channel, exception, "ConfirmListener.handle{N,A}ck");
    }

    public void handleBlockedListenerException(Connection connection, Throwable exception) {
        handleConnectionKiller(connection, exception, "BlockedListener");
    }

    public void handleConsumerException(Channel channel, Throwable exception,
                                        Consumer consumer, String consumerTag,
                                        String methodName)
    {
        handleChannelKiller(channel, exception, "Consumer " + consumer
                                              + " (" + consumerTag + ")"
                                              + " method " + methodName
                                              + " for channel " + channel);
    }

    protected void handleChannelKiller(Channel channel, Throwable exception, String what) {
        // TODO: log the exception
        System.err.println("DefaultExceptionHandler: " + what + " threw an exception for channel "
                + channel + ":");
        exception.printStackTrace();
        try {
            channel.close(AMQP.REPLY_SUCCESS, "Closed due to exception from " + what);
        } catch (AlreadyClosedException ace) {
            // noop
        } catch (IOException ioe) {
            // TODO: log the failure
            System.err.println("Failure during close of channel " + channel + " after " + exception
                    + ":");
            ioe.printStackTrace();
            channel.getConnection().abort(AMQP.INTERNAL_ERROR, "Internal error closing channel for " + what);
        }
    }

    protected void handleConnectionKiller(Connection connection, Throwable exception, String what) {
        // TODO: log the exception
        System.err.println("DefaultExceptionHandler: " + what + " threw an exception for connection "
                + connection + ":");
        exception.printStackTrace();
        try {
            connection.close(AMQP.REPLY_SUCCESS, "Closed due to exception from " + what);
        } catch (AlreadyClosedException ace) {
            // noop
        } catch (IOException ioe) {
            // TODO: log the failure
            System.err.println("Failure during close of connection " + connection + " after " + exception
                    + ":");
            ioe.printStackTrace();
            connection.abort(AMQP.INTERNAL_ERROR, "Internal error closing connection for " + what);
        }
    }
}
