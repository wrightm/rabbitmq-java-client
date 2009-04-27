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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import com.rabbitmq.client.ContentHeader;

/**
 * Parses an AMQP wire-protocol {@link ContentHeader} from a
 * DataInputStream. Methods on this object are usually called from
 * autogenerated code.
 */
public class ContentHeaderPropertyReader {
    /** Stream we are reading from */
    private final ValueReader in;

    /** Current field flag word */
    public int flagWord;

    /** Current flag position counter */
    public int bitCount;

    /**
     * Protected API - Constructs a reader from the given input stream
     */
    public ContentHeaderPropertyReader(DataInputStream in) throws IOException {
        this.in = new ValueReader(in);
        this.flagWord = 1; // just the continuation bit
        this.bitCount = 15; // forces a flagWord read
    }

    public boolean isContinuationBitSet() {
        return (flagWord & 1) != 0;
    }

    public void readFlagWord() throws IOException {
        if (!isContinuationBitSet()) {
            // FIXME: Proper exception class!
            throw new IOException("Attempted to read flag word when none advertised");
        }
        flagWord = in.readShort(); //FIXME: should this be read as a signedShort?
        bitCount = 0;
    }

    public boolean readPresence() throws IOException {
        if (bitCount == 15) {
            readFlagWord();
        }

        int bit = 15 - bitCount;
        bitCount++;
        return (flagWord & (1 << bit)) != 0;
    }

    public void finishPresence() throws IOException {
        if (isContinuationBitSet()) {
            // FIXME: Proper exception class!
            throw new IOException("Unexpected continuation flag word");
        }
    }

    /** Reads and returns an AMQP short string content header field. */
    public String readShortstr() throws IOException {
        return in.readShortstr();
    }

    /** Reads and returns an AMQP "long string" (binary) content header field. */
    public LongString readLongstr() throws IOException {
        return in.readLongstr();
    }

    /** Reads and returns an AMQP short integer content header field. */
    public Integer readShort() throws IOException {
        return in.readShort();
    }

    /** Reads and returns an AMQP integer content header field. */
    public Integer readLong() throws IOException {
        return in.readLong();
    }

    /** Reads and returns an AMQP long integer content header field. */
    public Long readLonglong() throws IOException {
        return in.readLonglong();
    }

    /** Reads and returns an AMQP table content header field. */
    public Map<String, Object> readTable() throws IOException {
        return in.readTable();
    }

    /** Reads and returns an AMQP octet content header field. */
    public Integer readOctet() throws IOException {
        return in.readOctet();
    }

    /** Reads and returns an AMQP timestamp content header field. */
    public Date readTimestamp() throws IOException {
        return in.readTimestamp();
    }
}
