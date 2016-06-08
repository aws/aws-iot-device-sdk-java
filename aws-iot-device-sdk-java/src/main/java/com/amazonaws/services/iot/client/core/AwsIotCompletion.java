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

import java.util.concurrent.Future;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;

/**
 * This is a helper class that can be used to manage the request execution and
 * return either synchronously or asynchronously the result, e.g. success,
 * failure, or timeout. It's used by most of the APIs to implement blocking and
 * non-blocking calls with timeout support.
 */
public class AwsIotCompletion extends AWSIotMessage {

    /** The request containing the callback functions. */
    protected final AWSIotMessage request;

    /** The timeout associated with the request. */
    protected final long timeout;

    /** whether the request is asynchronous or not. */
    protected final boolean isAsync;

    /** The future object of the timeout task. */
    protected Future<?> timeoutTask;

    /** Indicates whether the request has completed successfully. */
    protected boolean hasSuccess;

    /** Indicates whether the request has completed with failure. */
    protected boolean hasFailure;

    /** Indicates whether the request has timed out. */
    protected boolean hasTimeout;

    /**
     * Instantiates a new completion object with a synchronous request.
     *
     * @param topic
     *            the topic of the request
     * @param qos
     *            the QoS of the request
     * @param timeout
     *            the timeout in milliseconds for the request. If timeout is 0
     *            or less, the request will never be timed out.
     */
    public AwsIotCompletion(String topic, AWSIotQos qos, long timeout) {
        super(topic, qos);

        this.timeout = timeout;
        this.request = null;
        this.isAsync = false;
    }

    /**
     * Instantiates a new completion object with a synchronous request.
     *
     * @param topic
     *            the topic of the request
     * @param qos
     *            the QoS of the request
     * @param payload
     *            the string payload of the request
     * @param timeout
     *            the timeout in milliseconds for the request. If timeout is 0
     *            or less, the request will never be timed out.
     */
    public AwsIotCompletion(String topic, AWSIotQos qos, String payload, long timeout) {
        super(topic, qos, payload);

        this.timeout = timeout;
        this.request = null;
        this.isAsync = false;
    }

    /**
     * Instantiates a new completion object with a synchronous request.
     *
     * @param topic
     *            the topic of the request
     * @param qos
     *            the QoS of the request
     * @param payload
     *            the byte array payload of the request
     * @param timeout
     *            the timeout in milliseconds for the request. If timeout is 0
     *            or less, the request will never be timed out.
     */
    public AwsIotCompletion(String topic, AWSIotQos qos, byte[] payload, long timeout) {
        super(topic, qos, payload);

        this.timeout = timeout;
        this.request = null;
        this.isAsync = false;
    }

    /**
     * Instantiates a new completion object either synchronous or asynchronous
     * request based on the <code>isAsync</code> argument.
     *
     * @param timeout
     *            the timeout in milliseconds for the request. If timeout is 0
     *            or less, the request will never be timed out.
     * @param isAsync
     *            whether or not the request is asynchronous
     */
    public AwsIotCompletion(long timeout, boolean isAsync) {
        super(null, null);

        this.request = null;
        this.timeout = timeout;
        this.isAsync = isAsync;
    }

    /**
     * Instantiates a new completion object either synchronous or asynchronous
     * request based on the <code>isAsync</code> argument. Callback functions
     * are provided through the <code>req</code> argument.
     *
     * @param req
     *            the request containing request topic, QoS, payload, and
     *            callback functions for asynchronous requests.
     * @param timeout
     *            the timeout in milliseconds for the request. If timeout is 0
     *            or less, the request will never be timed out.
     * @param isAsync
     *            whether or not the request is asynchronous
     */
    public AwsIotCompletion(AWSIotMessage req, long timeout, boolean isAsync) {
        super(req.getTopic(), req.getQos(), req.getPayload());

        this.request = req;
        this.timeout = timeout;
        this.isAsync = isAsync;
    }

    /**
     * The user of the completion object is expected to call this function to
     * either block until the request is completed or timed out in the case of
     * synchronous calls, or to schedule a timeout handler in the case of
     * asynchronous calls.
     *
     * @param client
     *            the client object that provides the execution thread pool for
     *            the timeout handler.
     * @throws AWSIotException
     *             For synchronous calls, this exception may be thrown if the
     *             request has failed.
     * @throws AWSIotTimeoutException
     *             For synchronous calls, this exception may be thrown if the
     *             request has timed out.
     */
    public void get(AbstractAwsIotClient client) throws AWSIotException, AWSIotTimeoutException {
        synchronized (this) {
            if (hasSuccess || hasFailure || hasTimeout) {
                // operation has completed before get() is called
                if (!isAsync) {
                    if (hasFailure) {
                        throw new AWSIotException("Error happened when processing command " + topic);
                    }
                    if (hasTimeout) {
                        throw new AWSIotTimeoutException("Request timed out when processing command " + topic);
                    }
                }
                return;
            }

            if (timeout > 0) {
                timeoutTask = client.scheduleTimeoutTask(new Runnable() {
                    @Override
                    public void run() {
                        onTimeout();
                    }
                }, timeout);
            }

            // if it's an asynchronous request, we don't block the calling
            // thread
            if (isAsync) {
                return;
            }

            while (!hasSuccess && !hasFailure && !hasTimeout) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    cancelTimeoutTask();
                    throw new AWSIotException(e);
                }
            }

            cancelTimeoutTask();

            if (hasFailure) {
                throw new AWSIotException(errorCode, errorMessage);
            }
            if (hasTimeout) {
                throw new AWSIotTimeoutException("Request timed out when processing request " + topic);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.amazonaws.services.iot.client.AwsIotMessage#onSuccess()
     */
    @Override
    public void onSuccess() {
        synchronized (this) {
            if (hasSuccess || hasFailure || hasTimeout) {
                return;
            }

            hasSuccess = true;
            cancelTimeoutTask();

            if (!isAsync) {
                notify();
                return;
            }
        }

        if (request != null) {
            request.onSuccess();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.amazonaws.services.iot.client.AwsIotMessage#onFailure()
     */
    @Override
    public void onFailure() {
        synchronized (this) {
            if (hasSuccess || hasFailure || hasTimeout) {
                return;
            }

            hasFailure = true;
            cancelTimeoutTask();

            if (!isAsync) {
                notify();
                return;
            }
        }

        if (request != null) {
            request.setErrorCode(errorCode);
            request.setErrorMessage(errorMessage);
            request.onFailure();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.amazonaws.services.iot.client.AwsIotMessage#onTimeout()
     */
    @Override
    public void onTimeout() {
        synchronized (this) {
            if (hasSuccess || hasFailure || hasTimeout) {
                return;
            }

            hasTimeout = true;
            cancelTimeoutTask();

            if (!isAsync) {
                notify();
                return;
            }
        }

        if (request != null) {
            request.onTimeout();
        }
    }

    /**
     * Cancel timeout task.
     */
    private void cancelTimeoutTask() {
        if (timeoutTask != null && !timeoutTask.isCancelled()) {
            timeoutTask.cancel(false);
        }
    }

}
