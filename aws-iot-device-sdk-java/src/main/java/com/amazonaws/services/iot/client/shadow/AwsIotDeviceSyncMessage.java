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

import com.amazonaws.services.iot.client.AWSIotDeviceErrorCode;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;

public class AwsIotDeviceSyncMessage extends AWSIotMessage {

    private static final Logger LOGGER = Logger.getLogger(AwsIotDeviceSyncMessage.class.getName());

    private final AbstractAwsIotDevice device;

    public AwsIotDeviceSyncMessage(String topic, AWSIotQos qos, AbstractAwsIotDevice device) {
        super(topic, qos);
        this.device = device;
    }

    @Override
    public void onSuccess() {
        if (payload != null) {
            try {
                long version = AwsIotJsonDeserializer.deserializeVersion(device, getStringPayload());
                if (version > 0) {
                    LOGGER.info("Received shadow version number: " + version);

                    boolean updated = device.getLocalVersion().compareAndSet(-1, version);
                    if (!updated) {
                        LOGGER.warning(
                                "Local version not updated likely because newer version recieved from shadow update");
                    }
                }
            } catch (IOException e) {
                LOGGER.warning("Device update error: " + e.getMessage());
            }
        }
    }

    @Override
    public void onFailure() {
        if (AWSIotDeviceErrorCode.NOT_FOUND.equals(errorCode)) {
            LOGGER.info("No shadow document found, reset local version to 0");
            device.getLocalVersion().set(0);
        } else {
            LOGGER.warning("Failed to get shadow version: " + errorMessage);
        }
    }
    
}
