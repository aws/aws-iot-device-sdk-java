package com.amazonaws.services.iot.client.greengrass;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

public class DiscoveryHandler {
    private static char[] BLANK_PASSWORD = "".toCharArray();

    /**
     * Gets the discovery information for a specific thing, in the specified region, using the certificate and private key files given
     *
     * @param thingName
     * @param regionName
     * @param certificate
     * @param privateKey
     * @return
     * @throws CertificateException
     * @throws IOException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     * @throws NoSuchProviderException
     */
    public DiscoveryInfo getDiscoveryInfo(String thingName, String regionName, Certificate certificate, PrivateKey privateKey) throws GeneralSecurityException, IOException {
        DiscoveryInfo discoveryInfo = new DiscoveryInfo();

        // HTTPS with client certificate support adapted from https://stackoverflow.com/a/28883926

        // Create a keystore with the private key and certificate
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry("", privateKey, BLANK_PASSWORD, new Certificate[]{certificate});

        // Create a key manager from our keystore
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, BLANK_PASSWORD);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        // Build the default trust manager. Not specifying a keystore here gets the trusted CAs from the operating system.
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        // Build the SSL/TLS context and socket factory with the key manager (our cert) and trust manager (trusted CAs)
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        // Get a URL and connection for the discovery URL in the specified region for the specified thing
        URL url = new URL("https://greengrass-ats.iot." + regionName + ".amazonaws.com:8443/greengrass/discover/thing/" + thingName);
        URLConnection urlConnection = url.openConnection();

        // Create the HTTPS connection with our SSL/TLS socket factory
        HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlConnection;
        httpsUrlConnection.setSSLSocketFactory(sslSocketFactory);

        try {
            // Read all of the data from the connection - from https://stackoverflow.com/questions/4328711/read-url-to-string-in-few-lines-of-java-code
            String discoveryJson = new Scanner(httpsUrlConnection.getInputStream(), "UTF-8").useDelimiter("\\A").next();

            // Use Jackson to convert the JSON to the discovery info object and return it
            discoveryInfo = new ObjectMapper().readValue(discoveryJson, DiscoveryInfo.class);
            return discoveryInfo;
        } catch (FileNotFoundException e) {
            // Greengrass discovery info has not been populated
            discoveryInfo.discoveryThrowable = e;
            discoveryInfo.discoveryError = DiscoveryError.GreengrassGroupsDiscoveryInfoHasNotBeenPopulated;
            return discoveryInfo;
        } catch (IOException e) {
            discoveryInfo.discoveryThrowable = e;

            String message = e.getMessage();

            if (message.contains("HTTP response code: 403")) {
                // Discovery information for device not found
                discoveryInfo.discoveryError = DiscoveryError.DiscoveryInfoForThingNotFound;

                return discoveryInfo;
            }

            if (message.contains("certificate_unknown")) {
                // Device certificate not found in AWS IoT
                discoveryInfo.discoveryError = DiscoveryError.DeviceCertificateNotKnownToAwsIoT;

                return discoveryInfo;
            }

            // Unknown issue
            discoveryInfo.discoveryError = DiscoveryError.Unknown;
            return discoveryInfo;
        }
    }

    public List<String> getGroupCAPems(DiscoveryInfo discoveryInfo) {
        if ((discoveryInfo == null) || (discoveryInfo.GGGroups == null)) {
            return null;
        }

        List<String> groupCaPems = discoveryInfo.GGGroups.stream()
                .filter(Objects::nonNull)
                .filter(discoveryGroupInfo -> discoveryGroupInfo.CAs != null)
                .flatMap(discoveryGroupInfo -> discoveryGroupInfo.CAs.stream())
                .collect(Collectors.toList());

        return groupCaPems;
    }

    public List<GreengrassEndpoint> getGreengrassEndpoints(DiscoveryInfo discoveryInfo) {
        if ((discoveryInfo == null) || (discoveryInfo.GGGroups == null)) {
            return null;
        }

        return discoveryInfo.GGGroups.stream()
                .filter(Objects::nonNull)
                .filter(discoveryGroupInfo -> discoveryGroupInfo.Cores != null)
                .flatMap(discoveryGroupInfo -> discoveryGroupInfo.Cores.stream())
                .filter(discoveryCoreInfo -> discoveryCoreInfo.Connectivity != null)
                .flatMap(discoveryCoreInfo -> discoveryCoreInfo.Connectivity.stream())
                .map(discoveryCoreConnectivityInfo -> new GreengrassEndpoint(discoveryCoreConnectivityInfo.HostAddress, discoveryCoreConnectivityInfo.PortNumber))
                .collect(Collectors.toList());
    }
}
