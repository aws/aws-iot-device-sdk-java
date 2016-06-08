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

package com.amazonaws.services.iot.client.shadow;

import java.util.logging.Logger;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;

/**
 * This class extends {@link AWSIotTopic} to provide customized callback
 * functions for the subscription requests of the shadow commands.
 */
public class AwsIotDeviceCommandAckListener extends AWSIotTopic {

    private static final Logger LOGGER = Logger.getLogger(AwsIotDeviceCommandAckListener.class.getName());

    private final AbstractAwsIotDevice device;

    public AwsIotDeviceCommandAckListener(String topic, AWSIotQos qos, AbstractAwsIotDevice device) {
        super(topic, qos);
        this.device = device;
    }

    @Override
    public void onMessage(AWSIotMessage message) {
        device.onCommandAck(message);
    }

    @Override
    public void onSuccess() {
        device.onSubscriptionAck(topic, true);
    }

    @Override
    public void onFailure() {
        LOGGER.warning("Failed to subscribe to device topic " + topic);
        device.onSubscriptionAck(topic, false);
    }

    @Override
    public void onTimeout() {
        LOGGER.warning("Timeout when subscribing to device topic " + topic);
        device.onSubscriptionAck(topic, false);
    }

}
