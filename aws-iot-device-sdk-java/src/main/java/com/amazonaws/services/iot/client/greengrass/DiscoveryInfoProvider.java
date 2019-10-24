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

package com.amazonaws.services.iot.client.greengrass;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.core.AwsIotRuntimeException;
import com.amazonaws.services.iot.client.util.AwsIotTlsSocketFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.logging.Logger;

/**
 * The class that provides functionality to perform a Greengrass discovery process to the cloud.
 */
public class DiscoveryInfoProvider {

    private static final Logger LOGGER = Logger.getLogger(DiscoveryInfoProvider.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String endpoint;
    private final Integer port;
    private final Integer timeoutSec;
    private final SSLSocketFactory sslSocketFactory;

    /**
     * <p>
     * Constructs a new instance of DiscoveryInfoProvider with port it is 8443 by default
     * and timeout 120 seconds by default.
     * </p>
     *
     * @param endpoint
     *            the client endpoint in the form of {@code <account-specific
     *            prefix>.iot.<aws-region>.amazonaws.com}. The account-specific
     *            prefix can be found on the AWS IoT console or by using the
     *            {@code describe-endpoint} command through the AWS command line
     *            interface.
     * @param keyStore
     *            the key store containing the client X.509 certificate and
     *            private key. The {@link KeyStore} object can be constructed
     *            using X.509 certificate file and private key file created on
     *            the AWS IoT console. For more details, please refer to the
     *            README file of this SDK.
     * @param keyPassword
     *            the key password protecting the private key in the
     *            {@code keyStore} argument.
     */
    public DiscoveryInfoProvider(String endpoint, KeyStore keyStore, String keyPassword) {
        this(endpoint, 8443, keyStore, keyPassword);
    }

    /**
     * <p>
     * Constructs a new instance of DiscoveryInfoProvider with timeout 120 seconds by default.
     * </p>
     *
     * @param endpoint
     *            the client endpoint in the form of {@code <account-specific
     *            prefix>.iot.<aws-region>.amazonaws.com}. The account-specific
     *            prefix can be found on the AWS IoT console or by using the
     *            {@code describe-endpoint} command through the AWS command line
     *            interface.
     * @param port
     *            the port number to connect to. For discovery purpose,
     *            it is 8443 by default.
     * @param keyStore
     *            the key store containing the client X.509 certificate and
     *            private key. The {@link KeyStore} object can be constructed
     *            using X.509 certificate file and private key file created on
     *            the AWS IoT console. For more details, please refer to the
     *            README file of this SDK.
     * @param keyPassword
     *            the key password protecting the private key in the
     *            {@code keyStore} argument.
     */
    public DiscoveryInfoProvider(String endpoint, Integer port, KeyStore keyStore, String keyPassword) {
        this(endpoint, port, 120, keyStore, keyPassword);
    }

    /**
     * <p>
     * Constructs a new instance of DiscoveryInfoProvider.
     * </p>
     *
     * @param endpoint
     *            the client endpoint in the form of {@code <account-specific
     *            prefix>.iot.<aws-region>.amazonaws.com}. The account-specific
     *            prefix can be found on the AWS IoT console or by using the
     *            {@code describe-endpoint} command through the AWS command line
     *            interface.
     * @param port
     *            the port number to connect to. For discovery purpose,
     *            it is 8443 by default.
     * @param timeoutSec
     *            time out configuration in seconds to consider a discovery
     *            request sending/response waiting has been timed out.
     * @param keyStore
     *            the key store containing the client X.509 certificate and
     *            private key. The {@link KeyStore} object can be constructed
     *            using X.509 certificate file and private key file created on
     *            the AWS IoT console. For more details, please refer to the
     *            README file of this SDK.
     * @param keyPassword
     *            the key password protecting the private key in the
     *            {@code keyStore} argument.
     */
    public DiscoveryInfoProvider(String endpoint, Integer port, Integer timeoutSec, KeyStore keyStore, String keyPassword) {
        this.endpoint = endpoint;
        this.port = port;
        this.timeoutSec = timeoutSec;

        try {
            this.sslSocketFactory = new AwsIotTlsSocketFactory(keyStore, keyPassword, null);
        } catch (AWSIotException e) {
            throw new AwsIotRuntimeException(e);
        }
    }

    /**
     * <p>
     * Perform the discovery request for the given Greengrass aware device thing name.
     * </p>
     *
     * @param thingName
     *            greengrass aware device thing name.
     * @return DiscoveryInfo object.
     * @throws DiscoveryException unable discover thing.
     */
    public DiscoveryInfo discover(String thingName) throws DiscoveryException {
        LOGGER.info("Starting discover request...");
        LOGGER.info("Endpoint: " + endpoint + ":" + port);
        LOGGER.info("Target thing: " + thingName);

        try {
            URL discoveryUrl = makeDiscoveryURL(endpoint, port, thingName);

            HttpsURLConnection connection = (HttpsURLConnection) discoveryUrl.openConnection();
            connection.setConnectTimeout(timeoutSec * 1000);
            connection.setSSLSocketFactory(sslSocketFactory);

            return parseResponse(DiscoveryInfo.class, connection.getInputStream());
        } catch (IOException e) {
            final String message = e.getMessage();
            if (message.contains("HTTP response code: 403")) {
                throw new DiscoveryException("Discovery Info for Thing Not Found", e);
            } else if (message.contains("certificate_unknown")) {
                throw new DiscoveryException("Device Certificate Not Known to AWS IoT", e);
            } else {
                throw new DiscoveryException(
                    "Occurred I/O exception while retrieve Discover Information from AWS IoT Greengrass", e);
            }
        }
    }

    private static URL makeDiscoveryURL(String endpoint, Integer port, String thingName) {
        final String url = String.format(
            "https://%s:%s/greengrass/discover/thing/%s", endpoint, port, thingName);

        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new DiscoveryException("Provided malformed discovery URL", e);
        }
    }

    private static <T> T parseResponse(Class<T> clazz, InputStream object) throws DiscoveryException {
        try {
            return OBJECT_MAPPER.readValue(object, clazz);
        } catch (Exception e) {
            throw new DiscoveryException("Unable to de-serialize bytes into object", e);
        }
    }

}
