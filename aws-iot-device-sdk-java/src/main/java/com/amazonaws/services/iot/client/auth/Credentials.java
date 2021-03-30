/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package com.amazonaws.services.iot.client.auth;

/**
 * A class representing a set of AWS credentials.
 */
public class Credentials {

    private String accessKeyId;
    private String secretAccessKey;
    private String sessionToken;

    public Credentials(String accessKeyId, String secretAccessKey) {
        this(accessKeyId, secretAccessKey, null);
    }

    public Credentials(String accessKeyId, String secretAccessKey, String sessionToken) {
        this.accessKeyId = accessKeyId.trim();
        this.secretAccessKey = secretAccessKey;
        this.sessionToken = sessionToken;
    }

    public String getAccessKeyId() { return accessKeyId; }
    public String getSecretAccessKey() { return secretAccessKey; }
    public String getSessionToken() { return sessionToken; }
}
