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

/**
 * Encapsulates a group of parameters used for AMQP's Basic methods
 */
public class Envelope {
    private final long _deliveryTag;
    private final boolean _redeliver;
    private final String _exchange;
    private final String _routingKey;

    /**
     * Construct an {@link Envelope} with the specified construction parameters
     * @param deliveryTag the delivery tag
     * @param redeliver true if this is a redelivery following a failed ack
     * @param exchange the exchange used for the current operation
     * @param routingKey the associated routing key
     */
    public Envelope(long deliveryTag, boolean redeliver, String exchange, String routingKey) {
        this._deliveryTag = deliveryTag;
        this._redeliver = redeliver;
        this._exchange = exchange;
        this._routingKey = routingKey;
    }

    /**
     * Get the delivery tag included in this parameter envelope
     * @return the delivery tag
     */
    public long getDeliveryTag() {
        return _deliveryTag;
    }

    /**
     * Get the redelivery flag included in this parameter envelope
     * @return the redelivery flag
     */
    public boolean isRedeliver() {
        return _redeliver;
    }

    /**
     * Get the name of the exchange included in this parameter envelope
     * @return the exchange
     */
    public String getExchange() {
        return _exchange;
    }

    /**
     * Get the routing key included in this parameter envelope
     * @return the routing key
     */
    public String getRoutingKey() {
        return _routingKey;
    }
}
