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
 * Thrown when the likely cause is an authentication failure.
 */
public class PossibleAuthenticationFailureException extends IOException
{
    /** Default for non-checking. */
    private static final long serialVersionUID = 1L;

    public PossibleAuthenticationFailureException(Throwable cause)
    {
        super("Possibly caused by authentication failure");
        super.initCause(cause);
    }

    public PossibleAuthenticationFailureException(String reason)
    {
        super(reason);
    }
}
