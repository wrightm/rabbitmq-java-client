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
package com.rabbitmq.client.impl;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ContentHeader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Implementation of ContentHeader - specialized by autogenerated code in AMQP.java.
 */

public abstract class AMQContentHeader implements ContentHeader {
    /**
     * Private API - Called by {@link AMQChannel#handleFrame}. Parses the header frame.
     */
    @SuppressWarnings("unused")
    public long readFrom(DataInputStream in) throws IOException {
        int weight = in.readShort(); // Not currently used
        long bodySize = in.readLong();
        readPropertiesFrom(new ContentHeaderPropertyReader(in));
        return bodySize;
    }

    /**
     * Private API - Called by {@link AMQCommand#transmit}
     */
    public void writeTo(DataOutputStream out, long bodySize) throws IOException {
        out.writeShort(0); // weight - not currently used
        out.writeLong(bodySize);
        writePropertiesTo(new ContentHeaderPropertyWriter(out));
    }

    /**
     * Private API - Autogenerated reader for this header
     */
    public abstract void readPropertiesFrom(ContentHeaderPropertyReader reader) throws IOException;

    /**
     * Private API - Autogenerated writer for this header
     */
    public abstract void writePropertiesTo(ContentHeaderPropertyWriter writer) throws IOException;

    /**
     * Public API - {@inheritDoc}
     */
    public void appendPropertyDebugStringTo(StringBuffer acc) {
        acc.append("(?)");
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("#contentHeader<").append(getClassName()).append(">");
        this.appendPropertyDebugStringTo(sb);
        return sb.toString();
    }

    public Frame toFrame(int channelNumber, long bodySize) throws IOException {
        Frame frame = new Frame(AMQP.FRAME_HEADER, channelNumber);
        DataOutputStream bodyOut = frame.getOutputStream();
        bodyOut.writeShort(getClassId());
        writeTo(bodyOut, bodySize);
        return frame;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
