/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.services.iot.client;

/**
 * This timeout exception can be thrown by the blocking APIs in this library
 * when expected time has elapsed.
 */
public class AWSIotTimeoutException extends Exception {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new exception object.
     *
     * @param message
     *            the error message
     */
    public AWSIotTimeoutException(String message) {
        super(message);
    }

    /**
     * Instantiates a new exception object.
     *
     * @param cause
     *            the cause. A null value is permitted, and indicates that the
     *            cause is nonexistent or unknown.
     */
    public AWSIotTimeoutException(Throwable cause) {
        super(cause);
    }

}
