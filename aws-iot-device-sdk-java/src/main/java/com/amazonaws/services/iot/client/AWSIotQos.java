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
 * QoS definitions. The AWS IoT service supports QoS0 and QoS1 defined by the
 * MQTT protocol.
 */
public enum AWSIotQos {

    /** The QoS0. */
    QOS0(0),

    /** The QoS1. */
    QOS1(1);

    /** The qos. */
    private final int qos;

    /**
     * Instantiates a QoS object.
     *
     * @param qos
     *            the QoS level
     */
    private AWSIotQos(final int qos) {
        this.qos = qos;
    }

    /**
     * Gets the integer representation of the QoS
     *
     * @return the integer value of the QoS
     */
    public int getValue() {
        return this.qos;
    }

    /**
     * Gets the Enum representation of the QoS
     *
     * @param qos
     *            the integer value of the QoS
     * @return the Enum value of the QoS
     */
    public static AWSIotQos valueOf(int qos) {
        if (qos == 0) {
            return QOS0;
        } else if (qos == 1) {
            return QOS1;
        } else {
            throw new IllegalArgumentException("QoS not supported");
        }
    }

}
