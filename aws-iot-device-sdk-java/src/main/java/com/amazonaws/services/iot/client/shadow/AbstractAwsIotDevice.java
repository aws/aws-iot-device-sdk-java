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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.services.iot.client.AWSIotConfig;
import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotDeviceProperty;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;
import com.amazonaws.services.iot.client.shadow.AwsIotDeviceCommandManager.Command;
import com.amazonaws.services.iot.client.shadow.AwsIotDeviceCommandManager.CommandAck;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.Getter;
import lombok.Setter;

/**
 * The actual implementation of {@link AWSIotDevice}.
 */
@Getter
@Setter
public abstract class AbstractAwsIotDevice {

    private static final Logger LOGGER = Logger.getLogger(AbstractAwsIotDevice.class.getName());

    protected final String thingName;

    protected long reportInterval = AWSIotConfig.DEVICE_REPORT_INTERVAL;
    protected boolean enableVersioning = AWSIotConfig.DEVICE_ENABLE_VERSIONING;
    protected AWSIotQos deviceReportQos = AWSIotQos.valueOf(AWSIotConfig.DEVICE_REPORT_QOS);
    protected AWSIotQos shadowUpdateQos = AWSIotQos.valueOf(AWSIotConfig.DEVICE_SHADOW_UPDATE_QOS);
    protected AWSIotQos methodQos = AWSIotQos.valueOf(AWSIotConfig.DEVICE_METHOD_QOS);
    protected AWSIotQos methodAckQos = AWSIotQos.valueOf(AWSIotConfig.DEVICE_METHOD_ACK_QOS);

    private final Map<String, Field> reportedProperties;
    private final Map<String, Field> updatableProperties;
    private final AwsIotDeviceCommandManager commandManager;
    private final ConcurrentMap<String, Boolean> deviceSubscriptions;
    private final ObjectMapper jsonObjectMapper;

    private AbstractAwsIotClient client;
    private Future<?> syncTask;
    private AtomicLong localVersion;

    protected AbstractAwsIotDevice(String thingName) {
        this.thingName = thingName;

        reportedProperties = getDeviceProperties(true, false);
        updatableProperties = getDeviceProperties(false, true);
        commandManager = new AwsIotDeviceCommandManager(this);

        deviceSubscriptions = new ConcurrentHashMap<>();
        for (String topic : getDeviceTopics()) {
            deviceSubscriptions.put(topic, false);
        }

        jsonObjectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(AbstractAwsIotDevice.class, new AwsIotJsonSerializer());
        jsonObjectMapper.registerModule(module);

        localVersion = new AtomicLong(-1);
    }

    protected AbstractAwsIotDevice getDevice() {
        return this;
    }

    protected String get() throws AWSIotException {
        AWSIotMessage message = new AWSIotMessage(null, methodQos);
        return commandManager.runCommandSync(Command.GET, message);
    }

    protected String get(long timeout) throws AWSIotException, AWSIotTimeoutException {
        AWSIotMessage message = new AWSIotMessage(null, methodQos);
        return commandManager.runCommandSync(Command.GET, message, timeout);
    }

    protected void get(AWSIotMessage message, long timeout) throws AWSIotException {
        commandManager.runCommand(Command.GET, message, timeout);
    }

    protected void update(String jsonState) throws AWSIotException {
        AWSIotMessage message = new AWSIotMessage(null, methodQos, jsonState);
        commandManager.runCommandSync(Command.UPDATE, message);
    }

    protected void update(String jsonState, long timeout) throws AWSIotException, AWSIotTimeoutException {
        AWSIotMessage message = new AWSIotMessage(null, methodQos, jsonState);
        commandManager.runCommandSync(Command.UPDATE, message, timeout);
    }

    protected void update(AWSIotMessage message, long timeout) throws AWSIotException {
        commandManager.runCommand(Command.UPDATE, message, timeout);
    }

    protected void delete() throws AWSIotException {
        AWSIotMessage message = new AWSIotMessage(null, methodQos);
        commandManager.runCommandSync(Command.DELETE, message);
    }

    protected void delete(long timeout) throws AWSIotException, AWSIotTimeoutException {
        AWSIotMessage message = new AWSIotMessage(null, methodQos);
        commandManager.runCommandSync(Command.DELETE, message, timeout);
    }

    protected void delete(AWSIotMessage message, long timeout) throws AWSIotException {
        commandManager.runCommand(Command.DELETE, message, timeout);
    }

    protected void onShadowUpdate(String jsonState) {
        // synchronized block to serialize device accesses
        synchronized (this) {
            try {
                AwsIotJsonDeserializer.deserialize(this, jsonState);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to update device", e);
            }
        }
    }

    protected String onDeviceReport() {
        // synchronized block to serialize device accesses
        synchronized (this) {
            try {
                return jsonObjectMapper.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                LOGGER.log(Level.WARNING, "Failed to generate device report", e);
                return null;
            }
        }
    }

    public void activate() throws AWSIotException {
        stopSync();

        for (String topic : getDeviceTopics()) {
            AWSIotTopic awsIotTopic;

            if (commandManager.isDeltaTopic(topic)) {
                awsIotTopic = new AwsIotDeviceDeltaListener(topic, shadowUpdateQos, this);
            } else {
                awsIotTopic = new AwsIotDeviceCommandAckListener(topic, methodAckQos, this);
            }

            client.subscribe(awsIotTopic, client.getServerAckTimeout());
        }

        startSync();
    }

    public void deactivate() throws AWSIotException {
        stopSync();

        commandManager.onDeactivate();

        for (String topic : getDeviceTopics()) {
            deviceSubscriptions.put(topic, false);

            AWSIotTopic awsIotTopic = new AWSIotTopic(topic);
            client.unsubscribe(awsIotTopic, client.getServerAckTimeout());
        }
    }

    public boolean isTopicReady(String topic) {
        Boolean status = deviceSubscriptions.get(topic);

        return Boolean.TRUE.equals(status);
    }

    public boolean isCommandReady(Command command) {
        Boolean accepted = deviceSubscriptions.get(commandManager.getTopic(command, CommandAck.ACCEPTED));
        Boolean rejected = deviceSubscriptions.get(commandManager.getTopic(command, CommandAck.REJECTED));

        return (Boolean.TRUE.equals(accepted) && Boolean.TRUE.equals(rejected));
    }

    public void onSubscriptionAck(String topic, boolean success) {
        deviceSubscriptions.put(topic, success);
        commandManager.onSubscriptionAck(topic, success);
    }

    public void onCommandAck(AWSIotMessage message) {
        commandManager.onCommandAck(message);
    }

    protected void startSync() {
        // don't start the publish task if no properties are to be published
        if (reportedProperties.isEmpty() || reportInterval <= 0) {
            return;
        }

        syncTask = client.scheduleRoutineTask(new Runnable() {
            @Override
            public void run() {
                if (!isCommandReady(Command.UPDATE)) {
                    LOGGER.fine("Device not ready for reporting");
                    return;
                }
                
                long reportVersion = localVersion.get();
                if (enableVersioning && reportVersion < 0) {
                    // if versioning is enabled, synchronize the version first
                    LOGGER.fine("Starting version sync");
                    startVersionSync();
                    return;
                }

                String jsonState = onDeviceReport();
                if (jsonState != null) {
                    LOGGER.fine("Sending device report");
                    sendDeviceReport(reportVersion, jsonState);
                }
            }
        }, 0l, reportInterval);
    }

    protected void stopSync() {
        if (syncTask != null) {
            syncTask.cancel(false);
            syncTask = null;
        }

        localVersion.set(-1);
    }

    protected void startVersionSync() {
        localVersion.set(-1);

        AwsIotDeviceSyncMessage message = new AwsIotDeviceSyncMessage(null, shadowUpdateQos, this);
        try {
            commandManager.runCommand(Command.GET, message, client.getServerAckTimeout(), true);
        } catch (AWSIotTimeoutException e) {
            // async command, shouldn't receive timeout exception
        } catch (AWSIotException e) {
            LOGGER.log(Level.WARNING, "Failed to publish version update message", e);
        }
    }

    private void sendDeviceReport(long reportVersion, String jsonState) {
        StringBuilder payload = new StringBuilder("{");

        if (enableVersioning) {
            payload.append("\"version\":").append(reportVersion).append(",");
        }
        payload.append("\"state\":{\"reported\":").append(jsonState).append("}}");

        AwsIotDeviceReportMessage message = new AwsIotDeviceReportMessage(null, shadowUpdateQos, reportVersion,
                payload.toString(), this);
        if (enableVersioning && reportVersion != localVersion.get()) {
            LOGGER.warning("Local version number has changed, skip reporting for this round");
            return;
        }

        try {
            commandManager.runCommand(Command.UPDATE, message, client.getServerAckTimeout(), true);
        } catch (AWSIotTimeoutException e) {
            // async command, shouldn't receive timeout exception
        } catch (AWSIotException e) {
            LOGGER.log(Level.WARNING, "Failed to publish device report message", e);
        }
    }

    private Map<String, Field> getDeviceProperties(boolean enableReport, boolean allowUpdate) {
        Map<String, Field> properties = new HashMap<>();

        for (Field field : this.getClass().getDeclaredFields()) {
            AWSIotDeviceProperty annotation = field.getAnnotation(AWSIotDeviceProperty.class);
            if (annotation == null) {
                continue;
            }

            String propertyName = annotation.name().length() > 0 ? annotation.name() : field.getName();
            if ((enableReport && annotation.enableReport()) || (allowUpdate && annotation.allowUpdate())) {
                properties.put(propertyName, field);
            }
        }

        return properties;
    }

    private List<String> getDeviceTopics() {
        List<String> topics = new ArrayList<>();

        topics.add(commandManager.getTopic(Command.DELTA, null));

        topics.add(commandManager.getTopic(Command.GET, CommandAck.ACCEPTED));
        topics.add(commandManager.getTopic(Command.GET, CommandAck.REJECTED));
        topics.add(commandManager.getTopic(Command.UPDATE, CommandAck.ACCEPTED));
        topics.add(commandManager.getTopic(Command.UPDATE, CommandAck.REJECTED));
        topics.add(commandManager.getTopic(Command.DELETE, CommandAck.ACCEPTED));
        topics.add(commandManager.getTopic(Command.DELETE, CommandAck.REJECTED));

        return topics;
    }

}
