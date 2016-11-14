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

import java.security.KeyStore;

import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;

/**
 * This class is the main interface of the AWS IoT Java library. It provides
 * both blocking and non-blocking methods for interacting with AWS IoT services
 * over the MQTT protocol. With this client, one can directly publish messages
 * to the AWS IoT service and subscribe or unsubscribe to any pub/sub topics.
 * When using this class in conjunction with {@link AWSIotDevice}, one can
 * easily access AWS IoT device shadows in the cloud, and keep them in sync with
 * the real devices.
 * <p>
 * There are two types of connections this SDK supports to connect to the AWS
 * IoT service:
 * </p>
 * <ul>
 * <li>MQTT (over TLS 1.2) with X.509 certificate based mutual authentication</li>
 * <li>MQTT over Secure WebSocket with AWS SigV4 authentication</li>
 * </ul>
 * <p>
 * For MQTT over TLS, a {@link KeyStore} containing a valid device certificate
 * and private key is required for instantiating the client. Password for
 * decrypting the private key in the KeyStore must also be provided.
 * </p>
 * <p>
 * For MQTT over WebSocket, AWS Signature Version 4 (SigV4) protocol is used for
 * device authentication. For that, a valid AWS IAM access Id and access key
 * pair is required for instantiating the client.
 * <p>
 * In both cases, AWS IoT IAM policies must be configured properly before the
 * connection can be established with the AWS IoT Gateway. For more information
 * about AWS IoT service, please refer to the <a href=
 * "http://docs.aws.amazon.com/iot/latest/developerguide/what-is-aws-iot.html"
 * >AWS IoT developer guide</a>.
 * </p>
 * <p>
 * To use the client directly, a typical flow would be like the below, and since
 * methods in this class are thread-safe, publish and subscribe can be called
 * from different threads.
 * </p>
 * 
 * <pre>
 * {@code
 *     AWSIotMqttClient client = new AWSIotMqttClient(...);
 *     
 *     client.connect();
 *     
 *     ...
 *     client.subscribe(topic, ...)
 *     ...
 *     client.publish(message, ...)
 * }
 * </pre>
 * <p>
 * When using this client in conjunction with {@link AWSIotDevice}, one can
 * implement a device that is always synchronized with its AWS IoT shadow by
 * just providing getter and setter methods for the device attributes. The
 * library does all the heavy lifting by collecting device attributes using the
 * getter methods provided and reporting to the shadow periodically. It also
 * subscribes to device changes and updates the device by calling provided
 * setter methods whenever a change is received. All of these are handled by the
 * library with no extra code required from the user. {@link AWSIotDevice} also
 * provides methods for accessing device shadows directly. Please refer to
 * {@link AWSIotDevice} for more details. A typical flow would be like below.
 * </p>
 * 
 * <pre>
 * {@code
 *     AWSIotMqttClient client = new AWSIotMqttClient(...);
 *     
 *     SomeDevice someDevice = new SomeDevice(thingName);    // SomeDevice extends AWSIotDevice
 *     
 *     client.attach(someDevice);
 *     
 *     client.connect();
 * }
 * </pre>
 * <p>
 * The library contains sample applications that demonstrate different ways of
 * using this client library.
 * </p>
 */
public class AWSIotMqttClient extends AbstractAwsIotClient {

    /**
     * Instantiates a new client using TLS 1.2 mutual authentication. Client
     * certificate and private key are passed in through the {@link KeyStore}
     * argument. The key password protecting the private key in the
     * {@link KeyStore} is also required.
     *
     * @param clientEndpoint
     *            the client endpoint in the form of {@code <account-specific
     *            prefix>.iot.<aws-region>.amazonaws.com}. The account-specific
     *            prefix can be found on the AWS IoT console or by using the
     *            {@code describe-endpoint} command through the AWS command line
     *            interface.
     * @param clientId
     *            the client ID uniquely identify a MQTT connection. Two clients
     *            with the same client ID are not allowed to be connected
     *            concurrently to a same endpoint.
     * @param keyStore
     *            the key store containing the client X.509 certificate and
     *            private key. The {@link KeyStore} object can be constructed
     *            using X.509 certificate file and private key file created on
     *            the AWS IoT console. For more details, please refer to the
     *            README file of this SDK.
     * @param keyPassword
     *            the key password protecting the private key in the
     *            {@code keyStore} argument.
     */
    public AWSIotMqttClient(String clientEndpoint, String clientId, KeyStore keyStore, String keyPassword) {
        super(clientEndpoint, clientId, keyStore, keyPassword);
    }

    /**
     * Instantiates a new client using Secure WebSocket and AWS SigV4
     * authentication. AWS IAM credentials, including the access key ID and
     * secret access key, are required for signing the request. Credentials can
     * be permanent ones associated with IAM users or temporary ones generated
     * via the AWS Cognito service.
     *
     * @param clientEndpoint
     *            the client endpoint in the form of
     *            {@literal <account-specific-prefix>.iot.<region>.amazonaws.com}
     *            . The account-specific prefix can be found on the AWS IoT
     *            console or by using the {@code describe-endpoint} command
     *            through the AWS command line interface.
     * @param clientId
     *            the client ID uniquely identify a MQTT connection. Two clients
     *            with the same client ID are not allowed to be connected
     *            concurrently to a same endpoint.
     * @param awsAccessKeyId
     *            the AWS access key id
     * @param awsSecretAccessKey
     *            the AWS secret access key
     */
    public AWSIotMqttClient(String clientEndpoint, String clientId, String awsAccessKeyId, String awsSecretAccessKey) {
        super(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey, null);
    }

    /**
     * Instantiates a new client using Secure WebSocket and AWS SigV4
     * authentication. AWS IAM credentials, including the access key ID and
     * secret access key, are required for signing the request. Credentials can
     * be permanent ones associated with IAM users or temporary ones generated
     * via the AWS Cognito service.
     *
     * @param clientEndpoint
     *            the client endpoint in the form of
     *            {@literal <account-specific-prefix>.iot.<region>.amazonaws.com}
     *            . The account-specific prefix can be found on the AWS IoT
     *            console or by using the {@code describe-endpoint} command
     *            through the AWS command line interface.
     * @param clientId
     *            the client ID uniquely identify a MQTT connection. Two clients
     *            with the same client ID are not allowed to be connected
     *            concurrently to a same endpoint.
     * @param awsAccessKeyId
     *            the AWS access key id
     * @param awsSecretAccessKey
     *            the AWS secret access key
     * @param sessionToken
     *            Session token received along with the temporary credentials
     *            from services like STS server, AssumeRole, or Amazon Cognito.
     */
    public AWSIotMqttClient(String clientEndpoint, String clientId, String awsAccessKeyId, String awsSecretAccessKey,
            String sessionToken) {
        super(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey, sessionToken);
    }

    /**
     * Updates credentials used for signing Secure WebSocket URLs. When temporary
     * credentails used for the WebSocket connection are expired, newer
     * credentails can be supplied through this API to allow new connections to
     * be reestablished using the new credentails.
     *
     * @param awsAccessKeyId
     *            the AWS access key id
     * @param awsSecretAccessKey
     *            the AWS secret access key
     * @param sessionToken
     *            Session token received along with the temporary credentials
     *            from services like STS server, AssumeRole, or Amazon Cognito.
     */
    @Override
    public void updateCredentials(String awsAccessKeyId, String awsSecretAccessKey, String sessionToken) {
        super.updateCredentials(awsAccessKeyId, awsSecretAccessKey, sessionToken);
    }

    /**
     * Gets the number of client threads currently configured. Each client has
     * their own thread pool, which is used to execute user callback functions
     * as well as any timeout callback functions requested. By default, the
     * thread pool is configured with one execution thread.
     *
     * @return the number of client threads
     */
    @Override
    public int getNumOfClientThreads() {
        return super.getNumOfClientThreads();
    }

    /**
     * Sets a new value for the number of client threads. This value must be set
     * before {@link #connect()} is called.
     *
     * @param numOfClientThreads
     *            the new number of client threads. The default value is 1.
     */
    @Override
    public void setNumOfClientThreads(int numOfClientThreads) {
        super.setNumOfClientThreads(numOfClientThreads);
    }

    /**
     * Gets the connection timeout in milliseconds currently configured.
     * Connection timeout specifies how long the client should wait for the
     * connection to be established with the server. By default, it's 30,000ms.
     *
     * @return the connection timeout
     */
    @Override
    public int getConnectionTimeout() {
        return super.getConnectionTimeout();
    }

    /**
     * Sets a new value in milliseconds for the connection timeout. This value
     * must be set before {@link #connect()} is called.
     *
     * @param connectionTimeout
     *            the new connection timeout. The default value is 30,000ms.
     */
    @Override
    public void setConnectionTimeout(int connectionTimeout) {
        super.setConnectionTimeout(connectionTimeout);
    }

    /**
     * Gets the maximum number of connection retries currently configured.
     * Connections will be automatically retried for the configured maximum
     * times when failing to be established or lost. User disconnect, requested
     * via {@link #disconnect()} will not be retried. By default, it's 5 times.
     * Setting it to 0 will disable the connection retry function.
     *
     * @return the max connection retries
     */
    @Override
    public int getMaxConnectionRetries() {
        return super.getMaxConnectionRetries();
    }

    /**
     * Sets a new value for the maximum connection retries. This value must be
     * set before {@link #connect()} is called. Setting it to 0 will disable the
     * connection retry function.
     *
     * @param maxConnectionRetries
     *            the new max connection retries. The default value is 5.
     */
    @Override
    public void setMaxConnectionRetries(int maxConnectionRetries) {
        super.setMaxConnectionRetries(maxConnectionRetries);
    }

    /**
     * Gets the base retry delay in milliseconds currently configured. For each
     * connection failure, a brief delay has to elapse before the connection is
     * retried. The retry delay is calculated using this simple formula
     * {@code delay = min(baseRetryDelay * pow(2, numRetries), maxRetryDelay)}.
     * By default, the base retry delay is 3,000ms.
     *
     * @return the base retry delay
     */
    @Override
    public int getBaseRetryDelay() {
        return super.getBaseRetryDelay();
    }

    /**
     * Sets a new value in milliseconds for the base retry delay. This value
     * must be set before {@link #connect()} is called.
     *
     * @param baseRetryDelay
     *            the new base retry delay. The default value is 3,000ms.
     */
    @Override
    public void setBaseRetryDelay(int baseRetryDelay) {
        super.setBaseRetryDelay(baseRetryDelay);
    }

    /**
     * Gets the maximum retry delay in milliseconds currently configured. For
     * each connection failure, a brief delay has to elapse before the
     * connection is retried. The retry delay is calculated using this simple
     * formula
     * {@code delay = min(baseRetryDelay * pow(2, numRetries), maxRetryDelay)}.
     * By default, the maximum retry delay is 30,000ms.
     *
     * @return the maximum retry delay
     */
    @Override
    public int getMaxRetryDelay() {
        return super.getMaxRetryDelay();
    }

    /**
     * Sets a new value in milliseconds for the maximum retry delay. This value
     * must be set before {@link #connect()} is called.
     *
     * @param maxRetryDelay
     *            the new max retry delay. The default value is 30,000ms.
     */
    @Override
    public void setMaxRetryDelay(int maxRetryDelay) {
        super.setMaxRetryDelay(maxRetryDelay);
    }

    /**
     * Gets the server acknowledge timeout in milliseconds currently configured.
     * This timeout is used internally by the SDK when subscribing to shadow
     * confirmation topics for get, update, and delete requests. It's also used
     * for re-subscribing to user topics when the connection is retried. For
     * most of the APIs provided in the SDK, the user can specify the timeout as
     * an argument. By default, the server acknowledge timeout is 3,000ms.
     *
     * @return the server acknowledge timeout
     */
    @Override
    public int getServerAckTimeout() {
        return super.getServerAckTimeout();
    }

    /**
     * Sets a new value in milliseconds for the default server acknowledge
     * timeout. This value must be set before {@link #connect()} is called.
     *
     * @param serverAckTimeout
     *            the new server acknowledge timeout. The default value is
     *            3,000ms.
     */
    @Override
    public void setServerAckTimeout(int serverAckTimeout) {
        super.setServerAckTimeout(serverAckTimeout);
    }

    /**
     * Gets the keep-alive interval for the MQTT connection in milliseconds
     * currently configured. Setting this value to 0 will disable the keep-alive
     * function for the connection. The default keep alive interval is 30,000ms.
     *
     * @return the keep alive interval
     */
    @Override
    public int getKeepAliveInterval() {
        return super.getKeepAliveInterval();
    }

    /**
     * Sets a new value in milliseconds for the connection keep-alive interval.
     * This value must be set before {@link #connect()} is called. Setting this
     * value to 0 will disable the keep-alive function.
     *
     * @param keepAliveInterval
     *            the new keep alive interval. The default value is 30,000ms.
     */
    @Override
    public void setKeepAliveInterval(int keepAliveInterval) {
        super.setKeepAliveInterval(keepAliveInterval);
    }

    /**
     * Gets the maximum offline queue size current configured. The offline
     * queues are used for temporarily holding outgoing requests while the
     * connection is being established or retried. When the connection is
     * established, offline queue messages will be sent out as usual. They can
     * be useful for dealing with transient connection failures by allowing the
     * application to continuously send requests while the connection is being
     * established. Each type of request, namely publish, subscribe, and
     * unsubscribe, has their own offline queue. The default offline queue size
     * is 64. Setting it to 0 will disable the offline queues.
     *
     * @return the max offline queue size
     */
    @Override
    public int getMaxOfflineQueueSize() {
        return super.getMaxOfflineQueueSize();
    }

    /**
     * Sets a new value for the maximum offline queue size. This value must be
     * set before {@link #connect()} is called. Setting it to 0 will disable the
     * offline queues.
     *
     * @param maxOfflineQueueSize
     *            the new maximum offline queue size. The default value is 64.
     */
    @Override
    public void setMaxOfflineQueueSize(int maxOfflineQueueSize) {
        super.setMaxOfflineQueueSize(maxOfflineQueueSize);
    }

    /**
     * Gets the Last Will and Testament message currently configured. The Last
     * Will and Testament message with configured payload will be published when
     * the client connection is lost or terminated ungracefully, i.e. not
     * through {@link #disconnect()}.
     *
     * @return the will message
     */
    @Override
    public AWSIotMessage getWillMessage() {
        return super.getWillMessage();
    }

    /**
     * Sets a new Last Will and Testament message. The message must be set
     * before {@link #connect()} is called. By default, Last Will and Testament
     * message is not sent.
     *
     * @param willMessage
     *            the new Last Will and Testament message message. The default
     *            value is {@code null}.
     */
    @Override
    public void setWillMessage(AWSIotMessage willMessage) {
        super.setWillMessage(willMessage);
    }

    /**
     * Connect the client to the server. This is a blocking call, so the calling
     * thread will be blocked until the operation succeeded or failed.
     * <p>
     * Note: Blocking API call without specifying a timeout, in very rare cases,
     * can block the calling thread indefinitely, if the server response is not
     * received or lost. Use the alternative APIs with timeout for applications
     * that expect responses within fixed duration.
     * </p>
     *
     * @throws AWSIotException
     *             exception thrown if the connection operation fails
     */
    @Override
    public void connect() throws AWSIotException {
        super.connect();
    }

    /**
     * Connect the client to the server. This is a blocking call, so the calling
     * thread will be blocked until the operation succeeded, failed, or timed
     * out.
     *
     * @param timeout
     *            the timeout in milliseconds that the calling thread will wait
     * @throws AWSIotException
     *             exception thrown if the operation fails
     * @throws AWSIotTimeoutException
     *             exception thrown if the operation times out
     */
    @Override
    public void connect(long timeout) throws AWSIotException, AWSIotTimeoutException {
        super.connect(timeout);
    }

    /**
     * Connect the client to the server. This call can be either blocking or
     * non-blocking specified by the {@code blocking} argument. For blocking
     * calls, the calling thread is blocked until the operation completed,
     * failed, or timed out; for non-blocking calls, the calling thread will not
     * be blocked while the connection is being established.
     *
     * @param timeout
     *            the timeout in milliseconds that the calling thread will wait
     * @param blocking
     *            whether the call should be blocking or non-blocking
     * @throws AWSIotException
     *             exception thrown if the operation fails
     * @throws AWSIotTimeoutException
     *             exception thrown if the operation times out
     */
    @Override
    public void connect(long timeout, boolean blocking) throws AWSIotException, AWSIotTimeoutException {
        super.connect(timeout, blocking);
    }

    /**
     * Disconnect the client from the server. This is a blocking call, so the
     * calling thread will be blocked until the operation succeeded or failed.
     * <p>
     * Note: Blocking API call without specifying a timeout, in very rare cases,
     * can block the calling thread indefinitely, if the server response is not
     * received or lost. Use the alternative APIs with timeout for applications
     * that expect responses within fixed duration.
     * </p>
     *
     * @throws AWSIotException
     *             exception thrown if the operation fails
     */
    @Override
    public void disconnect() throws AWSIotException {
        super.disconnect();
    }

    /**
     * Disconnect the client from the server. This is a blocking call, so the
     * calling thread will be blocked until the operation succeeded, failed, or
     * timed out.
     *
     * @param timeout
     *            the timeout in milliseconds that the calling thread will wait
     * @throws AWSIotException
     *             exception thrown if the operation fails
     * @throws AWSIotTimeoutException
     *             exception thrown if the operation times out
     */
    @Override
    public void disconnect(long timeout) throws AWSIotException, AWSIotTimeoutException {
        super.disconnect(timeout);
    }

    /**
     * Disconnect the client from the server. This call can be either blocking
     * or non-blocking specified by the {@code blocking} argument. For blocking
     * calls, the calling thread is blocked until the operation completed,
     * failed, or timed out; for non-blocking calls, the calling thread will not
     * be blocked while the connection is being terminated.
     *
     * @param timeout
     *            the timeout in milliseconds that the calling thread will wait
     * @param blocking
     *            whether the call should be blocking or non-blocking
     * @throws AWSIotException
     *             exception thrown if the operation fails
     * @throws AWSIotTimeoutException
     *             exception thrown if the operation times out
     */
    @Override
    public void disconnect(long timeout, boolean blocking) throws AWSIotException, AWSIotTimeoutException {
        super.disconnect(timeout, blocking);
    }

    /**
     * Publishes the payload to a given topic. This is a blocking call so the
     * calling thread is blocked until the publish operation succeeded or
     * failed. MQTT QoS0 is used for publishing the payload.
     * <p>
     * Note: Blocking API call without specifying a timeout, in very rare cases,
     * can block the calling thread indefinitely, if the server response is not
     * received or lost. Use the alternative APIs with timeout for applications
     * that expect responses within fixed duration.
     * </p>
     *
     * @param topic
     *            the topic to be published to
     * @param payload
     *            the payload to be published
     * @throws AWSIotException
     *             exception thrown if the publish operation fails
     */
    @Override
    public void publish(String topic, String payload) throws AWSIotException {
        super.publish(topic, payload);
    }

    /**
     * Publishes the payload to a given topic. This is a blocking call so the
     * calling thread is blocked until the publish operation succeeded, failed,
     * or the specified timeout has elapsed. MQTT QoS0 is used for publishing
     * the payload.
     *
     * @param topic
     *            the topic to be published to
     * @param payload
     *            the payload to be published
     * @param timeout
     *            the timeout in milliseconds that the calling thread will wait
     * @throws AWSIotException
     *             the exception thrown if the publish operation fails
     * @throws AWSIotTimeoutException
     *             the exception thrown if the publish operation times out
     */
    @Override
    public void publish(String topic, String payload, long timeout) throws AWSIotException, AWSIotTimeoutException {
        super.publish(topic, payload, timeout);
    }

    /**
     * Publishes the payload to a given topic. This is a blocking call so the
     * calling thread is blocked until the publish operation succeeded or
     * failed.
     * <p>
     * Note: Blocking API call without specifying a timeout, in very rare cases,
     * can block the calling thread indefinitely, if the server response is not
     * received or lost. Use the alternative APIs with timeout for applications
     * that expect responses within fixed duration.
     * </p>
     *
     * @param topic
     *            the topic to be published to
     * @param qos
     *            the MQTT QoS used for publishing
     * @param payload
     *            the payload to be published
     * @throws AWSIotException
     *             the exception thrown if the publish operation fails
     */
    @Override
    public void publish(String topic, AWSIotQos qos, String payload) throws AWSIotException {
        super.publish(topic, qos, payload);
    }

    /**
     * Publishes the payload to a given topic. This is a blocking call so the
     * calling thread is blocked until the publish operation succeeded, failed,
     * or the specified timeout has elapsed.
     *
     * @param topic
     *            the topic to be published to
     * @param qos
     *            the MQTT QoS used for publishing
     * @param payload
     *            the payload to be published
     * @param timeout
     *            the timeout in milliseconds that the calling thread will wait
     * @throws AWSIotException
     *             the exception thrown if the publish operation fails
     * @throws AWSIotTimeoutException
     *             the exception thrown if the publish operation times out
     */
    @Override
    public void publish(String topic, AWSIotQos qos, String payload, long timeout) throws AWSIotException,
            AWSIotTimeoutException {
        super.publish(topic, qos, payload, timeout);
    }

    /**
     * Publishes the raw payload to a given topic. This is a blocking call so
     * the calling thread is blocked until the publish operation succeeded or
     * failed. MQTT QoS0 is used for publishing the payload.
     * <p>
     * Note: Blocking API call without specifying a timeout, in very rare cases,
     * can block the calling thread indefinitely, if the server response is not
     * received or lost. Use the alternative APIs with timeout for applications
     * that expect responses within fixed duration.
     * </p>
     *
     * @param topic
     *            the topic to be published to
     * @param payload
     *            the payload to be published
     * @throws AWSIotException
     *             the exception thrown if the publish operation fails
     */
    @Override
    public void publish(String topic, byte[] payload) throws AWSIotException {
        super.publish(topic, payload);
    }

    /**
     * Publishes the raw payload to a given topic. This is a blocking call so
     * the calling thread is blocked until the publish operation succeeded,
     * failed, or the specified timeout has elapsed. MQTT QoS0 is used for
     * publishing the payload.
     *
     * @param topic
     *            the topic to be published to
     * @param payload
     *            the payload to be published
     * @param timeout
     *            the timeout in milliseconds that the calling thread will wait
     * @throws AWSIotException
     *             the exception thrown if the publish operation fails
     * @throws AWSIotTimeoutException
     *             the exception thrown if the publish operation times out
     */
    @Override
    public void publish(String topic, byte[] payload, long timeout) throws AWSIotException, AWSIotTimeoutException {
        super.publish(topic, payload, timeout);
    }

    /**
     * Publishes the raw payload to a given topic. This is a blocking call so
     * the calling thread is blocked until the publish operation is succeeded or
     * failed.
     * <p>
     * Note: Blocking API call without specifying a timeout, in very rare cases,
     * can block the calling thread indefinitely, if the server response is not
     * received or lost. Use the alternative APIs with timeout for applications
     * that expect responses within fixed duration.
     * </p>
     *
     * @param topic
     *            the topic to be published to
     * @param qos
     *            the MQTT QoS used for publishing
     * @param payload
     *            the payload to be published
     * @throws AWSIotException
     *             the exception thrown if the publish operation fails
     */
    @Override
    public void publish(String topic, AWSIotQos qos, byte[] payload) throws AWSIotException {
        super.publish(topic, qos, payload);
    }

    /**
     * Publishes the raw payload to a given topic. This is a blocking call so
     * the calling thread is blocked until the publish operation is succeeded,
     * failed, or the specified timeout has elapsed.
     *
     * @param topic
     *            the topic to be published to
     * @param qos
     *            the MQTT QoS used for publishing
     * @param payload
     *            the payload to be published
     * @param timeout
     *            the timeout in milliseconds that the calling thread will wait
     * @throws AWSIotException
     *             the exception thrown if the publish operation fails
     * @throws AWSIotTimeoutException
     *             the exception thrown if the publish operation times out
     */
    @Override
    public void publish(String topic, AWSIotQos qos, byte[] payload, long timeout) throws AWSIotException,
            AWSIotTimeoutException {
        super.publish(topic, qos, payload, timeout);
    }

    /**
     * Publishes the payload to a given topic. Topic, MQTT QoS, and payload are
     * given in the {@code message} argument. This is a non-blocking call so it
     * immediately returns once the operation has been queued in the system. The
     * result of the operation will be notified through the callback functions,
     * namely {@link AWSIotMessage#onSuccess} and
     * {@link AWSIotMessage#onFailure}, one of which will be invoked after the
     * operation succeeded or failed respectively. The default implementation
     * for the callback functions in {@link AWSIotMessage} does nothing. The
     * user could override one or more of these functions through subclassing.
     *
     * @param message
     *            the message, including the topic, MQTT QoS, and payload, to be
     *            published
     * @throws AWSIotException
     *             the exception thrown if the publish operation fails to be
     *             queued
     */
    @Override
    public void publish(AWSIotMessage message) throws AWSIotException {
        super.publish(message);
    }

    /**
     * Publishes the payload to a given topic. Topic, MQTT QoS, and payload are
     * given in the {@code message} argument. This is a non-blocking call so it
     * immediately returns once the operation has been queued in the system. The
     * result of the operation will be notified through the callback functions,
     * namely {@link AWSIotMessage#onSuccess}, {@link AWSIotMessage#onFailure},
     * and {@link AWSIotMessage#onTimeout}, one of which will be invoked after
     * the operation succeeded, failed, or timed out respectively. The user
     * could override one or more of these functions through subclassing.
     *
     * @param message
     *            the message, including the topic, MQTT QoS, and payload, to be
     *            published
     * @param timeout
     *            the timeout in milliseconds for the operation to be considered
     *            timed out
     * @throws AWSIotException
     *             the exception thrown if the publish operation fails to be
     *             queued
     */
    @Override
    public void publish(AWSIotMessage message, long timeout) throws AWSIotException {
        super.publish(message, timeout);
    }

    /**
     * Subscribes to a given topic. Topic and MQTT QoS are given in the
     * {@code topic} argument. This call can be either blocking or non-blocking
     * specified by the {@code blocking} argument. For blocking calls, the
     * calling thread is blocked until the subscribe operation completed or
     * failed; for non-blocking calls, the result of the operation will be
     * notified through the callback functions, namely
     * {@link AWSIotTopic#onSuccess} and {@link AWSIotTopic#onFailure}, one of
     * which will be invoked after the operation succeeded or failed
     * respectively. For both blocking and non-blocking calls, callback function
     * {@link AWSIotTopic#onMessage} is invoked when subscribed message arrives.
     * The default implementation for the callback functions in
     * {@link AWSIotTopic} does nothing. The user could override one or more of
     * these functions through subclassing.
     * <p>
     * Note: Blocking API call without specifying a timeout, in very rare cases,
     * can block the calling thread indefinitely, if the server response is not
     * received or lost. Use the alternative APIs with timeout for applications
     * that expect responses within fixed duration.
     * </p>
     *
     * @param topic
     *            the topic to subscribe to
     * @param blocking
     *            whether the call should be blocking or non-blocking
     * @throws AWSIotException
     *             the exception thrown if the subscribe operation fails
     *             (blocking) or fails to be queued (non-blocking)
     */
    @Override
    public void subscribe(AWSIotTopic topic, boolean blocking) throws AWSIotException {
        super.subscribe(topic, blocking);
    }

    /**
     * Subscribes to a given topic. Topic and MQTT QoS are given in the
     * {@code topic} argument. This call can be either blocking or non-blocking
     * specified by the {@code blocking} argument. For blocking call, the
     * calling thread is blocked until the subscribe operation completed,
     * failed, or timed out; for non-blocking call, the result of the operation
     * will be notified through the callback functions, namely
     * {@link AWSIotTopic#onSuccess}, {@link AWSIotTopic#onFailure} and
     * {@link AWSIotTopic#onTimeout}, one of which will be invoked after the
     * operation succeeded, failed, or timed out respectively. For both blocking
     * and non-blocking calls, callback function {@link AWSIotTopic#onMessage}
     * is invoked when subscribed message arrives. The default implementation
     * for the callback functions in {@link AWSIotTopic} does nothing. The user
     * could override one or more of these functions through subclassing.
     *
     * @param topic
     *            the topic to subscribe to
     * @param timeout
     *            the timeout in milliseconds for the operation to be considered
     *            timed out
     * @param blocking
     *            whether the call should be blocking or non-blocking
     * @throws AWSIotException
     *             the exception thrown if the subscribe operation fails
     *             (blocking) or fails to be queued (non-blocking)
     * @throws AWSIotTimeoutException
     *             the exception thrown if the subscribe operation times out.
     *             This exception is not thrown if the call is non-blocking;
     *             {@link AWSIotTopic#onTimeout} will be invoked instead if
     *             timeout happens.
     */
    @Override
    public void subscribe(AWSIotTopic topic, long timeout, boolean blocking) throws AWSIotException,
            AWSIotTimeoutException {
        super.subscribe(topic, timeout, blocking);
    }

    /**
     * Subscribes to a given topic. Topic and MQTT QoS are given in the
     * {@code topic} argument. This is a non-blocking call so it immediately
     * returns once is the operation has been queued in the system. The result
     * of the operation will be notified through the callback functions, namely
     * {@link AWSIotTopic#onSuccess} and {@link AWSIotTopic#onFailure}, one of
     * which will be invoked after the operation succeeded or failed
     * respectively. Another callback function, {@link AWSIotTopic#onMessage},
     * is invoked when subscribed message arrives. The default implementation
     * for the callback functions in {@link AWSIotTopic} does nothing. The user
     * could override one or more of these functions through sub-classing.
     *
     * @param topic
     *            the topic to subscribe to
     * @throws AWSIotException
     *             the exception thrown if the subscribe operation fails to be
     *             queued
     */
    @Override
    public void subscribe(AWSIotTopic topic) throws AWSIotException {
        super.subscribe(topic);
    }

    /**
     * Subscribes to a given topic. Topic and MQTT QoS are given in the
     * {@code topic} argument. This is a non-blocking call so it immediately
     * returns once is the operation has been queued in the system. The result
     * of the operation will be notified through the callback functions, namely
     * {@link AWSIotTopic#onSuccess}, {@link AWSIotTopic#onFailure}, and
     * {@link AWSIotTopic#onTimeout}, one of which will be invoked after the
     * operation succeeded, failed, or timed out respectively. Another callback
     * function, {@link AWSIotTopic#onMessage}, is invoked when subscribed
     * message arrives. The default implementation for the callback functions in
     * {@link AWSIotTopic} does nothing. The user could override one or more of
     * these functions through sub-classing.
     *
     * @param topic
     *            the topic to subscribe to
     * @param timeout
     *            the timeout in milliseconds for the operation to be considered
     *            timed out
     * @throws AWSIotException
     *             the exception thrown if the subscribe operation fails to be
     *             queued
     */
    @Override
    public void subscribe(AWSIotTopic topic, long timeout) throws AWSIotException {
        super.subscribe(topic, timeout);
    }

    /**
     * Unsubscribes to a given topic. This is a blocking call, so the calling
     * thread is blocked until the unsubscribe operation completed or failed.
     * <p>
     * Note: Blocking API call without specifying a timeout, in very rare cases,
     * can block the calling thread indefinitely, if the server response is not
     * received or lost. Use the alternative APIs with timeout for applications
     * that expect responses within fixed duration.
     * </p>
     *
     * @param topic
     *            the topic to unsubscribe to
     * @throws AWSIotException
     *             the exception thrown if the unsubscribe operation fails
     */
    @Override
    public void unsubscribe(String topic) throws AWSIotException {
        super.unsubscribe(topic);
    }

    /**
     * Unsubscribes to a given topic. This is a blocking call, so the calling
     * thread is blocked until the unsubscribe operation completed, failed, or
     * the specified timeout has elapsed.
     *
     * @param topic
     *            the topic to unsubscribe to
     * @param timeout
     *            the timeout in milliseconds that the calling thread will wait
     * @throws AWSIotException
     *             the exception thrown if the unsubscribe operation fails
     * @throws AWSIotTimeoutException
     *             the exception thrown if the unsubscribe operation times out
     */
    @Override
    public void unsubscribe(String topic, long timeout) throws AWSIotException, AWSIotTimeoutException {
        super.unsubscribe(topic, timeout);
    }

    /**
     * Unsubscribes to a given topic. This is a non-blocking call so it
     * immediately returns once the operation has been queued in the system. The
     * result of the operation will be notified through the callback functions,
     * namely {@link AWSIotTopic#onSuccess} and {@link AWSIotTopic#onFailure},
     * one of which will be invoked after the operation succeeded or failed
     * respectively. The default implementation for the callback functions in
     * {@link AWSIotTopic} does nothing. The user could override one or more of
     * these functions through subclassing.
     * 
     * @param topic
     *            the topic to unsubscribe to
     * @throws AWSIotException
     *             the exception thrown if the unsubscribe operation fails to be
     *             queued
     */
    @Override
    public void unsubscribe(AWSIotTopic topic) throws AWSIotException {
        super.unsubscribe(topic);
    }

    /**
     * Unsubscribes to a given topic. This is a non-blocking call so it
     * immediately returns once the operation has been queued in the system. The
     * result of the operation will be notified through the callback functions,
     * namely {@link AWSIotTopic#onSuccess}, {@link AWSIotTopic#onFailure}, and
     * {@link AWSIotTopic#onTimeout}, one of which will be invoked after the
     * operation succeeded, failed, or timed out respectively. The default
     * implementation for the callback functions in {@link AWSIotTopic} does
     * nothing. The user could override one or more of these functions through
     * subclassing.
     *
     * @param topic
     *            the topic to unsubscribe to
     * @param timeout
     *            the timeout in milliseconds for the operation to be considered
     *            timed out
     * @throws AWSIotException
     *             the exception thrown if the unsubscribe operation fails to be
     *             queued
     */
    @Override
    public void unsubscribe(AWSIotTopic topic, long timeout) throws AWSIotException {
        super.unsubscribe(topic, timeout);
    }

    /**
     * Attach a shadow device to the client. Once attached, the device, if
     * configured, will be automatically synchronized with the AWS Thing shadow
     * using this client and its connection. For more details about how to
     * configure and use a device, please refer to {@link AWSIotDevice}.
     *
     * @param device
     *            the device to be attached to the client
     * @throws AWSIotException
     *             the exception thrown if the attach operation fails
     */
    @Override
    public void attach(AWSIotDevice device) throws AWSIotException {
        super.attach(device);
    }

    /**
     * Detach the given device from the client. Device and shadow
     * synchronization will be stopped after the device is detached from the
     * client.
     *
     * @param device
     *            the device to be detached from the client
     * @throws AWSIotException
     *             the exception thrown if the detach operation fails
     */
    @Override
    public void detach(AWSIotDevice device) throws AWSIotException {
        super.detach(device);
    }

    /**
     * Gets the connection status of the connection used by the client.
     *
     * @return the connection status
     */
    @Override
    public AWSIotConnectionStatus getConnectionStatus() {
        return super.getConnectionStatus();
    }

    /**
     * This callback function is called when the connection used by the client
     * is successfully established. The user could supply a different callback
     * function via subclassing, however the default implementation should
     * always be called in the override function in order for the connection
     * retry as well as device synchronization to work properly.
     */
    @Override
    public void onConnectionSuccess() {
        super.onConnectionSuccess();
    }

    /**
     * This callback function is called when the connection used by the client
     * is temporarily lost. The user could supply a different callback function
     * via subclassing, however the default implementation should always be
     * called in the override function in order for the connection retry as well
     * as device synchronization to work properly.
     */
    @Override
    public void onConnectionFailure() {
        super.onConnectionFailure();
    }

    /**
     * This callback function is called when the connection used by the client
     * is permanently closed. The user could supply a different callback
     * function via subclassing, however the default implementation should
     * always be called in the override function in order for the connection
     * retry as well as device synchronization to work properly.
     */
    @Override
    public void onConnectionClosed() {
        super.onConnectionClosed();
    }

}
