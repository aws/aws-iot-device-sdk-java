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

package com.amazonaws.services.iot.client.sample.shadowEcho;

import java.io.IOException;

import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.sample.sampleUtil.CommandArguments;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This example demonstrates how to use {@link AWSIotDevice} to directly access
 * the shadow document.
 */
public class ShadowEchoSample {

    private static AWSIotMqttClient awsIotClient;

    public static void setClient(AWSIotMqttClient client) {
        awsIotClient = client;
    }

    private static void initClient(CommandArguments arguments) {
        String clientEndpoint = arguments.getNotNull("clientEndpoint", SampleUtil.getConfig("clientEndpoint"));
        String clientId = arguments.getNotNull("clientId", SampleUtil.getConfig("clientId"));

        String certificateFile = arguments.get("certificateFile", SampleUtil.getConfig("certificateFile"));
        String privateKeyFile = arguments.get("privateKeyFile", SampleUtil.getConfig("privateKeyFile"));
        if (awsIotClient == null && certificateFile != null && privateKeyFile != null) {
            String algorithm = arguments.get("keyAlgorithm", SampleUtil.getConfig("keyAlgorithm"));
            KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile, algorithm);

            awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword);
        }

        if (awsIotClient == null) {
            String awsAccessKeyId = arguments.get("awsAccessKeyId", SampleUtil.getConfig("awsAccessKeyId"));
            String awsSecretAccessKey = arguments.get("awsSecretAccessKey", SampleUtil.getConfig("awsSecretAccessKey"));
            String sessionToken = arguments.get("sessionToken", SampleUtil.getConfig("sessionToken"));

            if (awsAccessKeyId != null && awsSecretAccessKey != null) {
                awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey,
                        sessionToken);
            }
        }

        if (awsIotClient == null) {
            throw new IllegalArgumentException("Failed to construct client due to missing certificate or credentials.");
        }
    }

    public static void main(String args[]) throws IOException, AWSIotException, AWSIotTimeoutException,
            InterruptedException {
        CommandArguments arguments = CommandArguments.parse(args);
        initClient(arguments);

        String thingName = arguments.getNotNull("thingName", SampleUtil.getConfig("thingName"));
        AWSIotDevice device = new AWSIotDevice(thingName);

        awsIotClient.attach(device);
        awsIotClient.connect();

        // Delete existing document if any
        device.delete();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Thing thing = new Thing();

        while (true) {
            long desired = thing.state.desired.counter;
            thing.state.reported.counter = desired;
            thing.state.desired.counter = desired + 1;

            String jsonState = objectMapper.writeValueAsString(thing);

            try {
                // Send updated document to the shadow
                device.update(jsonState);
                System.out.println(System.currentTimeMillis() + ": >>> " + jsonState);
            } catch (AWSIotException e) {
                System.out.println(System.currentTimeMillis() + ": update failed for " + jsonState);
                continue;
            }

            try {
                // Retrieve updated document from the shadow
                String shadowState = device.get();
                System.out.println(System.currentTimeMillis() + ": <<< " + shadowState);

                thing = objectMapper.readValue(shadowState, Thing.class);
            } catch (AWSIotException e) {
                System.out.println(System.currentTimeMillis() + ": get failed for " + jsonState);
                continue;
            }

            Thread.sleep(1000);
        }

    }

}
