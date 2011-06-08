package com.rabbitmq.client.test.functional;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.test.BrokerTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class DeadLetterExchange extends BrokerTestCase {

    private static final String DEAD_LETTER_EXCHANGE = "dead.letter.exchange";

    private static final String DEAD_LETTER_EXCHANGE_ARG = "x-dead-letter-exchange";

    private static final String TEST_QUEUE_NAME = "test.queue.dead.letter";

    private static final String DEAD_LETTER_QUEUE = "queue.dle";

    @Override
    protected void createResources() throws IOException {
        this.channel.exchangeDeclare(DEAD_LETTER_EXCHANGE, "direct");
        this.channel.queueDeclare(DEAD_LETTER_QUEUE, false, true, false, null);
    }

    @Override
    protected void releaseResources() throws IOException {
        this.channel.exchangeDelete(DEAD_LETTER_EXCHANGE);
    }

    public void testDeclareQueueWithNoDeadLetterExchange() throws IOException {
        this.channel.queueDeclare(TEST_QUEUE_NAME, false, true, false, null);
    }

    public void testDeclareQueueWithExistingDeadLetterExchange() throws IOException {
        declareQueue(DEAD_LETTER_EXCHANGE);
    }

    public void testDeclareQueueWithInvalidDeadLetterExchangeArg() throws IOException {
        try {
            declareQueue(133);
            fail("x-dead-letter-exchange must be a valid exchange name");
        } catch(IOException ex) {
            checkShutdownSignal(AMQP.PRECONDITION_FAILED, ex);
        }

    }

    public void testDeclareQueueWithNonExistentDeadLetterExchange()
        throws IOException {
        try {
            declareQueue("some.random.exchange.name");
            fail("x-dead-letter-exchange must be a existing exchange name");
        } catch(IOException ex) {
            checkShutdownSignal(AMQP.PRECONDITION_FAILED, ex);
        }
    }

    public void testDeadLetterQueueTTLExpiredMessages() throws Exception {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-message-ttl", 1000);

        deadLetterTest(new Runnable() {
                public void run() {
                    sleep(2000);
                }
            },
            args,
            PropertiesFactory.NULL, "expired");

    }

    public void testDeadLetterQueueDeleted() throws Exception {
        deadLetterTest(new Callable<Void>() {
                public Void call() throws Exception{
                    channel.queueDelete(TEST_QUEUE_NAME);
                    return null;
                }
            }, null, PropertiesFactory.NULL, "queue_deleted");
    }

    public void testDeadLetterQueuePurged() throws Exception {
        deadLetterTest(new Callable<Void>() {
                public Void call() throws Exception{
                    channel.queuePurge(TEST_QUEUE_NAME);
                    return null;
                }
            }, null, PropertiesFactory.NULL, "queue_purged");
    }

    private void deadLetterTest(final Runnable deathTrigger,
                                Map<String, Object> queueDeclareArgs,
                                PropertiesFactory propsFactory,
                                String reason)
        throws Exception {
        deadLetterTest(new Callable<Object>() {
            public Object call() throws Exception {
                deathTrigger.run();
                return null;
            }
        }, queueDeclareArgs, propsFactory, reason);
    }
    private void deadLetterTest(Callable<?> deathTrigger,
                                Map<String, Object> queueDeclareArgs,
                                PropertiesFactory propsFactory,
                                String reason)
        throws Exception {

        declareQueue(DEAD_LETTER_EXCHANGE, queueDeclareArgs);

        this.channel.queueBind(TEST_QUEUE_NAME, "amq.direct", "test");
        this.channel.queueBind(DEAD_LETTER_QUEUE, DEAD_LETTER_EXCHANGE, "test");

        int msgCount = 10;

        for(int x = 0; x < msgCount; x++) {
            this.channel.basicPublish("amq.direct", "test",
                                      propsFactory.create(x),
                                      "test message".getBytes());
        }

        deathTrigger.call();

        for(int x = 0; x < msgCount; x++) {
            GetResponse getResponse =
                this.channel.basicGet(DEAD_LETTER_QUEUE, true);
            assertNotNull("Message not dead-lettered", getResponse);
            assertEquals("test message", new String(getResponse.getBody()));

            Map<String, Object> headers = getResponse.getProps().getHeaders();
            assertNotNull(headers);
            assertEquals(TEST_QUEUE_NAME, headers.get("x-death-queue").toString());
            assertEquals(reason, headers.get("x-death-reason").toString());
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch(InterruptedException ex) {
        }
    }

    private void declareQueue(Object deadLetterExchange) throws IOException {
        declareQueue(deadLetterExchange, new HashMap<String, Object>());
    }

    private void declareQueue(Object deadLetterExchange,
                              Map<String, Object> args) throws IOException {
        if(args == null) {
            args = new HashMap<String, Object>();
        }

        args.put(DEAD_LETTER_EXCHANGE_ARG, deadLetterExchange);
        this.channel.queueDeclare(TEST_QUEUE_NAME, false, true, false, args);
    }


    private static interface PropertiesFactory {
        static final PropertiesFactory NULL = new PropertiesFactory(){
                public AMQP.BasicProperties create(int msgNum) {
                    return null;
                }
            };

        AMQP.BasicProperties create(int msgNum);
    }
}
