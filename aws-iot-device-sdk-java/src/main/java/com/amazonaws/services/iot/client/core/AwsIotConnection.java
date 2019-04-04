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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.services.iot.client.AWSIotConnectionStatus;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;

import lombok.Getter;
import lombok.Setter;

/**
 * This class provides an abstract layer for the library to communicate with the
 * AWS IoT service without having to directly interact with the actual MQTT
 * implementation. The abstraction layer also provides connection retry logic as
 * well as offline message queuing.
 */
public abstract class AwsIotConnection implements AwsIotConnectionCallback {

    private static final Logger LOGGER = Logger.getLogger(AwsIotConnection.class.getName());

    /**
     * The client the connection is associated with.
     *
     * @return the current client
     */
    @Getter
    protected AbstractAwsIotClient client;

    /**
     * The connection status.
     * 
     * @param connectionStatus
     *            the new connection status
     * @return the current connection status
     */
    @Getter
    @Setter
    protected AWSIotConnectionStatus connectionStatus = AWSIotConnectionStatus.DISCONNECTED;

    /**
     * The future object holding the retry task.
     * 
     * @return the current retry task
     */
    @Getter
    private Future<?> retryTask;

    /**
     * The retry times.
     * 
     * @return the current retry times
     */
    @Getter
    private int retryTimes;

    /**
     * The callback functions for the connect request.
     *
     * @return the current connect callback
     */
    @Getter
    private AwsIotMessageCallback connectCallback;

    /**
     * Flag to indicate user disconnect is in progress.
     *
     * @return the current user disconnect flag
     */
    @Getter
    private boolean userDisconnect;

    /**
     * The offline publish queue holding messages while the connection is being
     * established.
     * 
     * @return the current offline publish queue
     */
    @Getter
    private ConcurrentLinkedQueue<AWSIotMessage> publishQueue = new ConcurrentLinkedQueue<>();

    /**
     * The offline subscribe request queue holding messages while the connection
     * is being established.
     * 
     * @return the current offline subscribe request queue
     */
    @Getter
    private ConcurrentLinkedQueue<AWSIotMessage> subscribeQueue = new ConcurrentLinkedQueue<>();

    /**
     * The offline unsubscribe request queue holding messages while the
     * connection is being established.
     * 
     * @return the current offline unsubscribe request queue
     */
    @Getter
    private ConcurrentLinkedQueue<AWSIotMessage> unsubscribeQueue = new ConcurrentLinkedQueue<>();

    /**
     * Instantiates a new connection object.
     *
     * @param client
     *            the client
     */
    public AwsIotConnection(AbstractAwsIotClient client) {
        this.client = client;
    }

    /**
     * Abstract method which is called to establish an underneath connection.
     *
     * @param callback
     *            connection callback functions
     * @throws AWSIotException
     *             this exception is thrown when the request is failed to be
     *             sent
     */
    protected abstract void openConnection(AwsIotMessageCallback callback) throws AWSIotException;

    /**
     * Abstract method which is called to terminate an underneath connection.
     *
     * @param callback
     *            connection callback functions
     * @throws AWSIotException
     *             this exception is thrown when the request is failed to be
     *             sent
     */
    protected abstract void closeConnection(AwsIotMessageCallback callback) throws AWSIotException;

    /**
     * Abstract method which is called to publish a message.
     *
     * @param message
     *            the message to be published
     * @throws AWSIotException
     *             this exception is thrown when there's an unrecoverable error
     *             happened while processing the request
     * @throws AwsIotRetryableException
     *             this exception is thrown when the request is failed to be
     *             sent, which will be queued and retried
     */
    protected abstract void publishMessage(AWSIotMessage message) throws AWSIotException, AwsIotRetryableException;

    /**
     * Abstract method which is called to subscribe to a topic.
     *
     * @param message
     *            the topic to be subscribed to
     * @throws AWSIotException
     *             this exception is thrown when there's an unrecoverable error
     *             happened while processing the request
     * @throws AwsIotRetryableException
     *             this exception is thrown when the request is failed to be
     *             sent, which will be queued and retried
     */
    protected abstract void subscribeTopic(AWSIotMessage message) throws AWSIotException, AwsIotRetryableException;

    /**
     * Abstract method which is called to unsubscribe to a topic.
     *
     * @param message
     *            the topic to be unsubscribed to
     * @throws AWSIotException
     *             this exception is thrown when there's an unrecoverable error
     *             happened while processing the request
     * @throws AwsIotRetryableException
     *             this exception is thrown when the request is failed to be
     *             sent, which will be queued and retried
     */
    protected abstract void unsubscribeTopic(AWSIotMessage message) throws AWSIotException, AwsIotRetryableException;

    /**
     * The actual publish method exposed by this class.
     *
     * @param message
     *            the message to be published
     * @throws AWSIotException
     *             this exception is thrown when the underneath failed to
     *             process the request
     */
    public void publish(AWSIotMessage message) throws AWSIotException {
        try {
            publishMessage(message);
        } catch (AwsIotRetryableException e) {
            if (client.getMaxOfflineQueueSize() > 0 && publishQueue.size() < client.getMaxOfflineQueueSize()) {
                publishQueue.add(message);
            } else {
                LOGGER.info("Failed to publish message to " + message.getTopic());
                throw new AWSIotException(e);
            }
        }
    }

    /**
     * Updates credentials for the connection, which will be used for new
     * connections.
     *
     * @param awsAccessKeyId
     *            the AWS access key id
     * @param awsSecretAccessKey
     *            the AWS secret access key
     * @param sessionToken
     *            Session token received along with the temporary credentials
     *            from services like STS server, AssumeRole, or Amazon Cognito.
     */
    public void updateCredentials(String awsAccessKeyId, String awsSecretAccessKey, String sessionToken) {
        // default implementation does nothing
    }

    /**
     * The actual subscribe method exposed by this class.
     *
     * @param message
     *            the topic to be subscribed to
     * @throws AWSIotException
     *             this exception is thrown when the underneath failed to
     *             process the request
     */
    public void subscribe(AWSIotMessage message) throws AWSIotException {
        try {
            subscribeTopic(message);
        } catch (AwsIotRetryableException e) {
            if (client.getMaxOfflineQueueSize() > 0 && subscribeQueue.size() < client.getMaxOfflineQueueSize()) {
                subscribeQueue.add(message);
            } else {
                LOGGER.info("Failed to subscribe to " + message.getTopic());
                throw new AWSIotException(e);
            }
        }

    }

    /**
     * The actual unsubscribe method exposed by this class.
     *
     * @param message
     *            the topic to be unsubscribed to
     * @throws AWSIotException
     *             this exception is thrown when the underneath failed to
     *             process the request
     */
    public void unsubscribe(AWSIotMessage message) throws AWSIotException {
        try {
            unsubscribeTopic(message);
        } catch (AwsIotRetryableException e) {
            if (client.getMaxOfflineQueueSize() > 0 && unsubscribeQueue.size() < client.getMaxOfflineQueueSize()) {
                unsubscribeQueue.add(message);
            } else {
                LOGGER.info("Failed to unsubscribe to " + message.getTopic());
                throw new AWSIotException(e);
            }
        }

    }

    /**
     * The actual connect method exposed by this class.
     *
     * @param callback
     *            user callback functions
     * @throws AWSIotException
     *             this exception is thrown when the underneath layer failed to
     *             process the request
     */
    public void connect(AwsIotMessageCallback callback) throws AWSIotException {
        cancelRetry();

        retryTimes = 0;
        userDisconnect = false;
        connectCallback = callback;

        openConnection(null);
    }

    /**
     * The actual disconnect method exposed by this class.
     * 
     * @param callback
     *            user callback functions
     * @throws AWSIotException
     *             this exception is thrown when the underneath layer failed to
     *             process the request
     */
    public void disconnect(AwsIotMessageCallback callback) throws AWSIotException {
        cancelRetry();

        retryTimes = 0;
        userDisconnect = true;
        connectCallback = null;

        closeConnection(callback);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.amazonaws.services.iot.client.core.AwsIotConnectionCallback#
     * onConnectionSuccess()
     */
    @Override
    public void onConnectionSuccess() {
        LOGGER.info("Connection successfully established");

        connectionStatus = AWSIotConnectionStatus.CONNECTED;
        retryTimes = 0;

        cancelRetry();

        // process offline messages
        try {
            while (subscribeQueue.size() > 0) {
                AWSIotMessage message = subscribeQueue.poll();
                subscribeTopic(message);
            }
            while (unsubscribeQueue.size() > 0) {
                AWSIotMessage message = unsubscribeQueue.poll();
                unsubscribeTopic(message);
            }
            while (publishQueue.size() > 0) {
                AWSIotMessage message = publishQueue.poll();
                publishMessage(message);
            }
        } catch (AWSIotException | AwsIotRetryableException e) {
            // should close the connection if we can't send message when
            // connection is good
            LOGGER.log(Level.WARNING, "Failed to send queued messages, will disconnect", e);
            try {
                closeConnection(null);
            } catch (AWSIotException ie) {
                LOGGER.log(Level.WARNING, "Failed to disconnect", ie);
            }
        }

        client.onConnectionSuccess();

        if (connectCallback != null) {
            connectCallback.onSuccess();
            connectCallback = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.amazonaws.services.iot.client.core.AwsIotConnectionCallback#
     * onConnectionFailure()
     */
    @Override
    public void onConnectionFailure() {
        LOGGER.info("Connection temporarily lost");

        connectionStatus = AWSIotConnectionStatus.DISCONNECTED;

        cancelRetry();

        if (shouldRetry()) {
            retryConnection();
            client.onConnectionFailure();
        } else {
            // permanent failure, notify the client and no more retries
            LOGGER.info("Connection retry cancelled or exceeded maximum retries");
            if (connectCallback != null) {
                connectCallback.onFailure();
                connectCallback = null;
            }

            client.onConnectionClosed();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.amazonaws.services.iot.client.core.AwsIotConnectionCallback#
     * onConnectionClosed()
     */
    @Override
    public void onConnectionClosed() {
        LOGGER.info("Connection permanently closed");

        connectionStatus = AWSIotConnectionStatus.DISCONNECTED;

        cancelRetry();

        if (connectCallback != null) {
            connectCallback.onFailure();
            connectCallback = null;
        }

        client.onConnectionClosed();
    }

    /**
     * Whether or not to reestablish the connection.
     *
     * @return true, if successful
     */
    private boolean shouldRetry() {
        return (!userDisconnect && (client.getMaxConnectionRetries() > 0 && retryTimes < client
                .getMaxConnectionRetries()));
    }

    /**
     * Cancel any pending retry request.
     */
    private void cancelRetry() {
        if (retryTask != null) {
            retryTask.cancel(false);
            retryTask = null;
        }
    }

    /**
     * Gets the exponentially back-off retry delay based on the number of times
     * the connection has been retried.
     *
     * @return the retry delay
     */
    long getRetryDelay() {
        double delay = Math.pow(2.0, retryTimes) * client.getBaseRetryDelay();
        delay = Math.min(delay, (double)client.getMaxRetryDelay());
        delay = Math.max(delay, 0.0);
        return (long)delay;
    }

    /**
     * Schedule retry task so the connection can be retried after the timeout
     */
    private void retryConnection() {
        if (retryTask != null) {
            LOGGER.warning("Connection retry already in progress");
            // retry task already scheduled, do nothing
            return;
        }

        retryTask = client.scheduleTimeoutTask(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Connection is being retried");

                connectionStatus = AWSIotConnectionStatus.RECONNECTING;
                retryTimes++;
                try {
                    openConnection(null);
                } catch (AWSIotException e) {
                    // permanent failure, notify the client and no more retries
                    client.onConnectionClosed();
                }
            }
        }, getRetryDelay());
    }

}
