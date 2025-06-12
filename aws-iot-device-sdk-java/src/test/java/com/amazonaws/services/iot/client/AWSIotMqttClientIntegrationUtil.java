package com.amazonaws.services.iot.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.services.iot.client.CredentialUtil;

public class AWSIotMqttClientIntegrationUtil {

    private static final String CLIENT_ENDPOINT = "ajje7lpljulm4-ats.iot.us-east-1.amazonaws.com";
    private static final String CLIENT_ID = UUID.randomUUID().toString();

    private static final String AUTH_MODE = System.getProperty("authMode");

    private static final Boolean IS_WEBSOCKET = Boolean.parseBoolean(System.getProperty("isWebSocket"));
    private static final String PUBLIC_MATERIAL = "arn:aws:secretsmanager:us-east-1:123124136734:secret:V1IotSdkIntegrationTestCertificate-vTRwjE";
    private static final String PRIVATE_MATERIAL = "arn:aws:secretsmanager:us-east-1:123124136734:secret:V1IotSdkIntegrationTestPrivateKey-vNUQU8";
    private static final String KEYSTORE_FILE = System.getProperty("keystoreFile");
    private static final String KEYSTORE_PASSWORD = System.getProperty("keystorePassword");
    private static final String KEY_PASSWORD = System.getProperty("keyPassword");

    private static final String LOG_LEVEL = System.getProperty("logLevel");

    public static void enableConsoleLogging(Logger logger) {
        Level level = Level.FINE;

        if (LOG_LEVEL != null) {
            level = Level.parse(LOG_LEVEL);
        }

        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);

        logger.addHandler(consoleHandler);
        logger.setUseParentHandlers(false);
        logger.setLevel(level);
    }

    public static AWSIotMqttClient getClient() {
        return getClient("");
    }

    public static AWSIotMqttClient getClient(String suffix) {
        assertNotNull("Client endpoint not provided", CLIENT_ENDPOINT);
        assertNotNull("Client ID not provided", CLIENT_ID);

        AWSIotMqttClient client = getClientFromAutoConfig(suffix);
        if (client == null) {
            client = getClientFromManualConfig(suffix);
        }

        return client;
    }

    private static AWSIotMqttClient getClientFromAutoConfig(final String suffix) {
        AWSIotMqttClient client = null;

        if (AUTH_MODE != null) {
            switch (AUTH_MODE) {
                case AuthMode.CERT_AUTH: // CredentialUtil handles cert from odin to generate client as well. : )
                case AuthMode.WSS_SIGV4_AUTH:
                    client = CredentialUtil.newClient(CLIENT_ENDPOINT, CLIENT_ID + suffix, PUBLIC_MATERIAL, PRIVATE_MATERIAL, IS_WEBSOCKET);
                    break;
                default:
                    throw new UnsupportedOperationException("No such auth mode supported: " + AUTH_MODE);
            }
        }

        return client;
    }

    private static AWSIotMqttClient getClientFromManualConfig(final String suffix) {
        AWSIotMqttClient client = null;

        if (KEYSTORE_FILE != null && KEYSTORE_PASSWORD != null) {
            try {
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD.toCharArray());

                client = new AWSIotMqttClient(CLIENT_ENDPOINT, CLIENT_ID + suffix, keyStore, KEY_PASSWORD);
            } catch (Exception e) {
                fail("Failed to load keystore file for the integration tests");
            }
        } else if (PUBLIC_MATERIAL != null && PRIVATE_MATERIAL != null) {
            client = CredentialUtil.newClient(CLIENT_ENDPOINT, CLIENT_ID + suffix, PUBLIC_MATERIAL, PRIVATE_MATERIAL, IS_WEBSOCKET);
        }

        return client;
    }

    private class AuthMode {
        private static final String CERT_AUTH = "CertificateMutualAuthentication";
        private static final String WSS_SIGV4_AUTH = "MqttOverWebSocketSigV4Signing";
        private static final String WSS_CUSTOM_AUTH = "MqttOverWebSocketCustomAuthZ"; // Reserved for custom authZ integ test
    }

}
