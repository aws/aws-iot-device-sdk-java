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
 * The class provides default values for the library. All the values defined
 * here can be overridden at runtime through setter functions in
 * {@link AWSIotMqttClient} and {@link AWSIotDevice}.
 */
public class AWSIotConfig {

    /**
     * The default value for number of client threads. See also
     * {@link AWSIotMqttClient#getNumOfClientThreads()}.
     */
    public static final int NUM_OF_CLIENT_THREADS = 1;

    /**
     * The default value for client connection timeout (milliseconds). See also
     * {@link AWSIotMqttClient#getConnectionTimeout()}.
     */
    public static final int CONNECTION_TIMEOUT = 30000;

    /**
     * The default value for service acknowledge timeout (milliseconds). See
     * also {@link AWSIotMqttClient#getServerAckTimeout()}.
     */
    public static final int SERVER_ACK_TIMEOUT = 3000;

    /**
     * The default value for client keep-alive interval (milliseconds). See also
     * {@link AWSIotMqttClient#getKeepAliveInterval()}.
     */
    public static final int KEEP_ALIVE_INTERVAL = 600000;

    /**
     * The default value for maximum connection retry times. See also
     * {@link AWSIotMqttClient#getMaxConnectionRetries()}.
     */
    public static final int MAX_CONNECTION_RETRIES = 5;

    /**
     * The default value for connection base retry delay (milliseconds). See
     * also {@link AWSIotMqttClient#getBaseRetryDelay()}.
     */
    public static final int CONNECTION_BASE_RETRY_DELAY = 3000;

    /**
     * The default value for connection maximum retry delay (milliseconds). See
     * also {@link AWSIotMqttClient#getMaxRetryDelay()}.
     */
    public static final int CONNECTION_MAX_RETRY_DELAY = 30000;

    /**
     * The default value for maximum offline queue size. See also
     * {@link AWSIotMqttClient#getMaxOfflineQueueSize()}.
     */
    public static final int MAX_OFFLINE_QUEUE_SIZE = 64;

    /**
     * The default value for device reporting interval (milliseconds). See also
     * {@link AWSIotDevice#getReportInterval()}.
     */
    public static final int DEVICE_REPORT_INTERVAL = 3000;

    /**
     * The default value for enabling device update versioning. See also
     * {@link AWSIotDevice#isEnableVersioning()}.
     */
    public static final boolean DEVICE_ENABLE_VERSIONING = false;

    /**
     * The default value for device reporting QoS level. See also
     * {@link AWSIotDevice#getDeviceReportQos()}.
     */
    public static final int DEVICE_REPORT_QOS = 0;

    /**
     * The default value for the QoS level for subscribing to shadow updates.
     * See also {@link AWSIotDevice#getShadowUpdateQos()}.
     */
    public static final int DEVICE_SHADOW_UPDATE_QOS = 0;

    /**
     * The default value for the QoS level for publishing shadow methods. See
     * also {@link AWSIotDevice#getMethodQos()}.
     */
    public static final int DEVICE_METHOD_QOS = 0;

    /**
     * The default value for the QoS level for subscribing to shadow method
     * acknowledgement. See also {@link AWSIotDevice#getMethodAckQos()}.
     */
    public static final int DEVICE_METHOD_ACK_QOS = 0;

}
