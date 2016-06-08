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

package com.amazonaws.services.iot.client.mqtt;

import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSuback;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;

/**
 * This class implements listener functions for the message events from the Paho
 * MQTT library.
 */
public class AwsIotMqttMessageListener implements IMqttActionListener {

    private static final Logger LOGGER = Logger.getLogger(AwsIotMqttMessageListener.class.getName());

    private static final int SUB_ACK_RETURN_CODE_FAILURE = 0x80;

    private AbstractAwsIotClient client;

    public AwsIotMqttMessageListener(AbstractAwsIotClient client) {
        this.client = client;
    }

    @Override
    public void onSuccess(IMqttToken token) {
        final AWSIotMessage message = (AWSIotMessage) token.getUserContext();
        if (message == null) {
            return;
        }

        boolean forceFailure = false;
        if (token.getResponse() instanceof MqttSuback) {
            MqttSuback subAck = (MqttSuback) token.getResponse();
            int qos[] = subAck.getGrantedQos();
            for (int i = 0; i < qos.length; i++) {
                if (qos[i] == SUB_ACK_RETURN_CODE_FAILURE) {
                    LOGGER.warning("Request failed: likely due to too many subscriptions or policy violations");
                    forceFailure = true;
                    break;
                }
            }
        }

        final boolean isSuccess = !forceFailure;
        client.scheduleTask(new Runnable() {
            @Override
            public void run() {
                if (isSuccess) {
                    message.onSuccess();
                } else {
                    message.onFailure();
                }
            }
        });
    }

    @Override
    public void onFailure(IMqttToken token, Throwable cause) {
        final AWSIotMessage message = (AWSIotMessage) token.getUserContext();
        if (message == null) {
            LOGGER.warning("Request failed: " + token.getException());
            return;
        }

        LOGGER.warning("Request failed for topic " + message.getTopic() + ": " + token.getException());
        client.scheduleTask(new Runnable() {
            @Override
            public void run() {
                message.onFailure();
            }
        });
    }

}
