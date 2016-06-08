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

import com.amazonaws.services.iot.client.shadow.AbstractAwsIotDevice;

/**
 * This class encapsulates all the functionalities that one can use to interact
 * with AWS IoT device shadows in the cloud. For more information about AWS IoT
 * device shadow, please refer to the <a href=
 * "http://docs.aws.amazon.com/iot/latest/developerguide/iot-thing-shadows.html"
 * >AWS IoT developer guide</a>.
 * <p>
 * {@link AWSIotDevice} represents a device that is one-to-one mapped with the
 * AWS IoT device shadow. The linkage is created through the shadow name that is
 * passed into the constructor.
 * </p>
 * <p>
 * There are two typical ways of using {@link AWSIotDevice}. One is to extend
 * {@link AWSIotDevice} and provide device attributes that are to be
 * synchronized with the shadow and their accessor methods (getters and
 * setters). The other way is to use the get/update/delete methods provided in
 * this class to directly access the shadow document. The first approach is easy
 * to implement and should work for most of the use cases; the second approach
 * provides the user the ability of directly accessing the data (document)
 * stored on the device shadow, which is very flexible, however the user is
 * responsible for parsing the shadow document encoded in JSON, and providing
 * shadow-compatible document in update calls. It's also possible to use both
 * approaches in a same application.
 * </p>
 * <p>
 * To leverage the synchronization function provided by the library, one needs
 * to extend {@link AWSIotDevice}. Device attributes that are to be kept in sync
 * with the shadow must be annotated with {@link AWSIotDeviceProperty}. One
 * should also provide getter functions for these annotated attributes to be
 * reported to the shadow as well as setter functions to accept updates from the
 * shadow. A simplified example is like this
 * </p>
 * 
 * <pre>
 *     public class SomeDevice extends AWSIotDevice {
 *        {@literal @}AWSIotDeviceProperty
 *         boolean switch;
 *         
 *         public boolean getSwitch() {
 *              // read from the device and return the value to be reported to the shadow
 *              return ...;
 *         }
 *         
 *         public void setSwitch(boolean requestedState) {
 *              // write to the device with the requested value from the shadow
 *         }
 *     }
 * </pre>
 * <p>
 * To linked the above class with the shadow, one could do like so
 * </p>
 * 
 * <pre>
 *     AWSIotMqttClient client = new AWSIotMqttClient(...);
 *     
 *     SomeDevice someDevice = new SomeDevice(thingName);
 *     
 *     client.attach(someDevice);
 *     
 *     client.connect();
 * </pre>
 * <p>
 * To access the shadow directly, one could do as the below. All the methods in
 * this class are thread-safe, therefore can be called in different user
 * threads.
 * </p>
 * 
 * <pre>
 *     AWSIotMqttClient client = new AWSIotMqttClient(...);
 *     
 *     AWSIotDevice awsIotDevice = new AWSIotDevice(thingName);
 *     
 *     client.attach(awsIotDevice);
 *     
 *     client.connect();
 *     
 *     ...
 *     String jsonDocument = awsIotDevice.get();
 *     ...
 *     client.update(jsonDocument);
 *     ...
 * </pre>
 * <p>
 * The library contains sample applications that demonstrate how each of these
 * two methods can be used.
 * </p>
 */
public class AWSIotDevice extends AbstractAwsIotDevice {

    /**
     * Instantiates a new device instance.
     *
     * @param thingName
     *            the thing name
     */
    public AWSIotDevice(String thingName) {
        super(thingName);
    }

    /**
     * Gets the device report interval.
     *
     * @return the report interval in milliseconds.
     */
    @Override
    public long getReportInterval() {
        return super.getReportInterval();
    }

    /**
     * Sets the device report interval in milliseconds. This value must be set
     * before the device is attached to a client via the
     * {@link AWSIotMqttClient#attach(AWSIotDevice)} call. The default interval
     * is 3,000ms. Setting it to 0 will disable reporting.
     *
     * @param reportInterval
     *            the new report interval
     */
    @Override
    public void setReportInterval(long reportInterval) {
        super.setReportInterval(reportInterval);
    }

    /**
     * Checks if versioning is enabled for device updates.
     *
     * @return true, if versioning is enabled for device updates.
     */
    @Override
    public boolean isEnableVersioning() {
        return super.isEnableVersioning();
    }

    /**
     * Sets the device update versioning to be enabled or disabled. This value
     * must be set before the device is attached to a client via the
     * {@link AWSIotMqttClient#attach(AWSIotDevice)} call.
     *
     * @param enableVersioning
     *            true to enable device update versioning; false to disable.
     */
    @Override
    public void setEnableVersioning(boolean enableVersioning) {
        super.setEnableVersioning(enableVersioning);
    }

    /**
     * Gets the MQTT QoS level for publishing the device report. The default QoS
     * is QoS 0.
     *
     * @return the device report QoS
     */
    @Override
    public AWSIotQos getDeviceReportQos() {
        return super.getDeviceReportQos();
    }

    /**
     * Sets the MQTT QoS level for publishing the device report. This value must
     * be set before the device is attached to a client via the
     * {@link AWSIotMqttClient#attach(AWSIotDevice)} call.
     *
     * @param deviceReportQos
     *            the new device report QoS
     */
    @Override
    public void setDeviceReportQos(AWSIotQos deviceReportQos) {
        super.setDeviceReportQos(deviceReportQos);
    }

    /**
     * Gets the MQTT QoS level for subscribing to shadow updates. The default
     * QoS is QoS 0.
     *
     * @return the shadow update QoS
     */
    @Override
    public AWSIotQos getShadowUpdateQos() {
        return super.getShadowUpdateQos();
    }

    /**
     * Sets the MQTT QoS level for subscribing to shadow updates. This value
     * must be set before the device is attached to a client via the
     * {@link AWSIotMqttClient#attach(AWSIotDevice)} call.
     *
     * @param shadowUpdateQos
     *            the new shadow update QoS
     */
    @Override
    public void setShadowUpdateQos(AWSIotQos shadowUpdateQos) {
        super.setShadowUpdateQos(shadowUpdateQos);
    }

    /**
     * Gets the MQTT QoS level for sending the shadow methods, namely Get,
     * Update, and Delete. The default QoS is QoS 0.
     *
     * @return the QoS level for sending shadow methods.
     */
    @Override
    public AWSIotQos getMethodQos() {
        return super.getMethodQos();
    }

    /**
     * Sets the MQTT QoS level for sending shadow methods. This value must be
     * set before the device is attached to a client via the
     * {@link AWSIotMqttClient#attach(AWSIotDevice)} call.
     *
     * @param methodQos
     *            the new QoS level for sending shadow methods.
     */
    @Override
    public void setMethodQos(AWSIotQos methodQos) {
        super.setMethodQos(methodQos);
    }

    /**
     * Gets the MQTT QoS level for subscribing to acknowledgement messages of
     * shadow methods. The default QoS is QoS 0.
     *
     * @return the QoS level for subscribing to acknowledgement messages.
     */
    @Override
    public AWSIotQos getMethodAckQos() {
        return super.getMethodAckQos();
    }

    /**
     * Sets the MQTT QoS level for subscribing to acknowledgement messages of
     * shadow methods. This value must be set before the device is attached to a
     * client via the {@link AWSIotMqttClient#attach(AWSIotDevice)} call.
     *
     * @param methodAckQos
     *            the new QoS level for subscribing to acknowledgement messages.
     */
    @Override
    public void setMethodAckQos(AWSIotQos methodAckQos) {
        super.setMethodAckQos(methodAckQos);
    }

    /**
     * Retrieves the latest state stored in the thing shadow. This method
     * returns the full JSON document, including meta data. This is a blocking
     * call, so the calling thread will be blocked until the operation succeeded
     * or failed.
     * <p>
     * Note: Blocking API call without specifying a timeout, in very rare cases,
     * can block the calling thread indefinitely, if the server response is not
     * received or lost. Use the alternative APIs with timeout for applications
     * that expect responses within fixed duration.
     * </p>
     *
     * @return the JSON document of the device state
     * @throws AWSIotException
     *             exception thrown if the operation fails
     */
    @Override
    public String get() throws AWSIotException {
        return super.get();
    }

    /**
     * Retrieves the latest state stored in the thing shadow. This method
     * returns the full JSON document, including meta data. This is a blocking
     * call, so the calling thread will be blocked until the operation
     * succeeded, failed, or timed out.
     *
     * @param timeout
     *            the timeout in milliseconds that the calling thread will wait
     * @return the JSON document of the device state
     * @throws AWSIotException
     *             exception thrown if the operation fails
     * @throws AWSIotTimeoutException
     *             exception thrown if the operation times out
     */
    @Override
    public String get(long timeout) throws AWSIotException, AWSIotTimeoutException {
        return super.get(timeout);
    }

    /**
     * Retrieves the latest state stored in the thing shadow. This method
     * returns the full JSON document, including meta data. This is a
     * non-blocking call, so it immediately returns once is the operation has
     * been queued in the system. The result of the operation will be notified
     * through the callback functions, namely {@link AWSIotMessage#onSuccess},
     * {@link AWSIotMessage#onFailure}, and {@link AWSIotMessage#onTimeout}, one
     * of which will be invoked after the operation succeeded, failed, or timed
     * out respectively.
     * 
     * @param message
     *            the message object contains callback functions; if the call is
     *            successful, the full JSON document of the device state will be
     *            stored in the {@code payload} field of {@code message}.
     * @param timeout
     *            the timeout in milliseconds for the operation to be considered
     *            timed out
     * @throws AWSIotException
     *             exception thrown if the operation fails
     */
    @Override
    public void get(AWSIotMessage message, long timeout) throws AWSIotException {
        super.get(message, timeout);
    }

    /**
     * Updates the content of a thing shadow with the data provided in the
     * request. This is a blocking call, so the calling thread will be blocked
     * until the operation succeeded or failed.
     * <p>
     * Note: Blocking API call without specifying a timeout, in very rare cases,
     * can block the calling thread indefinitely, if the server response is not
     * received or lost. Use the alternative APIs with timeout for applications
     * that expect responses within fixed duration.
     * </p>
     *
     * @param jsonState
     *            the JSON document of the new device state
     * @throws AWSIotException
     *             exception thrown if the operation fails
     */
    @Override
    public void update(String jsonState) throws AWSIotException {
        super.update(jsonState);
    }

    /**
     * Updates the content of a thing shadow with the data provided in the
     * request. This is a blocking call, so the calling thread will be blocked
     * until the operation succeeded, failed, or timed out.
     *
     * @param jsonState
     *            the JSON document of the new device state
     * @param timeout
     *            the timeout in milliseconds that the calling thread will wait
     * @throws AWSIotException
     *             exception thrown if the operation fails
     * @throws AWSIotTimeoutException
     *             exception thrown if the operation times out
     */
    @Override
    public void update(String jsonState, long timeout) throws AWSIotException, AWSIotTimeoutException {
        super.update(jsonState, timeout);
    }

    /**
     * Updates the content of a thing shadow with the data provided in the
     * request. This is a non-blocking call, so it immediately returns once is
     * the operation has been queued in the system. The result of the operation
     * will be notified through the callback functions, namely
     * {@link AWSIotMessage#onSuccess}, {@link AWSIotMessage#onFailure}, and
     * {@link AWSIotMessage#onTimeout}, one of which will be invoked after the
     * operation succeeded, failed, or timed out respectively.
     *
     * @param message
     *            the message object contains callback functions
     * @param timeout
     *            the timeout in milliseconds for the operation to be considered
     *            timed out
     * @throws AWSIotException
     *             exception thrown if the operation fails
     */
    @Override
    public void update(AWSIotMessage message, long timeout) throws AWSIotException {
        super.update(message, timeout);
    }

    /**
     * Deletes the content of a thing shadow. This is a blocking call, so the
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
    public void delete() throws AWSIotException {
        super.delete();
    }

    /**
     * Deletes the content of a thing shadow. This is a blocking call, so the
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
    public void delete(long timeout) throws AWSIotException, AWSIotTimeoutException {
        super.delete(timeout);
    }

    /**
     * Deletes the content of a thing shadow. This is a non-blocking call, so it
     * immediately returns once is the operation has been queued in the system.
     * The result of the operation will be notified through the callback
     * functions, namely {@link AWSIotMessage#onSuccess},
     * {@link AWSIotMessage#onFailure}, and {@link AWSIotMessage#onTimeout}, one
     * of which will be invoked after the operation succeeded, failed, or timed
     * out respectively.
     *
     * @param message
     *            the message object contains callback functions
     * @param timeout
     *            the timeout in milliseconds for the operation to be considered
     *            timed out
     * @throws AWSIotException
     *             exception thrown if the operation fails
     */
    @Override
    public void delete(AWSIotMessage message, long timeout) throws AWSIotException {
        super.delete(message, timeout);
    }

    /**
     * This function handles update messages received from the shadow. By
     * default, it invokes the setter methods provided for the annotated device
     * attributes. When there are multiple attribute changes received in one
     * shadow update, the order of invoking the setter methods are not defined.
     * One can override this function to provide their own implementation for
     * updating the device. The shadow update containing the delta (between the
     * 'desired' state and the 'reported' state) is passed in as an input
     * argument.
     *
     * @param jsonState
     *            the JSON document containing the delta between 'desired' and
     *            'reported' states
     */
    @Override
    public void onShadowUpdate(String jsonState) {
        super.onShadowUpdate(jsonState);
    }

    /**
     * This function handles collecting device data for reporting to the shadow.
     * By default, it invokes the getter methods provided for the annotated
     * device attributes. The data is serialized in a JSON document and reported
     * to the shadow. One could override this default implementation and provide
     * their own JSON document for reporting.
     *
     * @return the JSON document containing 'reported' state
     */
    @Override
    public String onDeviceReport() {
        return super.onDeviceReport();
    }

}
