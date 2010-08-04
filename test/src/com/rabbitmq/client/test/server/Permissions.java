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

package com.rabbitmq.client.test.server;

import com.rabbitmq.client.test.BrokerTestCase;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Command;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Method;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.AMQChannel;
import com.rabbitmq.client.impl.AMQImpl;
import com.rabbitmq.tools.Host;

public class Permissions extends BrokerTestCase
{

    protected Channel adminCh;
    protected Channel noAccessCh;

    public Permissions()
    {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("test");
        factory.setPassword("test");
        factory.setVirtualHost("/test");
        connectionFactory = factory;
    }

    protected void setUp()
        throws IOException
    {
        addRestrictedAccount();
        super.setUp();
    }

    protected void tearDown()
        throws IOException
    {
        super.tearDown();
        deleteRestrictedAccount();
    }

    protected void addRestrictedAccount()
        throws IOException
    {
        runCtl("add_user test test");
        runCtl("add_user testadmin test");
        runCtl("add_user noaccess test");
        runCtl("add_vhost /test");
        runCtl("set_permissions -p /test test configure write read");
        runCtl("set_permissions -p /test testadmin \".*\" \".*\" \".*\"");
        runCtl("set_permissions -p /test -s all noaccess \"\" \"\" \"\"");
    }

    protected void deleteRestrictedAccount()
        throws IOException
    {
        runCtl("clear_permissions -p /test noaccess");
        runCtl("clear_permissions -p /test testadmin");
        runCtl("clear_permissions -p /test test");
        runCtl("delete_vhost /test");
        runCtl("delete_user noaccess");
        runCtl("delete_user testadmin");
        runCtl("delete_user test");
    }

    protected void runCtl(String command)
        throws IOException
    {
        Host.executeCommand("../rabbitmq-server/scripts/rabbitmqctl " +
                            command);
    }

    protected void createResources()
        throws IOException
    {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("testadmin");
        factory.setPassword("test");
        factory.setVirtualHost("/test");
        Connection connection = factory.newConnection();
        adminCh = connection.createChannel();
        withNames(new WithName() {
                public void with(String name) throws IOException {
                    adminCh.exchangeDeclare(name, "direct");
                    adminCh.queueDeclare(name, false, false, false, null);
                }});

        factory = new ConnectionFactory();
        factory.setUsername("noaccess");
        factory.setPassword("test");
        factory.setVirtualHost("/test");
        noAccessCh = factory.newConnection().createChannel();
    }

    protected void releaseResources()
        throws IOException
    {
        withNames(new WithName() {
                public void with(String name) throws IOException {
                    adminCh.queueDelete(name);
                    adminCh.exchangeDelete(name);
                }});
        adminCh.getConnection().abort();
        noAccessCh.getConnection().abort();
    }

    protected void withNames(WithName action)
        throws IOException
    {
        action.with("configure");
        action.with("write");
        action.with("read");
    }

    public void testAuth()
    {
        ConnectionFactory unAuthFactory = new ConnectionFactory();
        unAuthFactory.setUsername("test");
        unAuthFactory.setPassword("tset");

        try {
            unAuthFactory.newConnection();
            fail("Exception expected if password is wrong");
        } catch (IOException e) {
            String msg = e.getMessage();
            assertTrue("Exception message should contain auth", msg.toLowerCase().contains("auth"));
        }
    }

    public void testExchangeConfiguration()
        throws IOException
    {
        runConfigureTest(new WithName() {
                public void with(String name) throws IOException {
                    channel.exchangeDeclare(name, "direct");
                }});
        runConfigureTest(new WithName() {
                public void with(String name) throws IOException {
                    channel.exchangeDeclarePassive(name);
                }});
        runConfigureTest(new WithName() {
                public void with(String name) throws IOException {
                    channel.exchangeDelete(name);
                }});
    }

    public void testQueueConfiguration()
        throws IOException
    {
        runConfigureTest(new WithName() {
                public void with(String name) throws IOException {
                    channel.queueDeclare(name, false, false, false, null);
                }});
        runConfigureTest(new WithName() {
                public void with(String name) throws IOException {
                    channel.queueDeclarePassive(name);
                }});
        runConfigureTest(new WithName() {
                public void with(String name) throws IOException {
                    channel.queueDelete(name);
                }});
    }

    public void testBinding()
        throws IOException
    {
        runTest(false, true, false, new WithName() {
                public void with(String name) throws IOException {
                    channel.queueBind(name, "read", "");
                }});
        runTest(false, false, true, new WithName() {
                public void with(String name) throws IOException {
                    channel.queueBind("write", name, "");
                }});
    }

    public void testPublish()
        throws IOException
    {
        runTest(false, true, false, new WithName() {
                public void with(String name) throws IOException {
                    channel.basicPublish(name, "", null, "foo".getBytes());
                    //followed by a dummy synchronous command in order
                    //to catch any errors
                    ((AMQChannel)channel).exnWrappingRpc(new AMQImpl.Channel.Flow(true));
                }});
    }

    public void testGet()
        throws IOException
    {
        runTest(false, false, true, new WithName() {
                public void with(String name) throws IOException {
                    channel.basicGet(name, true);
                }});
    }

    public void testConsume()
        throws IOException
    {
        runTest(false, false, true, new WithName() {
                public void with(String name) throws IOException {
                    channel.basicConsume(name, new QueueingConsumer(channel));
                }});
    }

    public void testPurge()
        throws IOException
    {
        runTest(false, false, true, new WithName() {
                public void with(String name) throws IOException {
                    ((AMQChannel)channel).exnWrappingRpc(new AMQImpl.Queue.Purge(0, name, false));
                }});
    }

    public void testAltExchConfiguration()
        throws IOException
    {
        runTest(false, false, false,
                createAltExchConfigTest("configure-me"));
        runTest(false, false, false,
                createAltExchConfigTest("configure-and-write-me"));
        runTest(false, true, false,
                createAltExchConfigTest("configure-and-read-me"));
    }

    public void testNoAccess()
        throws IOException
    {
        expectExceptionRun(AMQP.ACCESS_REFUSED, new WithName() {
                public void with(String _) throws IOException {
                    noAccessCh.queueDeclare("justaqueue", false, false, true, null);
                }}
        );
        expectExceptionRun(AMQP.ACCESS_REFUSED, new WithName() {
                public void with(String _) throws IOException {
                    noAccessCh.queueDeclare();
                }}
        );
        expectExceptionRun(AMQP.ACCESS_REFUSED, new WithName() {
                public void with(String _) throws IOException {
                    noAccessCh.queueDelete("configure");
                }}
        );
        expectExceptionRun(AMQP.ACCESS_REFUSED, new WithName() {
                public void with(String _) throws IOException {
                    noAccessCh.queueBind("write", "write", "write");
                }}
        );
        expectExceptionRun(AMQP.ACCESS_REFUSED, new WithName() {
                public void with(String _) throws IOException {
                    noAccessCh.queuePurge("read");
                }}
        );
        expectExceptionRun(AMQP.ACCESS_REFUSED, new WithName() {
                public void with(String _) throws IOException {
                    noAccessCh.exchangeDeclare("justanexchange", "direct");
                }}
        );
        expectExceptionRun(AMQP.ACCESS_REFUSED, new WithName() {
                public void with(String _) throws IOException {
                    noAccessCh.exchangeDeclare("configure", "direct");
                }}
        );
        expectExceptionRun(AMQP.ACCESS_REFUSED, new WithName() {
                public void with(String _) throws IOException {
                    noAccessCh.basicPublish("write", "", null, "foo".getBytes());
                    noAccessCh.queueDeclare();
                }}
        );
        expectExceptionRun(AMQP.ACCESS_REFUSED, new WithName() {
                public void with(String _) throws IOException {
                    noAccessCh.basicGet("read", false);
                }}
        );
        expectExceptionRun(AMQP.ACCESS_REFUSED, new WithName() {
                public void with(String _) throws IOException {
                    noAccessCh.basicConsume("read", null);
                }}
        );
    }

    protected void expectExceptionRun(int exceptionCode, WithName action)
        throws IOException
    {
        try {
            action.with("");
            fail();
        } catch (IOException e) {
            ShutdownSignalException sse = (ShutdownSignalException)e.getCause();
            if (sse.isHardError()) {
                fail("Got a hard-error.  Was expecting soft-error: " + exceptionCode);
            } else {
                AMQP.Channel.Close closeMethod =
                    (AMQP.Channel.Close) ((Command)sse.getReason()).getMethod();
                assertEquals(exceptionCode, closeMethod.getReplyCode());
            }
            noAccessCh = noAccessCh.getConnection().createChannel();
        }
    }

    protected WithName createAltExchConfigTest(final String exchange)
        throws IOException
    {
        return new WithName() {
            public void with(String ae) throws IOException {
                Map<String, Object> args = new HashMap<String, Object>();
                args.put("alternate-exchange", ae);
                channel.exchangeDeclare(exchange, "direct", false, false, args);
                channel.exchangeDelete(exchange);
            }};
    }

    protected void runConfigureTest(WithName test)
        throws IOException
    {
        runTest(true, "configure-me", test);
        runTest(false, "write-me", test);
        runTest(false, "read-me", test);
    }

    protected void runTest(boolean expC, boolean expW, boolean expR,
                           WithName test)
        throws IOException
    {
        runTest(expC, "configure", test);
        runTest(expW, "write", test);
        runTest(expR, "read", test);
    }

    protected void runTest(boolean exp, String name, WithName test)
        throws IOException
    {
        String msg = "'" + name + "' -> " + exp;
        try {
            test.with(name);
            assertTrue(msg, exp);
        } catch (IOException e) {
            assertFalse(msg, exp);
            Throwable t = e.getCause();
            assertTrue(msg, t instanceof ShutdownSignalException);
            Object r = ((ShutdownSignalException)t).getReason();
            assertTrue(msg, r instanceof Command);
            Method m = ((Command)r).getMethod();
            assertTrue(msg, m instanceof AMQP.Channel.Close);
            assertEquals(msg,
                         AMQP.ACCESS_REFUSED,
                         ((AMQP.Channel.Close)m).getReplyCode());
            //This fails due to bug 20296
            //openChannel();
            channel = connection.createChannel(channel.getChannelNumber() + 1);
        }
    }

    public interface WithName {
        public void with(String name) throws IOException;
    }

}
