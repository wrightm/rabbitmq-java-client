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
//  The Initial Developer of the Original Code is VMware, Inc.
//  Copyright (c) 2007-2011 VMware, Inc.  All rights reserved.
//

package com.rabbitmq.client.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.utility.IntAllocator;

/**
 * Manages a set of channels, indexed by channel number (<code><b>1.._channelMax</b></code>).
 */

public final class ChannelManager {
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    /** Monitor for <code>_channelMap</code> and <code>channelNumberAllocator</code> */
    private final Object monitor = new Object();
        /** Mapping from <code><b>1.._channelMax</b></code> to {@link ChannelN} instance */
        private final Map<Integer, ChannelN> _channelMap = new HashMap<Integer, ChannelN>();
        private final IntAllocator channelNumberAllocator;

    private final ConsumerWorkService workService;

    private final Set<CountDownLatch> shutdownSet = new HashSet<CountDownLatch>();

    /** Maximum channel number available on this connection. */
    private final int _channelMax;

    public int getChannelMax(){
      return _channelMax;
    }

    public ChannelManager(ConsumerWorkService workService, int channelMax) {
        if (channelMax == 0) {
            // The framing encoding only allows for unsigned 16-bit integers
            // for the channel number
            channelMax = (1 << 16) - 1;
        }
        _channelMax = channelMax;
        channelNumberAllocator = new IntAllocator(1, channelMax);
        
        this.workService = workService;
    }

    /**
     * Looks up a channel on this connection.
     * @param channelNumber the number of the required channel
     * @return the channel on this connection with number <code><b>channelNumber</b></code>
     * @throws UnknownChannelException if there is no channel with number <code><b>channelNumber</b></code> on this connection
     */
    public ChannelN getChannel(int channelNumber) {
        synchronized (this.monitor) {
            ChannelN ch = _channelMap.get(channelNumber);
            if(ch == null) throw new UnknownChannelException(channelNumber);
            return ch;
        }
    }

    public void handleSignal(ShutdownSignalException signal) {
        Set<ChannelN> channels;
        synchronized(this.monitor) {
            channels = new HashSet<ChannelN>(_channelMap.values());
        }
        for (ChannelN channel : channels) {
            releaseChannelNumber(channel);
            channel.processShutdownSignal(signal, true, true);
            shutdownSet.add(channel.getShutdownLatch());
        }
        scheduleShutdownProcessing();
    }

    private void scheduleShutdownProcessing() {
        final Set<CountDownLatch> sdSet = new HashSet<CountDownLatch>(shutdownSet);
        final ConsumerWorkService ssWorkService = workService;
        Thread shutdownThread = new Thread( new Runnable() {
            public void run() {
                for (CountDownLatch latch : sdSet) {
                    try { latch.await(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS); } catch (Throwable e) { }
                }
                ssWorkService.shutdown();
            }}, "ConsumerWorkServiceShutdown");
        shutdownThread.setDaemon(true);
        shutdownThread.start();
    }

    public ChannelN createChannel(AMQConnection connection) throws IOException {
        ChannelN ch;
        synchronized (this.monitor) {
            int channelNumber = channelNumberAllocator.allocate();
            if (channelNumber == -1) {
                return null;
            } else {
                ch = addNewChannel(connection, channelNumber);
            }
        }
        ch.open(); // now that it's been safely added
        return ch;
    }

    public ChannelN createChannel(AMQConnection connection, int channelNumber) throws IOException {
        ChannelN ch;
        synchronized (this.monitor) {
            if (channelNumberAllocator.reserve(channelNumber)) {
                ch = addNewChannel(connection, channelNumber);
            } else {
                return null;
            }
        }
        ch.open(); // now that it's been safely added
        return ch;
    }

    private ChannelN addNewChannel(AMQConnection connection, int channelNumber) throws IOException {
        if (_channelMap.containsKey(channelNumber)) {
            // That number's already allocated! Can't do it
            // This should never happen unless something has gone
            // badly wrong with our implementation.
            throw new IllegalStateException("We have attempted to "
                    + "create a channel with a number that is already in "
                    + "use. This should never happen. "
                    + "Please report this as a bug.");
        }
        ChannelN ch = new ChannelN(connection, channelNumber, this.workService);
        _channelMap.put(ch.getChannelNumber(), ch);
        return ch;
    }

    /**
     * Remove the channel from the channel map and free the number for re-use.
     * This method must be safe to call multiple times on the same channel. If
     * it is not then things go badly wrong.
     */
    public void releaseChannelNumber(ChannelN channel) {
        // Warning, here be dragons. Not great big ones, but little baby ones
        // which will nibble on your toes and occasionally trip you up when
        // you least expect it. (Pixies? HP2)
        // Basically, there's a race that can end us up here. It almost never
        // happens, but it's easier to repair it when it does than prevent it
        // from happening in the first place.
        // If we end up doing a Channel.close in one thread and a Channel.open
        // with the same channel number in another, the two can overlap in such
        // a way as to cause disconnectChannel on the old channel to try to
        // remove the new one. Ideally we would fix this race at the source,
        // but it's much easier to just catch it here.
        synchronized (this.monitor) {
            int channelNumber = channel.getChannelNumber();
            ChannelN existing = _channelMap.remove(channelNumber);
            // Nothing to do here. Move along.
            if (existing == null)
                return;
            // Oops, we've gone and stomped on someone else's channel. Put it
            // back and pretend we didn't touch it.
            else if (existing != channel) {
                _channelMap.put(channelNumber, existing);
                return;
            }
            channelNumberAllocator.free(channelNumber);
        }
    }
}
