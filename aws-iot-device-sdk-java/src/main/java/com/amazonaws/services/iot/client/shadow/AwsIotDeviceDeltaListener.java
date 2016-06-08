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
import java.util.logging.Logger;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * This class extends {@link AWSIotTopic} to provide a callback function for
 * receiving the shadow delta updates.
 */
public class AwsIotDeviceDeltaListener extends AWSIotTopic {

    private static final Logger LOGGER = Logger.getLogger(AwsIotDeviceDeltaListener.class.getName());

    private final AbstractAwsIotDevice device;

    public AwsIotDeviceDeltaListener(String topic, AWSIotQos qos, AbstractAwsIotDevice device) {
        super(topic, qos);
        this.device = device;
    }

    @Override
    public void onMessage(AWSIotMessage message) {
        String payload = message.getStringPayload();
        if (payload == null) {
            LOGGER.warning("Received empty delta for device " + device.getThingName());
            return;
        }

        JsonNode rootNode;
        try {
            rootNode = device.getJsonObjectMapper().readTree(payload);
            if (!rootNode.isObject()) {
                throw new IOException();
            }
        } catch (IOException e) {
            LOGGER.warning("Received invalid delta for device " + device.getThingName());
            return;
        }

        if (device.enableVersioning) {
            JsonNode node = rootNode.get("version");
            if (node == null) {
                LOGGER.warning("Missing version field in delta for device " + device.getThingName());
                return;
            }

            long receivedVersion = node.longValue();
            long localVersion = device.getLocalVersion().get();
            if (receivedVersion < localVersion) {
                LOGGER.warning("An old version of delta received for " + device.getThingName() + ", local "
                        + localVersion + ", received " + receivedVersion);
                return;
            }

            device.getLocalVersion().set(receivedVersion);
            LOGGER.info("Local version number updated to " + receivedVersion);
        }

        JsonNode node = rootNode.get("state");
        if (node == null) {
            LOGGER.warning("Missing state field in delta for device " + device.getThingName());
            return;
        }
        device.onShadowUpdate(node.toString());
    }

    @Override
    public void onSuccess() {
    }

    @Override
    public void onFailure() {
        LOGGER.warning("Failed to subscribe to device topic " + topic);
    }

    @Override
    public void onTimeout() {
        LOGGER.warning("Timeout when subscribing to device topic " + topic);
    }

}
