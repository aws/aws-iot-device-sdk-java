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

package com.amazonaws.services.iot.client.core;

import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.mqtt.AwsIotMqttConnection;
import com.amazonaws.services.iot.client.util.AwsIotWebSocketUrlSigner;

/**
 * This is a thin layer on top of {@link AwsIotMqttConnection} that provides a
 * WebSocket based communication channel to the MQTT implementation.
 */
public class AwsIotWebsocketConnection extends AwsIotMqttConnection {

    public AwsIotWebsocketConnection(AbstractAwsIotClient client, String awsAccessKeyId, String awsSecretAccessKey)
            throws AWSIotException {
        this(client, awsAccessKeyId, awsSecretAccessKey, null);
    }

    public AwsIotWebsocketConnection(AbstractAwsIotClient client, String awsAccessKeyId, String awsSecretAccessKey,
            String sessionToken) throws AWSIotException {
        super(client, null, "wss://" + client.getClientEndpoint() + ":443");

        // Port number must be included in the endpoint for signing otherwise
        // the signature verification will fail. This is because the Paho client
        // library (1.0.3) always includes port number in the host line of the HTTP
        // request header, e.g "Host: data.iot.us-east-1.amazonaws.com:443".
        String signedUrl = AwsIotWebSocketUrlSigner.getSignedUrl(client.getClientEndpoint() + ":443", awsAccessKeyId,
                awsSecretAccessKey, sessionToken, null);
        Set<String> uris = new HashSet<>();
        uris.add(signedUrl);
        this.setServerUris(uris);
    }

}
