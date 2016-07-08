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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;

/**
 * This class implements listener functions for client related events from the
 * Paho MQTT library.
 */
public class AwsIotMqttClientListener implements MqttCallback {

    private AbstractAwsIotClient client;

    public AwsIotMqttClientListener(AbstractAwsIotClient client) {
        this.client = client;
    }

    @Override
    public void connectionLost(Throwable arg0) {
        client.scheduleTask(new Runnable() {
            @Override
            public void run() {
                client.getConnection().onConnectionFailure();
            }
        });
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0) {
        // Callback is not used
    }

    @Override
    public void messageArrived(String topic, MqttMessage arg1) throws Exception {
        AWSIotMessage message = new AWSIotMessage(topic, AWSIotQos.valueOf(arg1.getQos()), arg1.getPayload());
        client.dispatch(message);
    }

}
