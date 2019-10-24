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

package com.amazonaws.services.iot.client.greengrass;

/**
 * Base exception for all exceptions thrown while discovery Greengrass Core
 */
public class DiscoveryException extends RuntimeException {

    /**
     * Constructs a new DiscoveryException
     */
    public DiscoveryException() {
    }

    /**
     * Constructs a new DiscoveryException with the specified error message.
     *
     * @param message
     *            describes the error encountered.
     */
    public DiscoveryException(String message) {
        super(message);
    }

    /**
     * Constructs a new DiscoveryException with the specified error message
     * and cause.
     *
     * @param message
     *            describes the error encountered.
     * @param cause
     *            the cause.
     */
    public DiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }

}
