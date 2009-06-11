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

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.IOUtils;

/**
 * Helper class to generates AMQP wire-protocol encoded values.
 */
public class ValueWriter
{

    /** Accumulates our output */
    private final DataOutputStream out;

    public ValueWriter(DataOutputStream out)
    {
        this.out = out;
    }

    /** Public API - encodes a short string. */
    public final void writeShortstr(String str)
        throws IOException
    {
        byte [] bytes = str.getBytes("utf-8");
        out.writeByte(bytes.length);
        out.write(bytes);
    }

    /** Public API - encodes a long string from a LongString. */
    public final void writeLongstr(LongString str)
        throws IOException
    {
        writeLong((int)str.length());
        IOUtils.copy(str.getStream(), out);
    }

    /** Public API - encodes a long string from a String. */
    public final void writeLongstr(String str)
        throws IOException
    {
        byte [] bytes = str.getBytes("utf-8");
        writeLong(bytes.length);
        out.write(bytes);
    }

    /** Public API - encodes a short integer. */
    public final void writeShort(int s)
        throws IOException
    {
        out.writeShort(s);
    }

    /** Public API - encodes an integer. */
    public final void writeLong(int l)
        throws IOException
    {
        // java's arithmetic on this type is signed, however its
        // reasonable to use ints to represent the unsigned long
        // type - for values < Integer.MAX_VALUE everything works
        // as expected
        out.writeInt(l);
    }

    /** Public API - encodes a long integer. */
    public final void writeLonglong(long ll)
        throws IOException
    {
        out.writeLong(ll);
    }

    /** Public API - encodes a table. */
    public final void writeTable(Map<String, Object> table)
        throws IOException
    {
        if (table == null) {
            // Convenience.
            out.writeInt(0);
        } else {
            out.writeInt((int)Frame.tableSize(table));
            for(Map.Entry<String,Object> entry:  table.entrySet()) {
                writeShortstr(entry.getKey());
                Object value = entry.getValue();
                if(value instanceof String) {
                    writeOctet('S');
                    writeLongstr((String)value);
                }
                else if(value instanceof LongString) {
                    writeOctet('S');
                    writeLongstr((LongString)value);
                }
                else if(value instanceof Integer) {
                    writeOctet('I');
                    writeLong((Integer) value);
                }
                else if(value instanceof BigDecimal) {
                    writeOctet('D');
                    BigDecimal decimal = (BigDecimal)value;
                    writeOctet(decimal.scale());
                    BigInteger unscaled = decimal.unscaledValue();
                    if(unscaled.bitLength() > 32) /*Integer.SIZE in Java 1.5*/
                        throw new IllegalArgumentException
                            ("BigDecimal too large to be encoded");
                    writeLong(decimal.unscaledValue().intValue());
                }
                else if(value instanceof Date) {
                    writeOctet('T');
                    writeTimestamp((Date)value);
                }
                else if(value instanceof Map) {
                    writeOctet('F');
                    // Ignore the warnings here.  We hate erasure
                    // (not even a little respect)
                    writeTable((Map<String, Object>) value);
                }
                else if (value instanceof Byte) {
                    writeOctet('b');
                    out.writeByte((Byte)value);
                }
                else if(value instanceof Double) {
                    writeOctet('d');
                    out.writeDouble((Double)value);
                }
                else if(value instanceof Float) {
                    writeOctet('f');
                    out.writeFloat((Float)value);
                }
                else if(value instanceof Long) {
                    writeOctet('l');
                    out.writeLong((Long)value);
                }
                else if(value instanceof Short) {
                    writeOctet('s');
                    out.writeShort((Short)value);
                }
                else if(value instanceof Boolean) {
                    writeOctet('t');
                    out.writeBoolean((Boolean)value);
                }
                else if(value instanceof byte[]) {
                    writeOctet('x');
                    writeLong(((byte[])value).length);
                    out.write((byte[])value);
                }
                else if(value == null) {
                    writeOctet('V');
                }
                else {
                    throw new IllegalArgumentException
                        ("Invalid value type: " + value.getClass().getName()
                         + " for key " + entry.getKey());
                }
            }
        }
    }

    /** Public API - encodes an octet from an int. */
    public final void writeOctet(int octet)
        throws IOException
    {
        out.writeByte(octet);
    }

    /** Public API - encodes an octet from a byte. */
    public final void writeOctet(byte octet)
        throws IOException
    {
        out.writeByte(octet);
    }

    /** Public API - encodes a timestamp. */
    public final void writeTimestamp(Date timestamp)
        throws IOException
    {
        // AMQP uses POSIX time_t which is in seconds since the epoch began
        writeLonglong(timestamp.getTime()/1000);
    }

    /**
     * Public API - call this to ensure all accumulated
     * values are correctly written to the output stream.
     */
    public void flush()
        throws IOException
    {
        out.flush();
    }
}