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

package com.amazonaws.services.iot.client.sample.pubSub;

import com.amazonaws.services.iot.client.*;
import com.amazonaws.services.iot.client.greengrass.DiscoveryHandler;
import com.amazonaws.services.iot.client.greengrass.DiscoveryInfo;
import com.amazonaws.services.iot.client.greengrass.GreengrassEndpoint;
import com.amazonaws.services.iot.client.sample.sampleUtil.CommandArguments;
import com.amazonaws.services.iot.client.sample.sampleUtil.PrivateKeyReader;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is an example that uses {@link AWSIotMqttClient} to subscribe to a topic and
 * publish messages to it on a Greengrass Core. Both blocking and non-blocking publishing are
 * demonstrated in this example.
 */
public class GreengrassDiscoverySample {
    private static final String TestTopic = "sdk/test/java";
    private static final AWSIotQos TestTopicQos = AWSIotQos.QOS0;
    private AWSIotMqttClient awsIotMqttClient;
    private CommandArguments commandArguments;

    public static void main(String args[]) throws Exception {
        CommandArguments commandArguments = CommandArguments.parse(args);

        GreengrassDiscoverySample greengrassDiscoverySample = new GreengrassDiscoverySample();
        greengrassDiscoverySample.run(commandArguments);
    }

    private AWSIotMqttClient initAndConnectClient() throws Exception {
        String thingName = commandArguments.getNotNull("thingName", SampleUtil.getConfig("thingName"));
        String region = commandArguments.getNotNull("region", SampleUtil.getConfig("region"));

        String certificateFile = commandArguments.getNotNull("certificateFile", SampleUtil.getConfig("certificateFile"));
        String privateKeyFile = commandArguments.getNotNull("privateKeyFile", SampleUtil.getConfig("privateKeyFile"));

        DiscoveryHandler discoveryHandler = new DiscoveryHandler();
        PrivateKey privateKey = PrivateKeyReader.getPrivateKey(privateKeyFile);
        List<Certificate> certificates = SampleUtil.loadCertificatesFromFile(certificateFile);

        DiscoveryInfo discoveryInfo = discoveryHandler.getDiscoveryInfo(thingName, region, certificates.get(0), privateKey);

        if (discoveryInfo.discoveryError != null) {
            throw new RuntimeException(String.format("Discovery error [%s]", discoveryInfo.discoveryError));
        }

        // Convert all of the PEM strings to certificates
        List<Certificate> trustedCaList = discoveryHandler.getGroupCAPems(discoveryInfo)
                .stream()
                .flatMap(pem -> {
                    try {
                        return SampleUtil.getCertificatesFromInputStream(new ByteArrayInputStream(pem.getBytes())).stream();
                    } catch (CertificateException e) {
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());

        List<GreengrassEndpoint> greengrassEndpoints = discoveryHandler.getGreengrassEndpoints(discoveryInfo);

        if (certificateFile == null || privateKeyFile == null) {
            throw new IllegalArgumentException("Failed to construct client due to missing certificate or credentials.");
        }

        String algorithm = commandArguments.get("keyAlgorithm", SampleUtil.getConfig("keyAlgorithm"));

        KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile, algorithm);

        // Loop through all of the endpoints that were returned
        for (GreengrassEndpoint greengrassEndpoint : greengrassEndpoints) {
            if (greengrassEndpoint.address.equals("127.0.0.1")) {
                // Skip localhost to avoid delays, remove this if you need to connect to localhost
                continue;
            }

            // Get the endpoint information as a colon separated string (HOST:PORT)
            String endpoint = greengrassEndpoint.toString();

            AWSIotMqttClient awsIotMqttClient = new AWSIotMqttClient(endpoint, thingName, pair.keyStore, pair.keyPassword, trustedCaList);

            // Set connection retries to zero so that on a failure the client will disconnect and we will retry discovery
            awsIotMqttClient.setMaxConnectionRetries(0);

            try {
                // Use a short delay for demo purposes
                awsIotMqttClient.connect();

                // Client connected, subscribe to the test topic and return to the caller
                AWSIotTopic topic = new TestTopicListener(TestTopic, TestTopicQos);
                awsIotMqttClient.subscribe(topic, true);

                return awsIotMqttClient;
            } catch (AWSIotException e) {
                // Client failed to connect, keep checking endpoints
            }
        }

        // No valid endpoints found
        throw new RuntimeException("No valid endpoints found");
    }

    private void run(CommandArguments commandArguments) throws Exception {
        this.commandArguments = commandArguments;

        while (true) {
            try {
                Thread blockingPublishThread = new Thread(new BlockingPublisher());
                Thread nonBlockingPublishThread = new Thread(new NonBlockingPublisher());

                blockingPublishThread.start();
                nonBlockingPublishThread.start();

                blockingPublishThread.join();
                nonBlockingPublishThread.join();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private AWSIotMqttClient getAwsIotMqttClient() throws Exception {
        synchronized (this) {
            if ((awsIotMqttClient != null) && (awsIotMqttClient.getConnectionStatus().equals(AWSIotConnectionStatus.DISCONNECTED))) {
                System.out.println("Greengrass client disconnected");
                awsIotMqttClient = null;
            }

            if (awsIotMqttClient == null) {
                System.out.println("Connecting Greengrass client");
                awsIotMqttClient = initAndConnectClient();
            }

            return awsIotMqttClient;
        }
    }

    private class BlockingPublisher implements Runnable {
        @Override
        public void run() {
            long counter = 1;

            while (true) {
                String payload = "hello from blocking publisher - " + (counter++);

                try {
                    getAwsIotMqttClient().publish(TestTopic, payload);
                } catch (RuntimeException e) {
                    System.err.println(System.currentTimeMillis() + ": " + e.getMessage());
                } catch (Exception e) {
                    System.err.println(System.currentTimeMillis() + ": publish failed for " + payload);
                }

                System.out.println(System.currentTimeMillis() + ": >>> " + payload);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println(System.currentTimeMillis() + ": BlockingPublisher was interrupted");
                    return;
                }
            }
        }
    }

    private class NonBlockingPublisher implements Runnable {
        @Override
        public void run() {
            long counter = 1;

            while (true) {
                String payload = "hello from non-blocking publisher - " + (counter++);

                AWSIotMessage message = new NonBlockingPublishListener(TestTopic, TestTopicQos, payload);

                try {
                    getAwsIotMqttClient().publish(message);
                } catch (RuntimeException e) {
                    System.err.println(System.currentTimeMillis() + ": " + e.getMessage());
                } catch (Exception e) {
                    System.err.println(System.currentTimeMillis() + ": publish failed for " + payload);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println(System.currentTimeMillis() + ": NonBlockingPublisher was interrupted");
                    return;
                }
            }
        }
    }
}
