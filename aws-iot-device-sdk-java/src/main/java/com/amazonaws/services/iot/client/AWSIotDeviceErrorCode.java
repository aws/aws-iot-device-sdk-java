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
 * These error codes are used by the server in acknowledgement message for the
 * shadow methods, namely Get, Update, and Delete.
 */
public enum AWSIotDeviceErrorCode {

    /** The bad request. */
    BAD_REQUEST(400),
    /** The Unauthorized. */
    UNAUTHORIZED(401),
    /** The Forbidden. */
    FORBIDDEN(403),
    /** The Not found. */
    NOT_FOUND(404),
    /** The Conflict. */
    CONFLICT(409),
    /** The Payload too large. */
    PAYLOAD_TOO_LARGE(413),
    /** The Unsupported media type. */
    UNSUPPORTED_MEDIA_TYPE(415),
    /** The Too many requests. */
    TOO_MANY_REQUESTS(429),
    /** The Internal service failure. */
    INTERNAL_SERVICE_FAILURE(429);

    /** The error code. */
    private final long errorCode;

    /**
     * Instantiates a new device error code object.
     *
     * @param errorCode
     *            the error code
     */
    private AWSIotDeviceErrorCode(final long errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Gets the error code value.
     *
     * @return the error code value
     */
    public long getValue() {
        return this.errorCode;
    }

    /**
     * Returns the Enum representation of the error code value
     *
     * @param code
     *            the error code value
     * @return the Enum representation of the error code, or null if the error
     *         code is unknown
     */
    public static AWSIotDeviceErrorCode valueOf(long code) {
        for (AWSIotDeviceErrorCode errorCode : AWSIotDeviceErrorCode.values()) {
            if (errorCode.errorCode == code) {
                return errorCode;
            }
        }

        return null;
    }

}
