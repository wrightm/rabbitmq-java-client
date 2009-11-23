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
//   Portions created by LShift Ltd are Copyright (C) 2007-2009 LShift
//   Ltd. Portions created by Cohesive Financial Technologies LLC are
//   Copyright (C) 2007-2009 Cohesive Financial Technologies
//   LLC. Portions created by Rabbit Technologies Ltd are Copyright
//   (C) 2007-2009 Rabbit Technologies Ltd.
//
//   All Rights Reserved.
//
//   Contributor(s): ______________________________________.
//

package com.rabbitmq.client.test.functional;

import com.rabbitmq.client.test.BrokerTestCase;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;

/**
 * This tests whether bindings are created and nuked properly.
 *
 * The tests attempt to declare durable queues on a secondary node, if
 * present, and that node is restarted as part of the tests while the
 * primary node is still running. That way we exercise any node-down
 * handler code in the server.
 *
 */
public class BindingLifecycleBase extends BrokerTestCase {
  protected static final String K = "K-" + System.currentTimeMillis();
  protected static final int N = 1;
  protected static final String Q = "Q-" + System.currentTimeMillis();
  protected static final String X = "X-" + System.currentTimeMillis();
  protected static final byte[] payload = ("" + System.currentTimeMillis()).getBytes();

  protected static String randomString() {
    return "-" + System.nanoTime();
  }
  public Channel secondaryChannel;
  public Connection secondaryConnection;

    @Override
  public void openChannel() throws IOException {
    if (secondaryConnection != null) {
      secondaryChannel = secondaryConnection.createChannel();
    }
    super.openChannel();
  }

  @Override
  public void openConnection() throws IOException {
    super.openConnection();
    if (secondaryConnection == null) {
      try {
        secondaryConnection = connectionFactory.newConnection("localhost", 5673);
      }
      catch (IOException e) {
      }
    }
  }

  @Override
  public void closeChannel() throws IOException {
    if (secondaryChannel != null) {
      secondaryChannel.abort();
      secondaryChannel = null;
    }
    super.closeChannel();
  }

  @Override
  public void closeConnection() throws IOException {
    if (secondaryConnection != null) {
      secondaryConnection.abort();
      secondaryConnection = null;
    }
    super.closeConnection();
  }

  protected void createQueueAndBindToExchange(Binding binding, boolean durable) throws IOException {
    channel.exchangeDeclare(binding.x, "direct", durable);
    channel.queueDeclare(binding.q, durable);
    channel.queueBind(binding.q, binding.x, binding.k);
  }

  protected void declareDurableQueue(String q) throws IOException {
    (secondaryChannel == null ? channel : secondaryChannel).queueDeclare(q, true);
  }

  protected void deleteExchangeAndQueue(Binding binding) throws IOException {
    channel.queueDelete(binding.q);
    channel.exchangeDelete(binding.x);
  }

  protected void doAutoDelete(boolean durable, int queues) throws IOException {
    String[] queueNames = null;
    Binding binding = Binding.randomBinding();
    channel.exchangeDeclare(binding.x, "direct", false, durable, true, null);
    channel.queueDeclare(binding.q, false, durable, false, true, null);
    channel.queueBind(binding.q, binding.x, binding.k);
    if (queues > 1) {
      int j = queues - 1;
      queueNames = new String[j];
      for (int i = 0; i < j; i++) {
        queueNames[i] = randomString();
        channel.queueDeclare(queueNames[i], false, durable, false, false, null);
        channel.queueBind(queueNames[i], binding.x, binding.k);
        channel.basicConsume(queueNames[i], true, new QueueingConsumer(channel));
      }
    }
    subscribeSendUnsubscribe(binding);
    if (durable) {
      restart();
    }
    if (queues > 1) {
      for (String s : queueNames) {
        channel.basicConsume(s, true, new QueueingConsumer(channel));
        Binding tmp = new Binding(s, binding.x, binding.k);
        sendUnroutable(tmp);
      }
    }
    channel.queueDeclare(binding.q, false, durable, true, true, null);
    // if (queues == 1): Because the exchange does not exist, this
    // bind should fail
    try {
      channel.queueBind(binding.q, binding.x, binding.k);
      sendRoutable(binding);
    }
    catch (Exception e) {
      // do nothing, this is the correct behaviour
      channel = null;
      return;
    }
    if (queues == 1) {
      deleteExchangeAndQueue(binding);
      fail("Queue bind should have failed");
    }
    // Do some cleanup
    if (queues > 1) {
      for (String q : queueNames) {
        channel.queueDelete(q);
      }
    }
  }

  protected void restart() throws IOException {
  }

  protected void sendRoutable(Binding binding) throws IOException {
    channel.basicPublish(binding.x, binding.k, null, payload);
    GetResponse response = channel.basicGet(binding.q, true);
    assertNotNull("The response should not be null", response);
  }

  protected void sendUnroutable(Binding binding) throws IOException {
    channel.basicPublish(binding.x, binding.k, null, payload);
    GetResponse response = channel.basicGet(binding.q, true);
    assertNull("The response SHOULD BE null", response);
  }

  protected Binding setupExchangeAndRouteMessage(boolean durable) throws IOException {
    Binding binding = setupExchangeBindings(durable);
    sendRoutable(binding);
    return binding;
  }

  protected Binding setupExchangeBindings(boolean durable) throws IOException {
    Binding binding = Binding.randomBinding();
    createQueueAndBindToExchange(binding, durable);
    return binding;
  }
  
  protected void subscribeSendUnsubscribe(Binding binding) throws IOException {
    String tag = channel.basicConsume(binding.q, new QueueingConsumer(channel));
    sendUnroutable(binding);
    channel.basicCancel(tag);
  }

  protected static class Binding {

    String q;
    String x;
    String k;

    static Binding randomBinding() {
      return new Binding(randomString(), randomString(), randomString());
    }

    protected Binding(String q, String x, String k) {
      this.q = q;
      this.x = x;
      this.k = k;
    }
  }

  // A couple of tests that are common to the subclasses (which differ on
  // whether the broker is restarted)

  /**
   *
   * The same thing as testExchangeAutoDelete, but with durable
   * queues.
   *
   * Main difference is restarting the broker to make sure that the
   * durable queues are blasted away.
   */
  public void testExchangeAutoDeleteDurable() throws IOException {
    doAutoDelete(true, 1);
  }

  /**
   * The same thing as testExchangeAutoDeleteManyBindings, but with
   * durable queues.
   */
  public void testExchangeAutoDeleteDurableManyBindings() throws IOException {
    doAutoDelete(true, 10);
  }

}
