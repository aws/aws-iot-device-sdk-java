/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package com.amazonaws.services.iot.client.auth;

public class StaticCredentialsProvider implements CredentialsProvider {

    private Credentials credentials;

    public StaticCredentialsProvider(Credentials credentials) {
        this.credentials = credentials;
    }

    public Credentials getCredentials() {
        return credentials;
    }
}
