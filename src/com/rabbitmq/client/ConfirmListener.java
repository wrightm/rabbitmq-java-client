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
//  Copyright (c) 2007-2012 VMware, Inc.  All rights reserved.
//


package com.rabbitmq.client;

import java.io.IOException;

/**
 * Implement this interface in order to be notified of Confirm events.
 * Acks represent messages handled succesfully; Nacks represent
 * messages lost by the broker.  Note, the lost messages could still
 * have been delivered to consumers, but the broker cannot guarantee
 * this.
 */
public interface ConfirmListener {
    void handleAck(long deliveryTag, boolean multiple)
        throws IOException;

    void handleNack(long deliveryTag, boolean multiple)
        throws IOException;
}
