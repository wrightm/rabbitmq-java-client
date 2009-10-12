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

import java.io.IOException;
import java.io.DataInputStream;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.impl.Frame;
import com.rabbitmq.client.impl.SocketFrameHandler;
import com.rabbitmq.client.ConnectionFactory;

import junit.framework.TestCase;


/**
 * Check that protocol negotiation works
 */
public class ConnectionOpen extends TestCase
{
  public void testCorrectProtocolHeader() throws IOException {
    ConnectionFactory factory = new ConnectionFactory();
    SocketFrameHandler fh = new SocketFrameHandler(factory.getSocketFactory(),
                                                   "localhost", AMQP.PROTOCOL.PORT);
    fh.sendHeader();
    Frame f = fh.readFrame();
  }

  public void testCrazyProtocolHeader() throws IOException {
    ConnectionFactory factory = new ConnectionFactory();
    SocketFrameHandler fh = new SocketFrameHandler(factory.getSocketFactory(),
                                                   "localhost", AMQP.PROTOCOL.PORT);
    fh.sendHeader(100, 3); // major, minor
    DataInputStream in = fh._inputStream;
    // we should get a valid protocol header back
    byte[] header = new byte[4];
    in.read(header);
    // The protocol header is "AMQP" plus a version that the server
    // supports.  We can really only test for the first bit.
    assertEquals("AMQP", new String(header));
    in.read(header);
    try {
      fh.readFrame();
    }
    catch (IOException ioe) {
      // expected
      return;
    }
    fail("Expected an IOException trying to read more from the socket.");
  }

}
