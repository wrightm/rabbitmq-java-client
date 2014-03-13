package com.rabbitmq.client3.test.functional;

import com.rabbitmq.client3.AMQP;
import com.rabbitmq.client3.Channel;
import com.rabbitmq.client3.test.BrokerTestCase;

import java.io.IOException;
import java.util.HashMap;

public class HeadersExchangeValidation extends BrokerTestCase {

    public void testHeadersValidation() throws IOException
    {
        AMQP.Queue.DeclareOk ok = channel.queueDeclare();
        String queue = ok.getQueue();

        HashMap<String, Object> arguments = new HashMap<String, Object>();
        succeedBind(queue, arguments);

        arguments.put("x-match", 23);
        failBind(queue, arguments);

        arguments.put("x-match", "all or any I don't mind");
        failBind(queue, arguments);

        arguments.put("x-match", "all");
        succeedBind(queue, arguments);

        arguments.put("x-match", "any");
        succeedBind(queue, arguments);
    }

    private void failBind(String queue, HashMap<String, Object> arguments) {
        try {
            Channel ch = connection.createChannel();
            ch.queueBind(queue, "amq.headers", "", arguments);
            fail("Expected failure");
        } catch (IOException e) {
	        checkShutdownSignal(AMQP.PRECONDITION_FAILED, e);
        }
    }

    private void succeedBind(String queue, HashMap<String, Object> arguments) throws IOException {
        Channel ch = connection.createChannel();
        ch.queueBind(queue, "amq.headers", "", arguments);
        ch.close();
    }
}
