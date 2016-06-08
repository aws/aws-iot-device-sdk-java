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

package com.amazonaws.services.iot.client.sample.shadow;

import java.util.Random;

import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotDeviceProperty;

/**
 * This class encapsulates an actual device. It extends {@link AWSIotDevice} to
 * define properties that are to be kept in sync with the AWS IoT shadow.
 */
public class ConnectedWindow extends AWSIotDevice {

    public ConnectedWindow(String thingName) {
        super(thingName);
    }

    @AWSIotDeviceProperty
    private boolean windowOpen;

    @AWSIotDeviceProperty
    private float roomTemperature;

    public boolean getWindowOpen() {
        // 1. read the window state from the window actuator
        boolean reportedState = this.windowOpen;
        System.out.println(
                System.currentTimeMillis() + " >>> reported window state: " + (reportedState ? "open" : "closed"));

        // 2. return the current window state
        return reportedState;
    }

    public void setWindowOpen(boolean desiredState) {
        // 1. update the window actuator with the desired state
        this.windowOpen = desiredState;

        System.out.println(
                System.currentTimeMillis() + " <<< desired window state to " + (desiredState ? "open" : "closed"));
    }

    public float getRoomTemperature() {
        // 1. Read the actual room temperature from the thermostat
        Random rand = new Random();
        float minTemperature = 20.0f;
        float maxTemperature = 85.0f;
        float reportedTemperature = rand.nextFloat() * (maxTemperature - minTemperature) + minTemperature;

        // 2. (optionally) update the local copy
        this.roomTemperature = reportedTemperature;

        // 3. return the current room temperature
        System.out.println(System.currentTimeMillis() + " >>> reported room temperature: " + reportedTemperature);
        return this.roomTemperature;
    }

    public void setRoomTemperature(float desiredTemperature) {
        // no-op as room temperature is a read-only property. It's not required
        // to have this setter.
    }

}
