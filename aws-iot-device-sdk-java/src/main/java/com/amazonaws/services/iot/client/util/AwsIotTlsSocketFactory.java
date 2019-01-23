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

import com.amazonaws.services.iot.client.AWSIotException;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;

/**
 * This class extends {@link SSLSocketFactory} to enforce TLS v1.2 to be used
 * for SSL sockets created by the library.
 */
public class AwsIotTlsSocketFactory extends SSLSocketFactory {
    private static final String TLS_V_1_2 = "TLSv1.2";

    /**
     * SSL Socket Factory A SSL socket factory is created and passed into this
     * class which decorates it to enable TLS 1.2 when sockets are created.
     */
    private final SSLSocketFactory sslSocketFactory;

    public AwsIotTlsSocketFactory(KeyStore keyStore, String keyPassword, List<Certificate> trustedCaList) throws AWSIotException {
        try {
            SSLContext context = SSLContext.getInstance(TLS_V_1_2);

            TrustManager[] trustManagers = null;

            if (trustedCaList != null) {
                KeyStore caKeyStore = KeyStore.getInstance("JKS");
                caKeyStore.load(null, null);

                for (Certificate additionalTrustedCertificate : trustedCaList) {
                    caKeyStore.setCertificateEntry("", additionalTrustedCertificate);
                }

                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

                // Get the trust managers for our group CAs
                // NOTE: Trying to create a list of trust managers that includes the operating system's default trust
                //         managers will fail. If you receive this exception:
                //
                //         javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
                //
                //         Then it is likely you are using the wrong trust manager or that the certificate has rotated
                //         and discovery needs to be performed again to get the new group CA.
                trustManagerFactory.init(caKeyStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            }

            KeyManagerFactory managerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            managerFactory.init(keyStore, keyPassword.toCharArray());
            context.init(managerFactory.getKeyManagers(), trustManagers, null);

            sslSocketFactory = context.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | KeyManagementException | IOException | CertificateException e) {
            throw new AWSIotException(e);
        }
    }

    public AwsIotTlsSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return sslSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return sslSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return ensureTls(sslSocketFactory.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return ensureTls(sslSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return ensureTls(sslSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException, UnknownHostException {
        return ensureTls(sslSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return ensureTls(sslSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        return ensureTls(sslSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    /**
     * Enable TLS 1.2 on any socket created by the underlying SSL Socket
     * Factory.
     *
     * @param socket newly created socket which may not have TLS 1.2 enabled.
     * @return TLS 1.2 enabled socket.
     */
    private Socket ensureTls(Socket socket) {
        if (socket != null && (socket instanceof SSLSocket)) {
            ((SSLSocket) socket).setEnabledProtocols(new String[]{TLS_V_1_2});

            // Ensure hostname is validated againt the CN in the certificate
            SSLParameters sslParams = new SSLParameters();
            sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            ((SSLSocket) socket).setSSLParameters(sslParams);
        }
        return socket;
    }

}
