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

import com.amazonaws.services.iot.client.core.AwsIotTopicCallback;

/**
 * This class is used for subscribing to a topic in the subscription APIs, such
 * as {@link AWSIotMqttClient#subscribe(AWSIotTopic topic)}.
 * <p>
 * In contains a callback function, {@link #onMessage}, that is invoked when a
 * subscribed message has arrived. In most cases, applications are expected to
 * override the default {@link #onMessage} method in order to access the message
 * payload.
 * </p>
 * <p>
 * This class extends {@link AWSIotMessage}, therefore callback functions in
 * {@link AWSIotMessage} can also be overridden if the application wishes to be
 * invoked for the outcomes of the subscription API. For more details, please
 * refer to {@link AWSIotMessage}.
 * </p>
 */
public class AWSIotTopic extends AWSIotMessage implements AwsIotTopicCallback {

    /**
     * Instantiates a new topic object.
     *
     * @param topic
     *            the topic to be subscribed to
     */
    public AWSIotTopic(String topic) {
        super(topic, AWSIotQos.QOS0);
    }

    /**
     * Instantiates a new topic object.
     *
     * @param topic
     *            the topic to be subscribed to
     * @param qos
     *            the MQTT QoS level for the subscription
     */
    public AWSIotTopic(String topic, AWSIotQos qos) {
        super(topic, qos);
    }

    /**
     * Callback function to be invoked upon the arrival of a subscribed message.
     *
     * @param message
     *            the message received
     */
    @Override
    public void onMessage(AWSIotMessage message) {
        // Default callback implementation is no-op
    }

}
