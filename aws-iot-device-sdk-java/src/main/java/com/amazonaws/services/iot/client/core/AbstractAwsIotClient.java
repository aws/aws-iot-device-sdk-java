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

package com.amazonaws.services.iot.client.core;

import java.security.KeyStore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocketFactory;

import com.amazonaws.services.iot.client.auth.CredentialsProvider;
import com.amazonaws.services.iot.client.AWSIotConfig;
import com.amazonaws.services.iot.client.AWSIotConnectionStatus;
import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.amazonaws.services.iot.client.shadow.AbstractAwsIotDevice;

import lombok.Getter;
import lombok.Setter;

/**
 * The actual implementation of {@code AWSIotMqttClient}.
 */
@Getter
@Setter
public abstract class AbstractAwsIotClient implements AwsIotConnectionCallback {

    private static final int DEFAULT_MQTT_PORT = 8883;

    private static final Logger LOGGER = Logger.getLogger(AbstractAwsIotClient.class.getName());

    protected final String clientId;
    protected final String clientEndpoint;
    protected final boolean clientEnableMetrics;
    protected final AwsIotConnectionType connectionType;

    protected int port = DEFAULT_MQTT_PORT;
    protected int numOfClientThreads = AWSIotConfig.NUM_OF_CLIENT_THREADS;
    protected int connectionTimeout = AWSIotConfig.CONNECTION_TIMEOUT;
    protected int serverAckTimeout = AWSIotConfig.SERVER_ACK_TIMEOUT;
    protected int keepAliveInterval = AWSIotConfig.KEEP_ALIVE_INTERVAL;
    protected int maxConnectionRetries = AWSIotConfig.MAX_CONNECTION_RETRIES;
    protected int baseRetryDelay = AWSIotConfig.CONNECTION_BASE_RETRY_DELAY;
    protected int maxRetryDelay = AWSIotConfig.CONNECTION_MAX_RETRY_DELAY;
    protected int maxOfflineQueueSize = AWSIotConfig.MAX_OFFLINE_QUEUE_SIZE;
    protected boolean cleanSession = AWSIotConfig.CLEAN_SESSION;
    protected AWSIotMessage willMessage;

    private final ConcurrentMap<String, AWSIotTopic> subscriptions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AbstractAwsIotDevice> devices = new ConcurrentHashMap<>();
    private final AwsIotConnection connection;

    private ScheduledExecutorService executionService;

    protected AbstractAwsIotClient(String clientEndpoint, String clientId, KeyStore keyStore, String keyPassword,
                                   boolean enableSdkMetrics) {
        this.clientEndpoint = clientEndpoint;
        this.clientId = clientId;
        this.connectionType = AwsIotConnectionType.MQTT_OVER_TLS;
        this.clientEnableMetrics = enableSdkMetrics;

        try {
            connection = new AwsIotTlsConnection(this, keyStore, keyPassword);
        } catch (AWSIotException e) {
            throw new AwsIotRuntimeException(e);
        }
    }
    
    protected AbstractAwsIotClient(String clientEndpoint, String clientId, KeyStore keyStore, String keyPassword) {
        // Enable Metrics by default
        this(clientEndpoint, clientId, keyStore, keyPassword, true);
    }

    protected AbstractAwsIotClient(String clientEndpoint, String clientId, String awsAccessKeyId,
                                   String awsSecretAccessKey, String sessionToken, boolean enableSdkMetrics) {
        //setting the region blank to ensure it's determined from the clientEndpoint
        this(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey, sessionToken, "", enableSdkMetrics);
    }

    protected AbstractAwsIotClient(String clientEndpoint, String clientId, String awsAccessKeyId,
                                   String awsSecretAccessKey, String sessionToken,
                                   String region, boolean enableSdkMetrics) {
        this.clientEndpoint = clientEndpoint;
        this.clientId = clientId;
        this.connectionType = AwsIotConnectionType.MQTT_OVER_WEBSOCKET;
        this.clientEnableMetrics = enableSdkMetrics;

        try {
            connection = new AwsIotWebsocketConnection(this, awsAccessKeyId, awsSecretAccessKey, sessionToken, region);
        } catch (AWSIotException e) {
            throw new AwsIotRuntimeException(e);
        }
    }

    protected AbstractAwsIotClient(String clientEndpoint, String clientId, String awsAccessKeyId,
                                   String awsSecretAccessKey, String sessionToken) {
        // Enable Metrics by default
        this(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey, sessionToken, true);
    }

    protected AbstractAwsIotClient(String clientEndpoint, String clientId, String awsAccessKeyId,
                                   String awsSecretAccessKey, String sessionToken, String region) {
        // Enable Metrics by default
        this(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey, sessionToken, region, true);
    }

    protected AbstractAwsIotClient(String clientEndpoint, String clientId, CredentialsProvider provider, String region) {
        this(clientEndpoint, clientId, provider, region, true);
    }

    protected AbstractAwsIotClient(String clientEndpoint, String clientId, CredentialsProvider provider, String region, boolean enableSdkMetrics) {
        this.clientEndpoint = clientEndpoint;
        this.clientId = clientId;
        this.connectionType = AwsIotConnectionType.MQTT_OVER_WEBSOCKET;
        this.clientEnableMetrics = enableSdkMetrics;

        try {
            connection = new AwsIotWebsocketConnection(this, provider, region);
        } catch (AWSIotException e) {
            throw new AwsIotRuntimeException(e);
        }
    }

    AbstractAwsIotClient(String clientEndpoint, String clientId, AwsIotConnection connection,
                         boolean enableSdkMetrics) {
        this.clientEndpoint = clientEndpoint;
        this.clientId = clientId;
        this.connection = connection;
        this.connectionType = null;
        this.clientEnableMetrics = enableSdkMetrics;
    }

    AbstractAwsIotClient(String clientEndpoint, String clientId, AwsIotConnection connection) {
        // Enable Metrics by default
        this(clientEndpoint, clientId, connection, true);
    }

    protected AbstractAwsIotClient(String clientEndpoint, String clientId, SSLSocketFactory socketFactory, boolean enableSdkMetrics) {
        this.clientEndpoint = clientEndpoint;
        this.clientId = clientId;
        this.connectionType = null;
        this.clientEnableMetrics = enableSdkMetrics;

        try {
            this.connection = new AwsIotTlsConnection(this, socketFactory);
        } catch (AWSIotException e) {
            throw new AwsIotRuntimeException(e);
        }
    }

    protected AbstractAwsIotClient(String clientEndpoint, String clientId, SSLSocketFactory socketFactory) {
        this(clientEndpoint, clientId, socketFactory, true);
    }

    protected AbstractAwsIotClient(String clientEndpoint, String clientId, SSLSocketFactory socketFactory, int port, boolean enableSdkMetrics) {
        this.clientEndpoint = clientEndpoint;
        this.clientId = clientId;
        this.connectionType = AwsIotConnectionType.MQTT_OVER_TLS;
        this.port = port;
        this.clientEnableMetrics = enableSdkMetrics;

        try {
            this.connection = new AwsIotTlsConnection(this, socketFactory);
        } catch (AWSIotException e) {
            throw new AwsIotRuntimeException(e);
        }
    }

    protected AbstractAwsIotClient(String clientEndpoint, String clientId, SSLSocketFactory socketFactory, int port) {
        this(clientEndpoint, clientId, socketFactory, port, true);
    }

    public void updateCredentials(String awsAccessKeyId, String awsSecretAccessKey, String sessionToken) {
        this.connection.updateCredentials(awsAccessKeyId, awsSecretAccessKey, sessionToken);
    }

    public void connect() throws AWSIotException {
        try {
            connect(0, true);
        } catch (AWSIotTimeoutException e) {
            // We shouldn't get timeout exception because timeout is 0
            throw new AwsIotRuntimeException(e);
        }
    }

    public void connect(long timeout) throws AWSIotException, AWSIotTimeoutException {
        connect(timeout, true);
    }

    public void connect(long timeout, boolean blocking) throws AWSIotException, AWSIotTimeoutException {
        synchronized (this) {
            if (executionService == null) {
                executionService = Executors.newScheduledThreadPool(numOfClientThreads);
            }
        }

        AwsIotCompletion completion = new AwsIotCompletion(timeout, !blocking);
        connection.connect(completion);
        completion.get(this);
    }

    public void disconnect() throws AWSIotException {
        try {
            disconnect(0, true);
        } catch (AWSIotTimeoutException e) {
            // We shouldn't get timeout exception because timeout is 0
            throw new AwsIotRuntimeException(e);
        }
    }

    public void disconnect(long timeout) throws AWSIotException, AWSIotTimeoutException {
        disconnect(timeout, true);
    }

    public void disconnect(long timeout, boolean blocking) throws AWSIotException, AWSIotTimeoutException {
        AwsIotCompletion completion = new AwsIotCompletion(timeout, !blocking);
        connection.disconnect(completion);
        completion.get(this);
    }

    public void publish(String topic, String payload) throws AWSIotException {
        publish(topic, AWSIotQos.QOS0, payload);
    }

    public void publish(String topic, String payload, long timeout) throws AWSIotException, AWSIotTimeoutException {
        publish(topic, AWSIotQos.QOS0, payload, timeout);
    }

    public void publish(String topic, AWSIotQos qos, String payload) throws AWSIotException {
        try {
            publish(topic, qos, payload, 0);
        } catch (AWSIotTimeoutException e) {
            // We shouldn't get timeout exception because timeout is 0
            throw new AwsIotRuntimeException(e);
        }
    }

    public void publish(String topic, AWSIotQos qos, String payload, long timeout)
            throws AWSIotException, AWSIotTimeoutException {
        AwsIotCompletion completion = new AwsIotCompletion(topic, qos, payload, timeout);
        connection.publish(completion);
        completion.get(this);
    }

    public void publish(String topic, byte[] payload) throws AWSIotException {
        publish(topic, AWSIotQos.QOS0, payload);
    }

    public void publish(String topic, byte[] payload, long timeout) throws AWSIotException, AWSIotTimeoutException {
        publish(topic, AWSIotQos.QOS0, payload, timeout);
    }

    public void publish(String topic, AWSIotQos qos, byte[] payload) throws AWSIotException {
        try {
            publish(topic, qos, payload, 0);
        } catch (AWSIotTimeoutException e) {
            // We shouldn't get timeout exception because timeout is 0
            throw new AwsIotRuntimeException(e);
        }
    }

    public void publish(String topic, AWSIotQos qos, byte[] payload, long timeout)
            throws AWSIotException, AWSIotTimeoutException {
        AwsIotCompletion completion = new AwsIotCompletion(topic, qos, payload, timeout);
        connection.publish(completion);
        completion.get(this);
    }

    public void publish(AWSIotMessage message) throws AWSIotException {
        publish(message, 0);
    }

    public void publish(AWSIotMessage message, long timeout) throws AWSIotException {
        AwsIotCompletion completion = new AwsIotCompletion(message, timeout, true);
        connection.publish(completion);
        try {
            completion.get(this);
        } catch (AWSIotTimeoutException e) {
            // We shouldn't get timeout exception because it's asynchronous call
            throw new AwsIotRuntimeException(e);
        }
    }

    public void subscribe(AWSIotTopic topic, boolean blocking) throws AWSIotException {
        try {
            _subscribe(topic, 0, !blocking);
        } catch (AWSIotTimeoutException e) {
            // We shouldn't get timeout exception because timeout is 0
            throw new AwsIotRuntimeException(e);
        }
    }

    public void subscribe(AWSIotTopic topic, long timeout, boolean blocking)
            throws AWSIotException, AWSIotTimeoutException {
        _subscribe(topic, timeout, !blocking);
    }

    public void subscribe(AWSIotTopic topic) throws AWSIotException {
        subscribe(topic, 0);
    }

    public void subscribe(AWSIotTopic topic, long timeout) throws AWSIotException {
        try {
            _subscribe(topic, timeout, true);
        } catch (AWSIotTimeoutException e) {
            // We shouldn't get timeout exception because it's asynchronous call
            throw new AwsIotRuntimeException(e);
        }
    }

    private void _subscribe(AWSIotTopic topic, long timeout, boolean async)
            throws AWSIotException, AWSIotTimeoutException {
        AwsIotCompletion completion = new AwsIotCompletion(topic, timeout, async);
        connection.subscribe(completion);
        completion.get(this);

        subscriptions.put(topic.getTopic(), topic);
    }

    public void unsubscribe(String topic) throws AWSIotException {
        try {
            unsubscribe(topic, 0);
        } catch (AWSIotTimeoutException e) {
            // We shouldn't get timeout exception because timeout is 0
            throw new AwsIotRuntimeException(e);
        }
    }

    public void unsubscribe(String topic, long timeout) throws AWSIotException, AWSIotTimeoutException {
        if (subscriptions.remove(topic) == null) {
            return;
        }

        AwsIotCompletion completion = new AwsIotCompletion(topic, AWSIotQos.QOS0, timeout);
        connection.unsubscribe(completion);
        completion.get(this);
    }

    public void unsubscribe(AWSIotTopic topic) throws AWSIotException {
        unsubscribe(topic, 0);
    }

    public void unsubscribe(AWSIotTopic topic, long timeout) throws AWSIotException {
        if (subscriptions.remove(topic.getTopic()) == null) {
            return;
        }

        AwsIotCompletion completion = new AwsIotCompletion(topic, timeout, true);
        connection.unsubscribe(completion);
        try {
            completion.get(this);
        } catch (AWSIotTimeoutException e) {
            // We shouldn't get timeout exception because it's asynchronous call
            throw new AwsIotRuntimeException(e);
        }
    }

    public boolean topicFilterMatch(String topicFilter, String topic) {
        if (topicFilter == null || topic == null) {
            return false;
        }

        String[] filterTokens = topicFilter.split("/");
        String[] topicTokens = topic.split("/");
        if (filterTokens.length > topicTokens.length) {
            return false;
        }

        for (int i = 0; i < filterTokens.length; i++) {
            if (filterTokens[i].equals("#")) {
                // '#' must be the last character
                return ((i + 1) == filterTokens.length);
            }

            if (!(filterTokens[i].equals(topicTokens[i]) || filterTokens[i].equals("+"))) {
                return false;
            }
        }

        return (filterTokens.length == topicTokens.length);
    }

    public void dispatch(final AWSIotMessage message) {
        boolean matches = false;

        for (String topicFilter : subscriptions.keySet()) {
            if (topicFilterMatch(topicFilter, message.getTopic())) {
                final AWSIotTopic topic = subscriptions.get(topicFilter);
                scheduleTask(new Runnable() {
                    @Override
                    public void run() {
                        topic.onMessage(message);
                    }
                });
                matches = true;
            }
        }

        if (!matches) {
            LOGGER.warning("Unexpected message received from topic " + message.getTopic());
        }
    }

    public void attach(AWSIotDevice device) throws AWSIotException {
        if (devices.putIfAbsent(device.getThingName(), device) != null) {
            return;
        }

        device.setClient(this);

        // start the shadow sync task if the connection is already established
        if (getConnectionStatus().equals(AWSIotConnectionStatus.CONNECTED)) {
            device.activate();
        }
    }

    public void detach(AWSIotDevice device) throws AWSIotException {
        if (devices.remove(device.getThingName()) == null) {
            return;
        }

        device.deactivate();
    }

    public AWSIotConnectionStatus getConnectionStatus() {
        if (connection != null) {
            return connection.getConnectionStatus();
        } else {
            return AWSIotConnectionStatus.DISCONNECTED;
        }
    }

    @Override
    public void onConnectionSuccess() {
        LOGGER.info("Client connection active: " + clientId);

        try {
            // resubscribe all the subscriptions
            for (AWSIotTopic topic : subscriptions.values()) {
                subscribe(topic, serverAckTimeout);
            }

            // start device sync
            for (AbstractAwsIotDevice device : devices.values()) {
                device.activate();
            }
        } catch (AWSIotException e) {
            // connection couldn't be fully recovered, disconnecting
            LOGGER.warning("Failed to complete subscriptions while client is active, will disconnect");
            try {
                connection.disconnect(null);
            } catch (AWSIotException de) {
                // ignore disconnect errors
            }
        }
    }

    @Override
    public void onConnectionFailure() {
        LOGGER.info("Client connection lost: " + clientId);

        // stop device sync
        for (AbstractAwsIotDevice device : devices.values()) {
            try {
                device.deactivate();
            } catch (AWSIotException e) {
                // ignore errors from deactivate() as the connection is lost
                LOGGER.warning("Failed to deactive all the devices, ignoring the error");
            }
        }
    }

    @Override
    public void onConnectionClosed() {
        LOGGER.info("Client connection closed: " + clientId);

        // stop device sync
        for (AbstractAwsIotDevice device : devices.values()) {
            try {
                device.deactivate();
            } catch (AWSIotException e) {
                // ignore errors from deactivate() as the connection is lost
                LOGGER.warning("Failed to deactive all the devices, ignoring the error");
            }
        }

        subscriptions.clear();
        devices.clear();

        executionService.shutdown();
        executionService = null;
    }

    public Future<?> scheduleTask(Runnable runnable) {
        return scheduleTimeoutTask(runnable, 0);
    }

    public Future<?> scheduleTimeoutTask(Runnable runnable, long timeout) {
        if (executionService == null) {
            throw new AwsIotRuntimeException("Client is not connected");
        }
        return executionService.schedule(runnable, timeout, TimeUnit.MILLISECONDS);
    }

    public Future<?> scheduleRoutineTask(Runnable runnable, long initialDelay, long period) {
        if (executionService == null) {
            throw new AwsIotRuntimeException("Client is not connected");
        }
        return executionService.scheduleAtFixedRate(runnable, initialDelay, period, TimeUnit.MILLISECONDS);
    }

}
