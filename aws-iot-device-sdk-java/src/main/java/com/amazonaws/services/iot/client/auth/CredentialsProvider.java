/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package com.amazonaws.services.iot.client.auth;

public interface CredentialsProvider {

    Credentials getCredentials();

}
