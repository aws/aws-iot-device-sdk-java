package com.amazonaws.services.iot.client.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.iot.client.AWSIotException;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotWebsocketConnectionTest {

    private final String ClientId = "test";
    private final String EndPoint = "iot.us-east-1.AMAZONAWS.COM";
    private final String AccessKeyId = "123";
    private final String SecretAccessKey = "456";
    private final String NewAccessKeyId = "new123";

    @Mock
    private AbstractAwsIotClient client;

    private AwsIotWebsocketConnection connection;

    @Before
    public void setup() throws AWSIotException {
        when(client.getClientEndpoint()).thenReturn(EndPoint);
        when(client.getClientId()).thenReturn(ClientId);

        connection = new AwsIotWebsocketConnection(client, AccessKeyId, SecretAccessKey);
    }

    @Test
    public void testGetServerUris() throws AWSIotException {
        Set<String> uris = connection.getServerUris();

        assertEquals(1, uris.size());
    }

    @Test
    public void testUpdateCredentails() throws AWSIotException {
        Set<String> uris1 = connection.getServerUris();

        connection.updateCredentials(NewAccessKeyId, SecretAccessKey, null);
        Set<String> uris2 = connection.getServerUris();

        assertEquals(1, uris1.size());
        assertEquals(1, uris2.size());
        assertTrue(!uris1.equals(uris2));
    }
}
