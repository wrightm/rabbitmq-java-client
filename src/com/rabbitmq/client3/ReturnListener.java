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


package com.rabbitmq.client3;

import java.io.IOException;

/**
 * Implement this interface in order to be notified of failed
 * deliveries when basicPublish is called with "mandatory" or
 * "immediate" flags set.
 * @see Channel#basicPublish
 */
public interface ReturnListener {
    void handleReturn(int replyCode,
            String replyText,
            String exchange,
            String routingKey,
            AMQP.BasicProperties properties,
            byte[] body)
        throws IOException;
}
