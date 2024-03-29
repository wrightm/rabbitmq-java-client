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


package com.rabbitmq.client3.impl;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import com.rabbitmq.client3.LongString;

/**
 * Generates AMQP wire-protocol encoded arguments. Methods on this
 * object are usually called from autogenerated code.
 */
public class MethodArgumentWriter
{
    /** Writes our output */
    private final ValueWriter out;
    /** When encoding one or more bits, records whether a group of bits is waiting to be written */
    private boolean needBitFlush;
    /** The current group of bits */
    private byte bitAccumulator;
    /** The current position within the group of bits */
    private int bitMask;

    /**
     * Constructs a MethodArgumentWriter targetting the given DataOutputStream.
     */
    public MethodArgumentWriter(ValueWriter out)
    {
        this.out = out;
        resetBitAccumulator();
    }

    /** Private API - called to reset the bit group variables. */
    private void resetBitAccumulator() {
        needBitFlush = false;
        bitAccumulator = 0;
        bitMask = 1;
    }

    /**
     * Private API - called when we may be transitioning from encoding
     * a group of bits to encoding a non-bit value.
     */
    private final void bitflush()
        throws IOException
    {
        if (needBitFlush) {
            out.writeOctet(bitAccumulator);
            resetBitAccumulator();
        }
    }

    /** Public API - encodes a short string argument. */
    public final void writeShortstr(String str)
        throws IOException
    {
        bitflush();
        out.writeShortstr(str);
    }

    /** Public API - encodes a long string argument from a LongString. */
    public final void writeLongstr(LongString str)
        throws IOException
    {
        bitflush();
        out.writeLongstr(str);
    }

    /** Public API - encodes a long string argument from a String. */
    public final void writeLongstr(String str)
        throws IOException
    {
        bitflush();
        out.writeLongstr(str);
    }

    /** Public API - encodes a short integer argument. */
    public final void writeShort(int s)
        throws IOException
    {
        bitflush();
        out.writeShort(s);
    }

    /** Public API - encodes an integer argument. */
    public final void writeLong(int l)
        throws IOException
    {
        bitflush();
        out.writeLong(l);
    }

    /** Public API - encodes a long integer argument. */
    public final void writeLonglong(long ll)
        throws IOException
    {
        bitflush();
        out.writeLonglong(ll);
    }

    /** Public API - encodes a boolean/bit argument. */
    public final void writeBit(boolean b)
        throws IOException
    {
        if (bitMask > 0x80) {
            bitflush();
        }
        if (b) {
            bitAccumulator |= bitMask;
        } else {
            // um, don't set the bit.
        }
        bitMask = bitMask << 1;
        needBitFlush = true;
    }

    /** Public API - encodes a table argument. */
    public final void writeTable(Map<String, Object> table)
        throws IOException
    {
        bitflush();
        out.writeTable(table);
    }

    /** Public API - encodes an octet argument from an int. */
    public final void writeOctet(int octet)
        throws IOException
    {
        bitflush();
        out.writeOctet(octet);
    }

    /** Public API - encodes an octet argument from a byte. */
    public final void writeOctet(byte octet)
        throws IOException
    {
        bitflush();
        out.writeOctet(octet);
    }

    /** Public API - encodes a timestamp argument. */
    public final void writeTimestamp(Date timestamp)
        throws IOException
    {
        bitflush();
        out.writeTimestamp(timestamp);
    }

    /**
     * Public API - call this to ensure all accumulated argument
     * values are correctly written to the output stream.
     */
    public void flush()
        throws IOException
    {
        bitflush();
        out.flush();
    }
}
