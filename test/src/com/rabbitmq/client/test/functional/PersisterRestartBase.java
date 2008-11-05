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
//   The Initial Developers of the Original Code are LShift Ltd.,
//   Cohesive Financial Technologies LLC., and Rabbit Technologies Ltd.
//
//   Portions created by LShift Ltd., Cohesive Financial Technologies
//   LLC., and Rabbit Technologies Ltd. are Copyright (C) 2007-2008
//   LShift Ltd., Cohesive Financial Technologies LLC., and Rabbit
//   Technologies Ltd.;
//
//   All Rights Reserved.
//
//   Contributor(s): ______________________________________.
//

package com.rabbitmq.client.test.functional;

import java.io.IOException;

import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.tools.Host;

public class PersisterRestartBase extends BrokerTestCase
{

    // The time in ms the RabbitMQ persister waits before flushing the
    // persister log
    //
    // This matches the value of LOG_BUNDLE_DELAY in
    // rabbit_persister.erl
    protected static final int PERSISTER_DELAY = 5;

    // The number of entries that the RabbitMQ persister needs to
    // write before it takes a snapshot.
    //
    // This matches the value of MAX_WRAP_ENTRIES in
    // rabbit_persister.erl
    protected final int PERSISTER_SNAPSHOT_THRESHOLD = 500;

    protected void restart()
        throws IOException
    {
        tearDown();
        Host.executeCommand("cd ../rabbitmq-test; make restart-on-node");
        setUp();
    }

    protected void forceSnapshot()
        throws IOException, InterruptedException
    {
        Host.executeCommand("cd ../rabbitmq-test; make force-snapshot");
    }

    protected void declareDurableTopicExchange(String x)
        throws IOException
    {
        channel.exchangeDeclare(ticket, x, "topic", true);
    }

    protected void declareDurableDirectExchange(String x)
        throws IOException
    {
        channel.exchangeDeclare(ticket, x, "direct", true);
    }

    protected void declareDurableQueue(String q)
        throws IOException
    {
        channel.queueDeclare(ticket, q, true);
    }

    protected void declareAndBindDurableQueue(String q, String x, String r)
        throws IOException
    {
        channel.queueDeclare(ticket, q, true);
        channel.queueBind(ticket, q, x, r);
    }

    protected void deleteQueue(String q)
        throws IOException
    {
        channel.queueDelete(ticket, q);
    }

    protected void deleteExchange(String x)
        throws IOException
    {
        channel.exchangeDelete(ticket, x);
    }

    protected GetResponse basicGet(String q)
        throws IOException
    {
        return channel.basicGet(ticket, q, true);
    }

    protected void basicPublishPersistent(String q)
        throws IOException
    {
        channel.basicPublish(ticket, "", q,
                             MessageProperties.PERSISTENT_TEXT_PLAIN,
                             "persistent message".getBytes());
    }

    protected void basicPublishVolatile(String q)
        throws IOException
    {
        channel.basicPublish(ticket, "", q,
                             MessageProperties.TEXT_PLAIN,
                             "not persistent message".getBytes());
    }

    protected void basicPublishPersistent(String x, String routingKey)
        throws IOException
    {
        channel.basicPublish(ticket, x, routingKey,
                             MessageProperties.PERSISTENT_TEXT_PLAIN,
                             "persistent message".getBytes());
    }

    protected void basicPublishVolatile(String x, String routingKey)
        throws IOException
    {
        channel.basicPublish(ticket, x, routingKey,
                             MessageProperties.TEXT_PLAIN,
                             "not persistent message".getBytes());
    }

    protected void assertDelivered(String q, int count, boolean redelivered)
        throws IOException
    {
        GetResponse r;
        for (int i = 0; i < count; i++) {
            r = basicGet(q);
            assertNotNull(r);
            assertEquals(r.getEnvelope().isRedeliver(), redelivered);
        }
        assertNull(basicGet(q));
    }

    protected void assertDelivered(String q, int count)
        throws IOException
    {
        assertDelivered(q, count, false);
    }

}
