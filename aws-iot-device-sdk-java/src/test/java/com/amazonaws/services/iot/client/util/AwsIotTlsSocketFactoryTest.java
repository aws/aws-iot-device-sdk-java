package com.amazonaws.services.iot.client.util;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotTlsSocketFactoryTest {

    @Mock
    private SSLSocketFactory sslSocketFactory;
    @Mock
    private SSLSocket socket;

    private AwsIotTlsSocketFactory tlsSocketFactory;

    @Before
    public void setup() {
        tlsSocketFactory = new AwsIotTlsSocketFactory(sslSocketFactory);
    }

    @Test
    public void getDefaultCipherSuites() {
        tlsSocketFactory.getDefaultCipherSuites();

        verify(sslSocketFactory, times(1)).getDefaultCipherSuites();
    }

    @Test
    public void getSupportedCipherSuites() {
        tlsSocketFactory.getSupportedCipherSuites();

        verify(sslSocketFactory, times(1)).getSupportedCipherSuites();
    }

    @Test
    public void testCreateSocket() throws IOException {
        when(sslSocketFactory.createSocket()).thenReturn(socket);

        tlsSocketFactory.createSocket();

        verify(socket, times(1)).setEnabledProtocols(new String[] { "TLSv1.2" });
    }

    @Test
    public void testCreateSocketWithSocket() throws IOException {
        when(sslSocketFactory.createSocket(nullable(Socket.class), anyString(), anyInt(), anyBoolean())).thenReturn(socket);

        tlsSocketFactory.createSocket(nullable(Socket.class), anyString(), anyInt(), anyBoolean());

        verify(socket, times(1)).setEnabledProtocols(new String[] { "TLSv1.2" });
    }

    @Test
    public void testCreateSocketWithHost() throws IOException, UnknownHostException {
        when(sslSocketFactory.createSocket(anyString(), anyInt())).thenReturn(socket);

        tlsSocketFactory.createSocket(anyString(), anyInt());

        verify(socket, times(1)).setEnabledProtocols(new String[] { "TLSv1.2" });
    }

    @Test
    public void testCreateSocketWithInetAddress() throws IOException, UnknownHostException {
        when(sslSocketFactory.createSocket(anyString(), anyInt(), nullable(InetAddress.class), anyInt())).thenReturn(socket);

        tlsSocketFactory.createSocket(anyString(), anyInt(), nullable(InetAddress.class), anyInt());

        verify(socket, times(1)).setEnabledProtocols(new String[] { "TLSv1.2" });
    }

    @Test
    public void testCreateSocketWithInetAddressAndPort() throws IOException {
        when(sslSocketFactory.createSocket(nullable(InetAddress.class), anyInt())).thenReturn(socket);

        tlsSocketFactory.createSocket(nullable(InetAddress.class), anyInt());

        verify(socket, times(1)).setEnabledProtocols(new String[] { "TLSv1.2" });
    }

    @Test
    public void testCreateSocketWithInetAddressAndLocalInetAddress() throws IOException {
        when(sslSocketFactory.createSocket(nullable(InetAddress.class), anyInt(), nullable(InetAddress.class), anyInt()))
                .thenReturn(socket);

        tlsSocketFactory.createSocket(nullable(InetAddress.class), anyInt(), nullable(InetAddress.class), anyInt());

        verify(socket, times(1)).setEnabledProtocols(new String[] { "TLSv1.2" });
    }

}
