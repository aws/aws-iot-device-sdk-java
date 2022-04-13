package com.amazonaws.services.iot.client.mqtt;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;

import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSuback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotMqttMessageListenerTest {

    @Mock
    private AbstractAwsIotClient client;
    @Mock
    private AWSIotMessage message;
    @Mock
    private MqttToken token;
    @Mock
    private MqttSuback subAck;

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

        when(token.getUserContext()).thenReturn(message);
    }

    @Test
    public void testOnSuccess() {
        AwsIotMqttMessageListener listener = new AwsIotMqttMessageListener(client);
        listener.onSuccess(token);

        verify(message, times(1)).onSuccess();
    }

    @Test
    public void testOnSuccessWithSubAckAccepted() {
        AwsIotMqttMessageListener listener = new AwsIotMqttMessageListener(client);

        when(token.getResponse()).thenReturn(subAck);
        when(subAck.getGrantedQos()).thenReturn(new int[] { 1 });

        listener.onSuccess(token);

        verify(message, times(1)).onSuccess();
    }

    @Test
    public void testOnSuccessWithSubAckRejected() {
        AwsIotMqttMessageListener listener = new AwsIotMqttMessageListener(client);

        when(token.getResponse()).thenReturn(subAck);
        when(subAck.getGrantedQos()).thenReturn(new int[] { 128 });

        listener.onSuccess(token);

        verify(message, times(1)).onFailure();
    }

    @Test
    public void testOnFailure() {
        AwsIotMqttMessageListener listener = new AwsIotMqttMessageListener(client);
        listener.onFailure(token, null);

        verify(message, times(1)).onFailure();
    }

}
