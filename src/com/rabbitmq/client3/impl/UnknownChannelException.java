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

class UnknownChannelException extends RuntimeException {
    /** Default for non-checking. */
    private static final long serialVersionUID = 1L;
    private final int channelNumber;

    public UnknownChannelException(int channelNumber) {
        super("Unknown channel number " + channelNumber);
        this.channelNumber = channelNumber;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

}
