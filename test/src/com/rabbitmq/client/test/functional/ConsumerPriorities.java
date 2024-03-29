package com.rabbitmq.client3.test.functional;

import com.rabbitmq.client3.AMQP;
import com.rabbitmq.client3.Channel;
import com.rabbitmq.client3.MessageProperties;
import com.rabbitmq.client3.QueueingConsumer;
import com.rabbitmq.client3.test.BrokerTestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ConsumerPriorities extends BrokerTestCase {
    public void testValidation() throws IOException {
        assertFailValidation(args("banana"));
        assertFailValidation(args(new HashMap()));
        assertFailValidation(args(null));
        assertFailValidation(args(Arrays.asList(1, 2, 3)));
    }

    private void assertFailValidation(Map<String, Object> args) throws IOException {
        Channel ch = connection.createChannel();
        String queue = ch.queueDeclare().getQueue();
        try {
            ch.basicConsume(queue, true, "", false, false, args, new QueueingConsumer(ch));
            fail("Validation should fail for " + args);
        } catch (IOException ioe) {
            checkShutdownSignal(AMQP.PRECONDITION_FAILED, ioe);
        }
    }

    private static final int COUNT = 10;

    public void testConsumerPriorities() throws Exception {
        String queue = channel.queueDeclare().getQueue();
        QueueingConsumer highConsumer = new QueueingConsumer(channel);
        QueueingConsumer medConsumer = new QueueingConsumer(channel);
        QueueingConsumer lowConsumer = new QueueingConsumer(channel);
        channel.basicConsume(queue, true, "high", false, false, args(1), highConsumer);
        channel.basicConsume(queue, true, "med", false, false, null, medConsumer);
        channel.basicConsume(queue, true, "low", false, false, args(-1), lowConsumer);

        publish(queue, COUNT, "high");
        channel.basicCancel("high");
        publish(queue, COUNT, "med");
        channel.basicCancel("med");
        publish(queue, COUNT, "low");

        assertContents(highConsumer, COUNT, "high");
        assertContents(medConsumer, COUNT, "med");
        assertContents(lowConsumer, COUNT, "low");
    }

    private Map<String, Object> args(Object o) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("x-priority", o);
        return map;
    }

    private void assertContents(QueueingConsumer qc, int count, String msg) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            QueueingConsumer.Delivery d = qc.nextDelivery();
            assertEquals(msg, new String(d.getBody()));
        }
        assertEquals(null, qc.nextDelivery(0));
    }

    private void publish(String queue, int count, String msg) throws IOException {
        for (int i = 0; i < count; i++) {
            channel.basicPublish("", queue, MessageProperties.MINIMAL_BASIC, msg.getBytes());
        }
    }
}
