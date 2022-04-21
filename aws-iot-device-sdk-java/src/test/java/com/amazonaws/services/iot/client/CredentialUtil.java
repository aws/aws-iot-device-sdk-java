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
package com.amazonaws.services.iot.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import com.amazonaws.services.iot.client.AWSIotMqttClient;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.*;
/**
 * A helper class for constructing {@link AWSIotMqttClient} from a given Odin
 * material set. Supported material set type are certificate pair and credential
 * pair.
 */
public class CredentialUtil {
    public static final String AWS_REGION = "us-east-1";
    // Use UID to distinguish concurrent running test 
    public static final String TEST_UID = UUID.randomUUID().toString();



    /**
     * Construct {@link AWSIotMqttClient} from a given Odin material set.
     *
     * @param retriever
     *            the Odin materialset retriever
     * @param clientEndpoint
     *            the client end point
     * @param clientId
     *            the client id
     * @param publicMaterialARN
     *            the public material containing either the certificate(TLS) or
     *            aws access key id(Websocket).
     * @param privateMaterialARN
     *            the private material containing either the private key(TLS) or
     *            aws secret access key(Websocket).
     * @return the client instance
     */
    public static AWSIotMqttClient newClient(String clientEndpoint, String clientId, String publicMaterialARN,
            String privateMateiralARN, Boolean isWebSocket) {

        String publicMaterial = CredentialUtil.getSecret(publicMaterialARN);
        String privateMaterial = CredentialUtil.getSecret(privateMateiralARN);

        if (isWebSocket == false) {
            return newMqttTlsClient(clientEndpoint, clientId+TEST_UID, publicMaterial,
                    privateMaterial);
        } else {
            return new AWSIotMqttClient(clientEndpoint, clientId+TEST_UID, publicMaterial, privateMaterial);
        }
    }

    /**
     * Construct a TLS based client.
     *
     * @param clientEndpoint
     *            the client end point
     * @param clientId
     *            the client id
     * @param certificateData
     *            the certificate data
     * @param privateKeyData
     *            the private key data
     * @return the client instance
     */
    public static AWSIotMqttClient newMqttTlsClient(String clientEndpoint, String clientId, String certificateString,
            String privateKeyString) {
        KeyStore keyStore;
        String keyPassword;
        
        try {
            Certificate certificate = loadCertificate(certificateString.getBytes("UTF-8"));
            PrivateKey privateKey = loadPrivateKey(formatPrivateKey(privateKeyString));

            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            keyStore.setCertificateEntry("alias", certificate);

            // randomly generated key password for the key in the KeyStore
            keyPassword = new BigInteger(128, new SecureRandom()).toString(32);
            keyStore.setKeyEntry("alias", privateKey, keyPassword.toCharArray(), new Certificate[] { certificate });
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new IllegalArgumentException("Failed to construct keystore from the materail set data");
        }

        return new AWSIotMqttClient(clientEndpoint, clientId, keyStore, keyPassword);
    }

    /**
     * Construct {@link Certificate} instance from decoded certificate data.
     *
     * @param data
     *            the raw certificate data
     * @return the certificate
     */
    private static Certificate loadCertificate(byte[] data) {
        Certificate certificate = null;

        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            certificate = certFactory.generateCertificate(new ByteArrayInputStream(data));
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Failed to decode certificate from the materail set");

        }

        return certificate;
    }

    /**
     * Construct {@link PrivateKey} instance from decoded private key data.
     *
     * @param data
     *            the raw private key data
     * @return the private key
     */
    private static PrivateKey loadPrivateKey(byte[] data) {
        PrivateKey privateKey = null;

        try {
            KeyFactory keyfactory = KeyFactory.getInstance("RSA");
            privateKey = keyfactory.generatePrivate(new PKCS8EncodedKeySpec(data));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("Failed to decode private key from the materail set");
        }

        return privateKey;
    }

    /**
     * Formart private key and decode it to byte[].
     *
     * @param privateKeyString
     *            private key String
     * @return the decoded private key
     */
    private static byte[] formatPrivateKey(String privateKeyString) {
        String formattedKey = privateKeyString
                .replace("-----BEGIN PRIVATE KEY-----\n", "")
                .replace("\n-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(formattedKey);
    }

    ///////////////////////////////////////////////////////////////////
    // Retrieve AWS Credential
    ///////////////////////////////////////////////////////////////////
    // Original Code from AWS Service
    // If you need more information about configurations or implementing the sample code, visit the AWS docs:
    // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-samples.html#prerequisites

    public static String getSecret(String secretName) {
        // Create a Secrets Manager client
        AWSSecretsManager client  = AWSSecretsManagerClientBuilder.standard()
                                        .withRegion(AWS_REGION)
                                        .build();
        
        // In this sample we only handle the specific exceptions for the 'GetSecretValue' API.
        // See https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html
        // We rethrow the exception by default.
        
        String secret;
        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
                        .withSecretId(secretName);
        GetSecretValueResult getSecretValueResult = null;

        try {
            getSecretValueResult = client.getSecretValue(getSecretValueRequest);
        } catch (DecryptionFailureException e) {
            // Secrets Manager can't decrypt the protected secret text using the provided KMS key.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw e;
        } catch (InternalServiceErrorException e) {
            // An error occurred on the server side.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw e;
        } catch (InvalidParameterException e) {
            // You provided an invalid value for a parameter.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw e;
        } catch (InvalidRequestException e) {
            // You provided a parameter value that is not valid for the current state of the resource.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw e;
        } catch (ResourceNotFoundException e) {
            // We can't find the resource that you asked for.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw e;
        }

        // Decrypts secret using the associated KMS key.
        // Depending on whether the secret is a string or binary, one of these fields will be populated.
        if (getSecretValueResult.getSecretString() != null) {
            secret = getSecretValueResult.getSecretString();
        }
        else {
            secret = new String(Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());
        }


        return secret;
    }

}
