package com.amazonaws.services.iot.client.mqtt;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;
import com.amazonaws.services.iot.client.core.AwsIotMessageCallback;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotMqttConnectionListenerTest {

    @Mock
    private AbstractAwsIotClient client;
    @Mock
    private AwsIotMqttConnection connection;
    @Mock
    AwsIotMessageCallback userCallback;

    @Before
    public void setup() {
        doAnswer(new Answer<Future<?>>() {
            @Override
            public Future<?> answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return null;
            }
        }).when(client).scheduleTask(any(Runnable.class));

        when(client.getConnection()).thenReturn(connection);
    }

    @Test
    public void testOnConnectSuccess() {
        AwsIotMqttConnectionListener listener = new AwsIotMqttConnectionListener(client, true, userCallback);

        listener.onSuccess(null);

        verify(connection, times(1)).onConnectionSuccess();
        verify(userCallback, times(1)).onSuccess();
    }

    @Test
    public void testOnConnectFailure() {
        AwsIotMqttConnectionListener listener = new AwsIotMqttConnectionListener(client, true, userCallback);

        listener.onFailure(null, null);

        verify(connection, times(1)).onConnectionFailure();
        verify(userCallback, times(1)).onFailure();
    }

    @Test
    public void testOnDisconnectSuccess() {
        AwsIotMqttConnectionListener listener = new AwsIotMqttConnectionListener(client, false, userCallback);

        listener.onSuccess(null);

        verify(connection, times(1)).onConnectionClosed();
        verify(userCallback, times(1)).onSuccess();
    }

    @Test
    public void testOnDisconnectFailure() {
        AwsIotMqttConnectionListener listener = new AwsIotMqttConnectionListener(client, false, userCallback);

        listener.onFailure(null, null);

        verify(connection, times(1)).onConnectionClosed();
        verify(userCallback, times(1)).onFailure();
    }

}
