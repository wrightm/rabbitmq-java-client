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
//  The Initial Developer of the Original Code is VMware, Inc.
//  Copyright (c) 2007-2011 VMware, Inc.  All rights reserved.
//


package com.rabbitmq.client.test.functional;

import com.rabbitmq.client.test.BrokerTestCase;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AckListener;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class Confirm extends BrokerTestCase
{
    final static int NUM_MESSAGES = 1000;
    private static final String TTL_ARG = "x-message-ttl";
    private SortedSet<Long> ackSet;

    @Override
    protected void setUp() throws IOException {
        super.setUp();
        ackSet = Collections.synchronizedSortedSet(new TreeSet<Long>());
        channel.setAckListener(new AckListener() {
                public void handleAck(long seqNo,
                                      boolean multiple) {
                    Confirm.this.handleAck(seqNo, multiple);
                }
            });
        channel.confirmSelect();
        channel.queueDeclare("confirm-test", true, true, false, null);
        channel.basicConsume("confirm-test", true,
                             new DefaultConsumer(channel));
        channel.queueDeclare("confirm-test-nondurable", false, true,
                             false, null);
        channel.basicConsume("confirm-test-nondurable", true,
                             new DefaultConsumer(channel));
        channel.queueDeclare("confirm-test-noconsumer", true,
                             true, false, null);
        channel.queueDeclare("confirm-test-2", true, true, false, null);
        channel.basicConsume("confirm-test-2", true,
                             new DefaultConsumer(channel));
        Map<String, Object> argMap =
            Collections.singletonMap(TTL_ARG, (Object)1);
        channel.queueDeclare("confirm-ttl", true, true, false, argMap);
        channel.queueBind("confirm-test", "amq.direct",
                          "confirm-multiple-queues");
        channel.queueBind("confirm-test-2", "amq.direct",
                          "confirm-multiple-queues");
    }

    public void testConfirmTransient()
        throws IOException, InterruptedException {
        confirmTest("", "confirm-test", false, false, false);
    }

    public void testConfirmPersistentSimple()
        throws IOException, InterruptedException
    {
        confirmTest("", "confirm-test", true, false, false);
    }

    public void testConfirmNonDurable()
        throws IOException, InterruptedException
    {
        confirmTest("", "confirm-test-nondurable", true, false, false);
    }

    public void testConfirmPersistentImmediate()
        throws IOException, InterruptedException
    {
        confirmTest("", "confirm-test", true, false, true);
    }

    public void testConfirmPersistentImmediateNoConsumer()
        throws IOException, InterruptedException
    {
        confirmTest("", "confirm-test-noconsumer", true, false, true);
    }

    public void testConfirmPersistentMandatory()
        throws IOException, InterruptedException
    {
        confirmTest("", "confirm-test", true, true, false);
    }

    public void testConfirmPersistentMandatoryReturn()
        throws IOException, InterruptedException
    {
        confirmTest("", "confirm-test-doesnotexist", true, true, false);
    }

    public void testConfirmMultipleQueues()
        throws IOException, InterruptedException
    {
        confirmTest("amq.direct", "confirm-multiple-queues",
                    true, false, false);
    }

    /* For testConfirmQueueDelete and testConfirmQueuePurge to be
     * relevant, the msg_store must not write the messages to disk
     * (thus causing a confirm).  I'd manually comment out the line in
     * internal_sync that notifies the clients. */

    public void testConfirmQueueDelete()
        throws IOException, InterruptedException
    {
        publishN("","confirm-test-noconsumer", true, false, false);

        channel.queueDelete("confirm-test-noconsumer");

        waitAcks();
    }

    public void testConfirmQueuePurge()
        throws IOException, InterruptedException
    {
        publishN("", "confirm-test-noconsumer", true, false, false);

        channel.queuePurge("confirm-test-noconsumer");

        waitAcks();
    }

    public void testConfirmBasicReject()
        throws IOException, InterruptedException
    {
        basicRejectCommon(false);

        waitAcks();
    }

    public void testConfirmQueueTTL()
        throws IOException, InterruptedException
    {
        publishN("", "confirm-ttl", true, false, false);

        waitAcks();
    }

    public void testConfirmBasicRejectRequeue()
        throws IOException, InterruptedException
    {
        basicRejectCommon(true);

        /* wait confirms to go through the broker */
        Thread.sleep(1000);

        channel.basicConsume("confirm-test-noconsumer", true,
                             new DefaultConsumer(channel));

        waitAcks();
    }

    public void testConfirmBasicRecover()
        throws IOException, InterruptedException
    {
        publishN("", "confirm-test-noconsumer", true, false, false);

        for (long i = 0; i < NUM_MESSAGES; i++) {
            GetResponse resp =
                channel.basicGet("confirm-test-noconsumer", false);
            long dtag = resp.getEnvelope().getDeliveryTag();
            // not acking
        }

        channel.basicRecover(true);

        Thread.sleep(1000);

        channel.basicConsume("confirm-test-noconsumer", true,
                             new DefaultConsumer(channel));

        waitAcks();
    }

    public void testConfirmSelect()
        throws IOException
    {
        try {
            Channel ch = connection.createChannel();
            ch.confirmSelect();
            ch.txSelect();
            fail();
        } catch (IOException ioe) {
            checkShutdownSignal(AMQP.PRECONDITION_FAILED, ioe);
        }
        try {
            Channel ch = connection.createChannel();
            ch.txSelect();
            ch.confirmSelect();
            fail();
        } catch (IOException ioe) {
            checkShutdownSignal(AMQP.PRECONDITION_FAILED, ioe);
        }
        Channel ch = connection.createChannel();
        ch.confirmSelect();
        ch.confirmSelect();
    }

    /* Publish NUM_MESSAGES persistent messages and wait for
     * confirmations. */
    public void confirmTest(String exchange, String queueName,
                            boolean persistent, boolean mandatory,
                            boolean immediate)
        throws IOException, InterruptedException
    {
        publishN(exchange, queueName, persistent, mandatory, immediate);

        waitAcks();
    }

    private void publishN(String exchangeName, String queueName,
                          boolean persistent, boolean mandatory,
                          boolean immediate)
        throws IOException
    {
        for (long i = 0; i < NUM_MESSAGES; i++) {
            ackSet.add(channel.getNextPublishSeqNo());
            publish(exchangeName, queueName, persistent, mandatory, immediate);
        }
    }


    private void publish(String exchangeName, String queueName,
                         boolean persistent, boolean mandatory,
                         boolean immediate)
        throws IOException
    {
        channel.basicPublish(exchangeName, queueName, mandatory, immediate,
                             persistent ? MessageProperties.PERSISTENT_BASIC
                                        : MessageProperties.BASIC,
                             "nop".getBytes());
    }

    private void handleAck(long msgSeqNo, boolean multiple) {
        if (!ackSet.contains(msgSeqNo)) {
            fail("got duplicate ack: " + msgSeqNo);
        }
        if (multiple) {
            ackSet.headSet(msgSeqNo + 1).clear();
        } else {
            ackSet.remove(msgSeqNo);
        }
    }

    private void basicRejectCommon(boolean requeue)
        throws IOException
    {
        publishN("", "confirm-test-noconsumer", true, false, false);

        for (long i = 0; i < NUM_MESSAGES; i++) {
            GetResponse resp =
                channel.basicGet("confirm-test-noconsumer", false);
            long dtag = resp.getEnvelope().getDeliveryTag();
            channel.basicReject(dtag, requeue);
        }
    }

    private void waitAcks() throws InterruptedException {
        while (ackSet.size() > 0)
            Thread.sleep(10);
    }
}
