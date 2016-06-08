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

import java.util.logging.Logger;

import com.amazonaws.services.iot.client.AWSIotDeviceErrorCode;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;

public class AwsIotDeviceReportMessage extends AWSIotMessage {

    private static final Logger LOGGER = Logger.getLogger(AwsIotDeviceReportMessage.class.getName());

    private final AbstractAwsIotDevice device;
    private final long reportVersion;

    public AwsIotDeviceReportMessage(String topic, AWSIotQos qos, long reportVersion, String jsonState,
            AbstractAwsIotDevice device) {
        super(topic, qos, jsonState);
        this.device = device;
        this.reportVersion = reportVersion;
    }

    @Override
    public void onSuccess() {
        // increment local version only if it hasn't be updated
        device.getLocalVersion().compareAndSet(reportVersion, reportVersion + 1);
    }

    @Override
    public void onFailure() {
        if (AWSIotDeviceErrorCode.CONFLICT.equals(errorCode)) {
            LOGGER.warning("Device version conflict, restart version synchronization");
            device.startVersionSync();
        } else {
            LOGGER.warning("Failed to publish device report: " + errorMessage);
        }
    }

}
