package com.rabbitmq.client3.test.functional;

import com.rabbitmq.client3.AlreadyClosedException;
import com.rabbitmq.client3.Channel;
import com.rabbitmq.client3.test.BrokerTestCase;

import java.io.IOException;

public class BasicGet extends BrokerTestCase {
  public void testBasicGetWithEnqueuedMessages() throws IOException, InterruptedException {
    assertTrue(channel.isOpen());
    String q = channel.queueDeclare().getQueue();

    basicPublishPersistent("msg".getBytes("UTF-8"), q);
    Thread.sleep(250);

    assertNotNull(channel.basicGet(q, true));
    channel.queuePurge(q);
    assertNull(channel.basicGet(q, true));
    channel.queueDelete(q);
  }

  public void testBasicGetWithEmptyQueue() throws IOException, InterruptedException {
    assertTrue(channel.isOpen());
    String q = channel.queueDeclare().getQueue();

    assertNull(channel.basicGet(q, true));
    channel.queueDelete(q);
  }

  public void testBasicGetWithClosedChannel() throws IOException, InterruptedException {
    assertTrue(channel.isOpen());
    String q = channel.queueDeclare().getQueue();

    channel.close();
    assertFalse(channel.isOpen());
    try {
      channel.basicGet(q, true);
      fail("expected basic.get on a closed channel to fail");
    } catch (AlreadyClosedException e) {
      // passed
    } finally {
      Channel tch = connection.createChannel();
      tch.queueDelete(q);
      tch.close();
    }

  }
}
