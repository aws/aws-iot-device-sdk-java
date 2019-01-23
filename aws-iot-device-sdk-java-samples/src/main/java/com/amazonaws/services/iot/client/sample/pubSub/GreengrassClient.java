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

import com.amazonaws.services.iot.client.AWSIotMqttClient;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.List;

public class GreengrassClient extends AWSIotMqttClient {
    public GreengrassClient(String clientEndpoint, String clientId, KeyStore keyStore, String keyPassword, List<Certificate> trustedCaList) {
        super(clientEndpoint, clientId, keyStore, keyPassword, trustedCaList);
    }
}
