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


package com.rabbitmq.client3.test.server;

import com.rabbitmq.client3.BlockedListener;
import com.rabbitmq.client3.Channel;
import com.rabbitmq.client3.Connection;
import com.rabbitmq.client3.ConnectionFactory;
import com.rabbitmq.client3.MessageProperties;
import com.rabbitmq.client3.test.BrokerTestCase;
import com.rabbitmq.tools.Host;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BlockedConnection extends BrokerTestCase {
    protected void releaseResources() throws IOException {
        try {
            unblock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void testBlock() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        Connection connection = connection(latch);
        block();
        publish(connection);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    public void testInitialBlock() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        block();
        Connection connection = connection(latch);
        publish(connection);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    private void block() throws IOException, InterruptedException {
        Host.rabbitmqctl("set_vm_memory_high_watermark 0.000000001");
        setResourceAlarm("disk");
    }

    private void unblock() throws IOException, InterruptedException {
        Host.rabbitmqctl("set_vm_memory_high_watermark 0.4");
        clearResourceAlarm("disk");
    }

    private Connection connection(final CountDownLatch latch) throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        Connection connection = factory.newConnection();
        connection.addBlockedListener(new BlockedListener() {
            public void handleBlocked(String reason) throws IOException {
                try {
                    unblock();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            public void handleUnblocked() throws IOException {
                latch.countDown();
            }
        });
        return connection;
    }

    private void publish(Connection connection) throws IOException {
        Channel ch = connection.createChannel();
        ch.basicPublish("", "", MessageProperties.BASIC, "".getBytes());
    }
}
