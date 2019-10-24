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

package com.amazonaws.services.iot.client.sample.greengrass;

import com.amazonaws.services.iot.client.*;
import com.amazonaws.services.iot.client.greengrass.ConnectivityInfo;
import com.amazonaws.services.iot.client.greengrass.CoreConnectivityInfo;
import com.amazonaws.services.iot.client.greengrass.DiscoveryInfo;
import com.amazonaws.services.iot.client.greengrass.DiscoveryInfoProvider;
import com.amazonaws.services.iot.client.sample.sampleUtil.CommandArguments;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.getConfig;

public class BasicDiscovery {

    private static final ObjectMapper ObjectMapper = new ObjectMapper();
    private static final String TestTopic = "sdk/test/java";
    private static final AWSIotQos TestTopicQos = AWSIotQos.QOS0;

    private static String appMode;
    private static AWSIotMqttClient awsIotMqttClient;

    private static void initClient(final CommandArguments args) throws CertificateException {
        // Read in command-line parameters
        String clientEndpoint = args.getNotNull("clientEndpoint", getConfig("clientEndpoint"));
        String thingName = args.getNotNull("thingName", getConfig("thingName"));

        String certificateFile = args.getNotNull("certificateFile", getConfig("certificateFile"));
        String privateKeyFile = args.getNotNull("privateKeyFile", getConfig("privateKeyFile"));
        String algorithm = args.get("keyAlgorithm", getConfig("keyAlgorithm"));
        if (certificateFile == null || privateKeyFile == null) {
            throw new IllegalArgumentException(
                "Missing credentials for authentication, you must specify --certificateFile and --privateKeyFile args.");
        }

        SampleUtil.KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(
            certificateFile, privateKeyFile, algorithm);

        appMode = args.getNotNull("mode", "both");
        if (!Arrays.asList("both", "publish", "subscribe").contains(appMode)) {
            throw new IllegalArgumentException(String.format(
                "Unknown --mode option '%s'. Must be one of [\"both\", \"publish\", \"subscribe\"]", appMode));
        }

        awsIotMqttClient = connectToGGC(clientEndpoint, thingName, pair.keyStore, pair.keyPassword);
    }

    private static AWSIotMqttClient connectToGGC(String clientEndpoint, String thingName,
                                                 KeyStore keyStore, String keyPassword) throws CertificateException {
        // Discover GGCs
        DiscoveryInfoProvider discoveryInfoProvider =
            new DiscoveryInfoProvider(clientEndpoint, 8443, 10, keyStore, keyPassword);
        DiscoveryInfo discoveryInfo = discoveryInfoProvider.discover(thingName);

        // We only pick the first ca and core info
        CoreConnectivityInfo core = discoveryInfo.getAllCores().get(0);
        List<Certificate> trustedCAs = discoveryInfo.getAllCas();

        // Iterate through all connection options for the core and use the first successful one
        for (ConnectivityInfo connectivityInfo : core.getConnectivity()) {
            final String coreEndpoint = connectivityInfo.getHostAddress() + ":" + connectivityInfo.getPortNumber();
            System.out.println("Trying to connect to core at " + coreEndpoint);

            AWSIotMqttClient client = new AWSIotMqttClient(coreEndpoint, thingName, keyStore, keyPassword, trustedCAs);
            client.setMaxConnectionRetries(0);

            try {
                client.connect();

                return client;
            } catch (AWSIotException e) {
                System.err.println("Error in connect!");
                System.err.println("Error message: " + e.getMessage());
            }
        }

        throw new RuntimeException(
            String.format("Cannot connect to core %s.", core.getThingArn()));
    }

    public static void main(String[] args) throws Exception {
        CommandArguments arguments = CommandArguments.parse(args);
        initClient(arguments);

        if ("both".equals(appMode) || "subscribe".equals(appMode)) {
            awsIotMqttClient.subscribe(new AWSIotTopic(TestTopic, TestTopicQos) {
                @Override
                public void onMessage(AWSIotMessage message) {
                    System.out.printf("Received message on topic %s: %s\n",
                        message.getTopic(), message.getStringPayload());
                }
            });
        }

        long loopCount = 0;
        while (true) {
            if ("both".equals(appMode) || "publish".equals(appMode)) {
                final Map<String, Object> msg = new HashMap<>();
                msg.put("message", "Hello World!");
                msg.put("sequence", loopCount++);
                final String json = ObjectMapper.writeValueAsString(msg);

                awsIotMqttClient.publish(TestTopic, AWSIotQos.QOS0, json);
                if ("publish".equals(appMode)) {
                    System.out.printf("Published topic %s: %s\n", TestTopic, json);
                }
            }

            Thread.sleep(1000);
        }
    }

}
