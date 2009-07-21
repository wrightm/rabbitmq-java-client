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
package com.rabbitmq.client.test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;

import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.AMQP;

import com.rabbitmq.client.test.functional.BrokerTestCase;

/**
 * Test for bug 19356 - SSL Support in rabbitmq
 *
 */
public class Bug19356Test extends BrokerTestCase {

    public Exception caughtException = null;
    public boolean completed = false;
    public boolean created = false;

    public void openConnection()
        throws IOException
    {
        try {
            connectionFactory.useSslProtocol();
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        } catch (KeyManagementException ex) {
            throw new IOException(ex);
        }


        if (connection == null) {
            connection = connectionFactory.newConnection("localhost", 5671);
        }
    }

    protected void releaseResources()
        throws IOException
    {
        if (created) {
            channel.queueDelete("Bug19356Test");
        }
    }

    public void testBug19356()
        throws IOException
    {
        channel.queueDeclare("Bug19356Test", false, false, true, true, null);
        channel.basicPublish("", "Bug19356Test", null, "SSL".getBytes());

        GetResponse chResponse = channel.basicGet("Bug19356Test", false);
        assertTrue(chResponse != null);

        byte[] body = chResponse.getBody();
        assertEquals("SSL", new String(body));
    }
}
