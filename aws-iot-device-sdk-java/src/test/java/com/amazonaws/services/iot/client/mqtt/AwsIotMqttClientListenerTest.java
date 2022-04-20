package com.amazonaws.services.iot.client.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.Future;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotMqttClientListenerTest {

    private static final String TEST_TOPIC = "test/topic";
    private static final byte[] TEST_PAYLOAD = "test payload".getBytes();

    @Mock
    private AbstractAwsIotClient client;
    @Mock
    private AwsIotMqttConnection connection;

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
    public void testConnectionLost() {
        AwsIotMqttClientListener listener = new AwsIotMqttClientListener(client);

        listener.connectionLost(null);

        verify(connection, times(1)).onConnectionFailure();
    }

    @Test
    public void testOnMessageArrived() throws Exception {
        AwsIotMqttClientListener listener = new AwsIotMqttClientListener(client);

        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(0);
        mqttMessage.setPayload(TEST_PAYLOAD);

        ArgumentCaptor<AWSIotMessage> messageCaptor = ArgumentCaptor.forClass(AWSIotMessage.class);
        doNothing().when(client).dispatch(messageCaptor.capture());

        listener.messageArrived(TEST_TOPIC, mqttMessage);

        verify(client, times(1)).dispatch(any(AWSIotMessage.class));
        assertTrue(Arrays.equals(TEST_PAYLOAD, messageCaptor.getValue().getPayload()));
        assertEquals(AWSIotQos.QOS0, messageCaptor.getValue().getQos());
    }

}
