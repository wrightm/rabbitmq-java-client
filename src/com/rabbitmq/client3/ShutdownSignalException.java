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

import com.rabbitmq.utility3.SensibleClone;

/**
 * Encapsulates a shutdown condition for a connection to an AMQP broker.
 * Depending on HardError when calling
 * {@link com.rabbitmq.client3.ShutdownSignalException#getReference()} we will
 * either get a reference to the Connection or Channel instance that fired
 * this exception.
 */

public class ShutdownSignalException extends RuntimeException implements SensibleClone<ShutdownSignalException> {
    /** Default for non-checking. */
    private static final long serialVersionUID = 1L;

    /** True if the connection is shut down, or false if this signal refers to a channel */
    private final boolean _hardError;

    /**
     * True if this exception is caused by explicit application
     * action; false if it originated with the broker or as a result
     * of detectable non-deliberate application failure
     */
    private final boolean _initiatedByApplication;

    /** Possible explanation */
    private final Object _reason;

    /** Either Channel or Connection instance, depending on _hardError */
    private final Object _ref;

    /**
     * Construct a ShutdownSignalException from the arguments.
     * @param hardError the relevant hard error
     * @param initiatedByApplication if the shutdown was client-initiated
     * @param reason Object describing the origin of the exception
     * @param ref Reference to Connection or Channel that fired the signal
     */
    public ShutdownSignalException(boolean hardError,
                                   boolean initiatedByApplication,
                                   Object reason, Object ref)
    {
        super((initiatedByApplication
               ? ("clean " + (hardError ? "connection" : "channel") + " shutdown")
               : ((hardError ? "connection" : "channel") + " error"))
              + "; reason: " + reason);
        this._hardError = hardError;
        this._initiatedByApplication = initiatedByApplication;
        this._reason = reason;
        // Depending on hardError what we got is either Connection or Channel reference
        this._ref = ref;
    }

    /** @return true if this signals a connection error, or false if a channel error */
    public boolean isHardError() { return _hardError; }

    /** @return true if this exception was caused by explicit application
     * action; false if it originated with the broker or as a result
     * of detectable non-deliberate application failure
     */
    public boolean isInitiatedByApplication() { return _initiatedByApplication; }

    /** @return the reason object, if any */
    public Object getReason() { return _reason; }

    /** @return Reference to Connection or Channel object that fired the signal **/
    public Object getReference() { return _ref; }

    public ShutdownSignalException sensibleClone() {
        try {
            return (ShutdownSignalException)super.clone();
        } catch (CloneNotSupportedException e) {
            // You've got to be kidding me
            throw new Error(e);
        }
    }
}


