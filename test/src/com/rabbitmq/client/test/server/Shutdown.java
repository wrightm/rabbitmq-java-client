package com.rabbitmq.client3.test.server;

import com.rabbitmq.client3.test.BrokerTestCase;
import com.rabbitmq.client3.AMQP;
import com.rabbitmq.client3.Channel;
import com.rabbitmq.client3.Connection;

import java.io.IOException;

public class Shutdown extends BrokerTestCase {

    public void testErrorOnShutdown() throws Exception {
        bareRestart();
        expectError(AMQP.CONNECTION_FORCED);
    }

}
