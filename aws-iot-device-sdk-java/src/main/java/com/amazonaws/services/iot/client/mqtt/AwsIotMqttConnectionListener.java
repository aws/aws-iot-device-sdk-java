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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;
import com.amazonaws.services.iot.client.core.AwsIotMessageCallback;

/**
 * This class implements listener functions for the connection events from the
 * Paho MQTT library.
 */
public class AwsIotMqttConnectionListener implements IMqttActionListener {

    private static final Logger LOGGER = Logger.getLogger(AwsIotMqttConnectionListener.class.getName());

    private final AbstractAwsIotClient client;
    private final boolean isConnect;
    private final AwsIotMessageCallback userCallback;

    public AwsIotMqttConnectionListener(AbstractAwsIotClient client, boolean isConnect,
            AwsIotMessageCallback userCallback) {
        this.client = client;
        this.isConnect = isConnect;
        this.userCallback = userCallback;
    }

    @Override
    public void onSuccess(IMqttToken arg0) {
        client.scheduleTask(new Runnable() {
            @Override
            public void run() {
                if (isConnect) {
                    client.getConnection().onConnectionSuccess();
                } else {
                    client.getConnection().onConnectionClosed();
                }
                if (userCallback != null) {
                    userCallback.onSuccess();
                }
            }
        });
    }

    @Override
    public void onFailure(IMqttToken arg0, Throwable arg1) {
        LOGGER.log(Level.WARNING, (isConnect ? "Connect" : "Disconnect") + " request failure", arg1);

        client.scheduleTask(new Runnable() {
            @Override
            public void run() {
                if (isConnect) {
                    client.getConnection().onConnectionFailure();
                } else {
                    client.getConnection().onConnectionClosed();
                }
                if (userCallback != null) {
                    userCallback.onFailure();
                }
            }
        });
    }

}
