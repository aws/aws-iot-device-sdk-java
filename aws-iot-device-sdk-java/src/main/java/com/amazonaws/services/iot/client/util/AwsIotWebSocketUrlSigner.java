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

package com.amazonaws.services.iot.client.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.amazonaws.services.iot.client.AWSIotException;

/**
 * The AWSIotWebSocketUrlSigner class creates the SigV4 signature and builds a
 * connection URL to be used with the Paho MQTT client.
 */
public class AwsIotWebSocketUrlSigner {

    /** Constant defining the algorithm use for hash calculation. */
    private static final String HASH_ALGORITHM = "SHA-256";
    /** Constant defining the algorithm use for MAC calculation. */
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    /** Constant defining the algorithm specifier in SigV4 parameters. */
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    /** Constant defining the key prefix string in SigV4 parameters. */
    private static final String KEY_PREFIX = "AWS4";
    /** Constant defining the terminator string in SigV4 parameters. */
    private static final String TERMINATOR = "aws4_request";
    /** Short date format pattern used in SigV4 parameters. */
    private static final String DATE_PATTERN = "yyyyMMdd";
    /** ISO 8601 date format pattern used in SigV4 signature parameters. */
    private static final String TIME_PATTERN = "yyyyMMdd'T'HHmmss'Z'";
    /** Default timezone used for converting signing date. */
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");
    /** Default charset used for URL encoding. */
    private static final String UTF8 = "UTF-8";
    /** Constant defining the HTTP method for the WebSocket connection. */
    private static final String METHOD = "GET";
    /** URI for WebSocket endpoint when doing initial HTTP operation. */
    private static final String CANONICAL_URI = "/mqtt";
    /** endpoint pattern used for validation and extracting region. */
    private static final Pattern endpointPattern = Pattern.compile("iot\\.([\\w-]+)\\.amazonaws\\.com(\\:\\d+)?$");

    /**
     * Given the region and service name provided to the client, the endpoint
     * and the current time return a signed connection URL to be used when
     * connecting via WebSocket to AWS IoT.
     * 
     * @param endpoint
     *            service endpoint with or without customer specific URL prefix.
     * @param awsAccessKeyId
     *            AWS access key ID used in SigV4 signature algorithm.
     * @param awsSecretAccessKey
     *            AWS secret access key used in SigV4 signature algorithm.
     * @param sessionToken
     *            Session token for temporary credentials.
     * @param signingDate
     *            time value to be used in SigV4 calculations. System current
     *            time will be used if null.
     * @return a URL with SigV4 signature formatted to be used with AWS IoT.
     * @throws AWSIotException
     *             Exception thrown when signed URL can be generated with given
     *             information.
     */
    public static String getSignedUrl(String endpoint, String awsAccessKeyId, String awsSecretAccessKey,
            String sessionToken, final Date signingDate) throws AWSIotException {
        if (endpoint == null || awsAccessKeyId == null || awsSecretAccessKey == null) {
            throw new IllegalArgumentException("Missing required data for signing");
        }

        endpoint = endpoint.trim().toLowerCase();
        String regionName = getRegionFromEndpoint(endpoint);
        if (regionName == null) {
            throw new IllegalArgumentException("Invalid endpoint provided");
        }

        String serviceName = "iotdata";
        awsAccessKeyId = awsAccessKeyId.trim();
        awsSecretAccessKey = awsSecretAccessKey.trim();

        Date dateToUse = signingDate;
        if (dateToUse == null) {
            dateToUse = new Date();
        }

        // SigV4 canonical string uses time in two formats
        String amzDate = getAmzDate(dateToUse);
        String dateStamp = getDateStamp(dateToUse);
        // Credential scoped to date and region
        String credentialScope = dateStamp + "/" + regionName + "/" + serviceName + "/aws4_request";
        // Now build the canonical string
        StringBuilder canonicalQueryStringBuilder = new StringBuilder();
        canonicalQueryStringBuilder.append("X-Amz-Algorithm=").append(ALGORITHM);
        canonicalQueryStringBuilder.append("&X-Amz-Credential=");
        try {
            canonicalQueryStringBuilder.append(URLEncoder.encode(awsAccessKeyId + "/" + credentialScope, UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new AWSIotException("Error encoding URL when building WebSocket URL");
        }
        canonicalQueryStringBuilder.append("&X-Amz-Date=").append(amzDate);
        canonicalQueryStringBuilder.append("&X-Amz-SignedHeaders=host");

        // headers and payload for the signing request
        // not used in an WebSocket URL, but encoded into the signature string
        String canonicalHeaders = "host:" + endpoint + "\n";
        String payloadHash = stringToHex(hash(""));

        // The request to sign includes the HTTP method, path, query string,
        // headers and payload
        String canonicalRequest = METHOD + "\n" + CANONICAL_URI + "\n" + canonicalQueryStringBuilder.toString() + "\n"
                + canonicalHeaders + "\nhost\n" + payloadHash;

        // Create a string to sign, generate a signing key...
        String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + credentialScope + "\n"
                + stringToHex(hash(canonicalRequest));
        byte[] signingKey = getSigningKey(dateStamp, regionName, serviceName, awsSecretAccessKey);
        // ...and sign the string.
        byte[] signatureBytes;
        try {
            signatureBytes = sign(stringToSign.getBytes(UTF8), signingKey);
        } catch (UnsupportedEncodingException e) {
            throw new AWSIotException("Error encoding URL when generating the signature bytes");
        }
        String signature = stringToHex(signatureBytes);

        // Add the signature to the query string.
        canonicalQueryStringBuilder.append("&X-Amz-Signature=");
        canonicalQueryStringBuilder.append(signature);

        // Now build the URL.
        String requestUrl = "wss://" + endpoint + CANONICAL_URI + "?" + canonicalQueryStringBuilder.toString();

        // If there are session credentials (from an STS server, AssumeRole, or
        // Amazon Cognito),
        // append the session token to the end of the URL string after signing.
        if (sessionToken != null) {
            sessionToken = sessionToken.trim();
            try {
                sessionToken = URLEncoder.encode(sessionToken, UTF8);
            } catch (UnsupportedEncodingException e) {
                throw new AWSIotException("Error encoding URL when appending session token to URL");
            }
            requestUrl += "&X-Amz-Security-Token=" + sessionToken;
        }

        return requestUrl;
    }

    private static String getRegionFromEndpoint(String endpoint) {
        Matcher matcher = endpointPattern.matcher(endpoint);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Converts byte data to a Hex-encoded string.
     *
     * @param data
     *            data to hex encode.
     * @return hex-encoded string.
     */
    private static String stringToHex(final byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i]);
            if (hex.length() == 1) {
                // Append leading zero.
                sb.append("0");
            } else if (hex.length() == 8) {
                // Remove ff prefix from negative numbers.
                hex = hex.substring(6);
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase();
    }

    /**
     * The SigV4 signing key is made up by consecutively hashing a number of
     * unique pieces of data.
     * 
     * @param dateStamp
     *            the current date in short date format.
     * @param regionName
     *            AWS region name.
     * @param serviceName
     *            service name for IoT service.
     * @param credentials
     *            AWS credential set to be used in signing.
     * @return byte array containing the SigV4 signing key.
     * @throws AWSIotException
     */
    private static byte[] getSigningKey(String dateStamp, String regionName, String serviceName,
            String awsSecretAccessKey) throws AWSIotException {
        // AWS4 uses a series of derived keys, formed by hashing different
        // pieces of data
        byte[] signingSecret;
        try {
            signingSecret = (KEY_PREFIX + awsSecretAccessKey).getBytes(UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new AWSIotException("Error encoding URL when generating the signing key");
        }
        byte[] signingDate = sign(dateStamp, signingSecret);
        byte[] signingRegion = sign(regionName, signingDate);
        byte[] signingService = sign(serviceName, signingRegion);
        return sign(TERMINATOR, signingService);
    }

    /**
     * Given the input epoch time returns a String of the proper format for the
     * ISO 8601 date + time in SigV4 parameters.
     * 
     * @param date
     *            desired date.
     * @return date formatted string in ISO 8601 date + time format.
     */
    private static String getAmzDate(final Date date) {
        SimpleDateFormat fomatter = new SimpleDateFormat(TIME_PATTERN);
        fomatter.setTimeZone(TIME_ZONE);
        return fomatter.format(date);
    }

    /**
     * Given the input epoch time returns a String of the proper format for the
     * short date in SigV4 parameters.
     * 
     * @param date
     *            desired date.
     * @return date formatted string in short date format.
     */
    private static String getDateStamp(final Date date) {
        SimpleDateFormat fomatter = new SimpleDateFormat(DATE_PATTERN);
        fomatter.setTimeZone(TIME_ZONE);
        return fomatter.format(date);
    }

    /**
     * Hashes the string contents (assumed to be UTF-8) using the SHA-256
     * algorithm.
     *
     * @param text
     *            The string to hash.
     * @return The hashed bytes from the specified string.
     * @throws AmazonClientException
     *             If the hash cannot be computed.
     */
    private static byte[] hash(String text) throws AWSIotException {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(text.getBytes(UTF8));
            return md.digest();
        } catch (Exception e) {
            throw new AWSIotException("Unable to compute hash while signing request: " + e.getMessage());
        }
    }

    /**
     * Sign the given string with the key provided.
     *
     * @param stringData
     *            String to be signed.
     * @param key
     *            the key for signing.
     * @return a byte array containing the signed string.
     * @throws AmazonClientException
     *             in the case of a signature error.
     */
    private static byte[] sign(String stringData, final byte[] key) throws AWSIotException {
        try {
            byte[] data = stringData.getBytes(UTF8);
            return sign(data, key);
        } catch (Exception e) {
            throw new AWSIotException("Unable to calculate a request signature: " + e.getMessage());
        }
    }

    /**
     * Sign the given data with the key provided.
     *
     * @param data
     *            byte buffer of data to be signed.
     * @param key
     *            the key for signing.
     * @return a byte array containing the signed string.
     * @throws AmazonClientException
     *             in the case of a signature error.
     */
    private static byte[] sign(byte[] data, final byte[] key) throws AWSIotException {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new AWSIotException("Unable to calculate a request signature: " + e.getMessage());
        }
    }

}
