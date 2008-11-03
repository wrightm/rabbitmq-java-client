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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.GetResponse;

import java.io.IOException;

public class Routing extends BrokerTestCase
{

    protected final String E = "MRDQ";
    protected final String Q1 = "foo";
    protected final String Q2 = "bar";

    protected void createResources() throws IOException {
        channel.exchangeDeclare(ticket, E, "direct");
        channel.queueDeclare(ticket, Q1);
        channel.queueDeclare(ticket, Q2);
    }

    protected void releaseResources() throws IOException {
        channel.queueDelete(ticket, Q1);
        channel.queueDelete(ticket, Q2);
        channel.exchangeDelete(ticket, E);
    }

    private void bind(String queue, String routingKey)
        throws IOException
    {
        channel.queueBind(ticket, queue, E, routingKey);
    }

    private void check(String routingKey, boolean expectQ1, boolean expectQ2)
        throws IOException
    {
        channel.basicPublish(ticket, E, routingKey, null, "mrdq".getBytes());
        checkGet(Q1, expectQ1);
        checkGet(Q2, expectQ2);
    }

    private void checkGet(String queue, boolean messageExpected)
        throws IOException
    {
        GetResponse r = channel.basicGet(ticket, queue, true);
        if (messageExpected) {
            assertNotNull(r);
        } else {
            assertNull(r);
        }
    }

    /**
     * Tests the "default queue name" and "default routing key" pieces
     * of the spec. See the doc for the "queue" and "routing key"
     * fields of queue.bind.
     */
    public void testMRDQRouting()
        throws IOException
    {
        bind(Q1, "baz");        //Q1, "baz"
        bind(Q1, "");           //Q1, ""
        bind("", "baz");        //Q2, "baz"
        bind("", "");           //Q2, Q2
        check("", true, false);
        check(Q1, false, false);
        check(Q2, false, true);
        check("baz", true, true);
    }

    /**
     * If a queue has more than one binding to an exchange, it should
     * NOT receive duplicate copies of a message that matches both
     * bindings.
     */
    public void testDoubleBinding()
        throws IOException
    {
        channel.queueBind(ticket, Q1, "amq.topic", "x.#");
        channel.queueBind(ticket, Q1, "amq.topic", "#.x");
        channel.basicPublish(ticket, "amq.topic", "x.y", null, "x.y".getBytes());
        checkGet(Q1, true);
        checkGet(Q1, false);
        channel.basicPublish(ticket, "amq.topic", "y.x", null, "y.x".getBytes());
        checkGet(Q1, true);
        checkGet(Q1, false);
        channel.basicPublish(ticket, "amq.topic", "x.x", null, "x.x".getBytes());
        checkGet(Q1, true);
        checkGet(Q1, false);
    }

    public void testUnbind() throws Exception {
        AMQP.Queue.DeclareOk ok = channel.queueDeclare(ticket);
        String queue = ok.getQueue();

        String routingKey = "quay";
        String x = "amq.direct";

        channel.queueBind(ticket, queue, x, routingKey);
        channel.basicPublish(ticket, x, routingKey, null, "foobar".getBytes());
        checkGet(queue, true);

        channel.queueUnbind(ticket, queue, x, routingKey);

        channel.basicPublish(ticket, x, routingKey, null, "foobar".getBytes());
        checkGet(queue, false);

        channel.queueDelete(ticket, queue);
    }
}
