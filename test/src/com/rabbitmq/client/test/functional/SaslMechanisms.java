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
//  The Initial Developer of the Original Code is GoPivotal, Inc.
//  Copyright (c) 2007-2013 GoPivotal, Inc.  All rights reserved.
//

package com.rabbitmq.client3.test.functional;

import com.rabbitmq.client3.AuthenticationFailureException;
import com.rabbitmq.client3.Connection;
import com.rabbitmq.client3.ConnectionFactory;
import com.rabbitmq.client3.LongString;
import com.rabbitmq.client3.PossibleAuthenticationFailureException;
import com.rabbitmq.client3.SaslConfig;
import com.rabbitmq.client3.SaslMechanism;
import com.rabbitmq.client3.impl.AMQConnection;
import com.rabbitmq.client3.impl.LongStringHelper;
import com.rabbitmq.client3.test.BrokerTestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class SaslMechanisms extends BrokerTestCase {
    private String[] mechanisms;

    public class Mechanism implements SaslMechanism {
        private String name;
        private byte[][] responses;
        private int counter;

        public Mechanism(String name, byte[][] responses) {
            this.name = name;
            this.responses = responses;
        }

        public String getName() {
            return name;
        }

        public LongString handleChallenge(LongString challenge, String username, String password) {
            counter++;
            return LongStringHelper.asLongString(responses[counter-1]);
        }
    }

    public class Config implements SaslConfig {
        private String name;
        private byte[][] responses;

        public Config(String name, byte[][] responses) {
            this.name = name;
            this.responses = responses;
        }

        public SaslMechanism getSaslMechanism(String[] mechanisms) {
            SaslMechanisms.this.mechanisms = mechanisms;
            return new Mechanism(name, responses);
        }
    }

    // TODO test gibberish examples. ATM the server is not very robust.

    public void testPlainLogin() throws IOException {
        loginOk("PLAIN", new byte[][] {"\0guest\0guest".getBytes()} );
        loginBad("PLAIN", new byte[][] {"\0guest\0wrong".getBytes()} );
    }

    public void testAMQPlainLogin() throws IOException {
        // guest / guest
        loginOk("AMQPLAIN", new byte[][] {{5,76,79,71,73,78,83,0,0,0,5,103,117,101,115,116,8,80,65,83,83,87,79,82,68,83,0,0,0,5,103,117,101,115,116}} );
        // guest / wrong
        loginBad("AMQPLAIN", new byte[][] {{5,76,79,71,73,78,83,0,0,0,5,103,117,101,115,116,8,80,65,83,83,87,79,82,68,83,0,0,0,5,119,114,111,110,103}} );
    }

    public void testCRLogin() throws IOException {
        // Make sure mechanisms is populated
        loginOk("PLAIN", new byte[][] {"\0guest\0guest".getBytes()} );

        // We might be running this standalone
        if (Arrays.asList(mechanisms).contains("RABBIT-CR-DEMO")) {
            loginOk("RABBIT-CR-DEMO", new byte[][] {"guest".getBytes(), "My password is guest".getBytes()} );
            loginBad("RABBIT-CR-DEMO", new byte[][] {"guest".getBytes(), "My password is wrong".getBytes()} );
        }
    }

    public void testConnectionCloseAuthFailureUsername() throws IOException {
        connectionCloseAuthFailure("incorrect-username", "incorrect-password");
    }

    public void testConnectionCloseAuthFailurePassword() throws IOException {
        connectionCloseAuthFailure(connectionFactory.getUsername(), "incorrect-password");
    }

    public void connectionCloseAuthFailure(String username, String password) throws IOException {
        String failDetail =  "for username " + username + " and password " + password;
        try {
            Connection conn = connectionWithoutCapabilities(username, password);
            fail("Expected PossibleAuthenticationFailureException " + failDetail);
            conn.abort();
        } catch (PossibleAuthenticationFailureException paf) {
            if (paf instanceof AuthenticationFailureException) {
                fail("Not expecting AuthenticationFailureException " + failDetail);
            }
        }
    }

    // start a connection without capabilities, causing authentication failures
    // to be reported by the broker by closing the connection
    private Connection connectionWithoutCapabilities(String username, String password) throws IOException {
        ConnectionFactory customFactory = connectionFactory.clone();
        customFactory.setUsername(username);
        customFactory.setPassword(password);
        Map<String, Object> customProperties = AMQConnection.defaultClientProperties();
        customProperties.remove("capabilities");
        customFactory.setClientProperties(customProperties);
        return customFactory.newConnection();
    }

    private void loginOk(String name, byte[][] responses) throws IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setSaslConfig(new Config(name, responses));
        Connection connection = factory.newConnection();
        connection.close();
    }

    private void loginBad(String name, byte[][] responses) throws IOException {
        try {
            loginOk(name, responses);
            fail("Login succeeded!");
        } catch (AuthenticationFailureException e) {
            // Ok
        }
    }
}
