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

import java.util.HashSet;
import java.util.Set;

import javax.net.SocketFactory;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;
import com.amazonaws.services.iot.client.core.AwsIotConnection;
import com.amazonaws.services.iot.client.core.AwsIotMessageCallback;
import com.amazonaws.services.iot.client.core.AwsIotRetryableException;

import lombok.Getter;
import lombok.Setter;

/**
 * This class extends {@link AwsIotConnection} to provide the basic MQTT pub/sub
 * functionalities using the Paho MQTT library.
 */
@Getter
@Setter
public class AwsIotMqttConnection extends AwsIotConnection {

    // Release Script will replace the version string on release. Refer to codebuild/cd/promote-release.yml
    private static final String USERNAME_METRIC_STRING = "?SDK=Java&Version=0.0.1-dev";
    private final SocketFactory socketFactory;

    private MqttAsyncClient mqttClient;
    private AwsIotMqttMessageListener messageListener;
    private AwsIotMqttClientListener clientListener;

    public AwsIotMqttConnection(AbstractAwsIotClient client, SocketFactory socketFactory, String serverUri)
            throws AWSIotException {
        super(client);

        this.socketFactory = socketFactory;

        messageListener = new AwsIotMqttMessageListener(client);
        clientListener = new AwsIotMqttClientListener(client);

        try {
            mqttClient = new MqttAsyncClient(serverUri, client.getClientId(), new MemoryPersistence());
            mqttClient.setCallback(clientListener);
        } catch (MqttException e) {
            throw new AWSIotException(e);
        }
    }

    AwsIotMqttConnection(AbstractAwsIotClient client, MqttAsyncClient mqttClient) throws AWSIotException {
        super(client);
        this.mqttClient = mqttClient;
        this.socketFactory = null;
    }

    public void openConnection(AwsIotMessageCallback callback) throws AWSIotException {
        try {
            AwsIotMqttConnectionListener connectionListener = new AwsIotMqttConnectionListener(client, true, callback);
            MqttConnectOptions options = buildMqttConnectOptions(client, socketFactory);
            mqttClient.connect(options, null, connectionListener);
        } catch (MqttException e) {
            throw new AWSIotException(e);
        }
    }

    public void closeConnection(AwsIotMessageCallback callback) throws AWSIotException {
        try {
            AwsIotMqttConnectionListener connectionListener = new AwsIotMqttConnectionListener(client, false, callback);
            mqttClient.disconnect(0, null, connectionListener);
        } catch (MqttException e) {
            throw new AWSIotException(e);
        }
    }

    @Override
    public void publishMessage(AWSIotMessage message) throws AWSIotException, AwsIotRetryableException {
        String topic = message.getTopic();
        MqttMessage mqttMessage = new MqttMessage(message.getPayload());
        mqttMessage.setQos(message.getQos().getValue());

        try {
            mqttClient.publish(topic, mqttMessage, message, messageListener);
        } catch (MqttException e) {
            if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_NOT_CONNECTED) {
                throw new AwsIotRetryableException(e);
            } else {
                throw new AWSIotException(e);
            }
        }
    }

    @Override
    public void subscribeTopic(AWSIotMessage message) throws AWSIotException, AwsIotRetryableException {
        try {
            mqttClient.subscribe(message.getTopic(), message.getQos().getValue(), message, messageListener);
        } catch (MqttException e) {
            if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_NOT_CONNECTED) {
                throw new AwsIotRetryableException(e);
            } else {
                throw new AWSIotException(e);
            }
        }
    }

    @Override
    public void unsubscribeTopic(AWSIotMessage message) throws AWSIotException, AwsIotRetryableException {
        try {
            mqttClient.unsubscribe(message.getTopic(), message, messageListener);
        } catch (MqttException e) {
            if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_NOT_CONNECTED) {
                throw new AwsIotRetryableException(e);
            } else {
                throw new AWSIotException(e);
            }
        }
    }

    public Set<String> getServerUris() {
        return new HashSet<>();
    }

    private MqttConnectOptions buildMqttConnectOptions(AbstractAwsIotClient client, SocketFactory socketFactory) {
        MqttConnectOptions options = new MqttConnectOptions();

        options.setSocketFactory(socketFactory);
        options.setCleanSession(client.isCleanSession());
        options.setConnectionTimeout(client.getConnectionTimeout() / 1000);
        options.setKeepAliveInterval(client.getKeepAliveInterval() / 1000);
        if(client.isClientEnableMetrics()) {
            options.setUserName(USERNAME_METRIC_STRING);
        }

        Set<String> serverUris = getServerUris();
        if (serverUris != null && !serverUris.isEmpty()) {
            String[] uriArray = new String[serverUris.size()];
            serverUris.toArray(uriArray);
            options.setServerURIs(uriArray);
        }

        if (client.getWillMessage() != null) {
            AWSIotMessage message = client.getWillMessage();

            options.setWill(message.getTopic(), message.getPayload(), message.getQos().getValue(), false);
        }

        return options;
    }

}
