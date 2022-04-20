package com.amazonaws.services.iot.client.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.util.Arrays;
import java.util.concurrent.Future;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;
import com.amazonaws.services.iot.client.core.AwsIotRetryableException;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotMqttConnectionTest {

    private static final String USERNAME_METRIC_PREFIX = "?SDK=Java&Version=";

    @Mock
    private AbstractAwsIotClient client;
    @Mock
    private MqttAsyncClient mqttClient;

    private AwsIotMqttConnection connection;
    private MqttConnectOptions options;
    private String mqttTopic;
    private MqttMessage mqttMessage;
    private int mqttQos;

    @Before
    public void setup() throws AWSIotException {
        lenient().doAnswer(new Answer<Future<?>>() {
            @Override
            public Future<?> answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return null;
            }
        }).when(client).scheduleTask(nullable(Runnable.class));

        when(client.getConnectionTimeout()).thenReturn(3000);
        when(client.getKeepAliveInterval()).thenReturn(3000);
        when(client.getWillMessage()).thenReturn(new AWSIotMessage("will/topic", AWSIotQos.QOS1, "will payload"));
        when(client.isCleanSession()).thenReturn(true);

        connection = new AwsIotMqttConnection(client, mqttClient);
    }

    public void testOpenConnection() throws AWSIotException, MqttSecurityException, MqttException {
        doAnswer(new Answer<IMqttToken>() {
            @Override
            public IMqttToken answer(InvocationOnMock invocation) throws Throwable {
                options = (MqttConnectOptions) invocation.getArguments()[0];
                return null;
            }
        }).when(mqttClient).connect(nullable(MqttConnectOptions.class), nullable(Object.class), nullable(IMqttActionListener.class));

        connection.openConnection(null);

        verify(mqttClient).connect(nullable(MqttConnectOptions.class), nullable(Object.class), nullable(IMqttActionListener.class));

        assertEquals(true, options.isCleanSession());
        assertEquals(null, options.getSocketFactory());
        assertEquals(3, options.getConnectionTimeout());
        assertEquals(3, options.getKeepAliveInterval());
        assertEquals("will/topic", options.getWillDestination());
        assertEquals(1, options.getWillMessage().getQos());
        assertTrue(Arrays.equals("will payload".getBytes(), options.getWillMessage().getPayload()));

    }

    @Test
    public void testOpenConnectionMetricsEnabled() throws AWSIotException, MqttSecurityException, MqttException {
        when(client.isClientEnableMetrics()).thenReturn(true);
        testOpenConnection();
        assertTrue(options.getUserName().startsWith(USERNAME_METRIC_PREFIX));
    }

    @Test
    public void testOpenConnectionMetricsDisabled() throws AWSIotException, MqttSecurityException, MqttException {
        when(client.isClientEnableMetrics()).thenReturn(false);
        testOpenConnection();
        assertNull(options.getUserName());
    }

    @Test(expected = AWSIotException.class)
    public void testOpenConnectionException() throws AWSIotException, MqttSecurityException, MqttException {
        when(mqttClient.connect(nullable(MqttConnectOptions.class), nullable(Object.class), nullable(IMqttActionListener.class)))
                .thenThrow(new MqttException(0));

        connection.openConnection(null);
    }

    @Test
    public void testCloseConnection() throws MqttException, AWSIotException {
        lenient().when(mqttClient.disconnect(anyInt(), nullable(Object.class), nullable(IMqttActionListener.class)))
                .thenReturn(new MqttToken());

        connection.closeConnection(null);

        verify(mqttClient).disconnect(anyLong(), nullable(Object.class), nullable(IMqttActionListener.class));
    }

    @Test(expected = AWSIotException.class)
    public void testCloseConnectionException() throws MqttException, AWSIotException {
        when(mqttClient.disconnect(anyLong(), nullable(Object.class), nullable(IMqttActionListener.class)))
                .thenThrow(new MqttException(0));

        connection.closeConnection(null);
    }

    @Test
    public void testPublishMessage()
            throws MqttPersistenceException, MqttException, AWSIotException, AwsIotRetryableException {
        AWSIotMessage message = new AWSIotMessage("test/topic", AWSIotQos.QOS1, "payload");

        doAnswer(new Answer<IMqttToken>() {
            @Override
            public IMqttToken answer(InvocationOnMock invocation) throws Throwable {
                mqttTopic = (String) invocation.getArguments()[0];
                mqttMessage = (MqttMessage) invocation.getArguments()[1];
                return null;
            }
        }).when(mqttClient).publish(nullable(String.class), nullable(MqttMessage.class), nullable(Object.class),
                nullable(IMqttActionListener.class));

        connection.publishMessage(message);

        verify(mqttClient).publish(nullable(String.class), nullable(MqttMessage.class), nullable(Object.class),
                nullable(IMqttActionListener.class));
        assertEquals("test/topic", mqttTopic);
        assertEquals(1, mqttMessage.getQos());
        assertTrue(Arrays.equals("payload".getBytes(), mqttMessage.getPayload()));
    }

    @Test(expected = AWSIotException.class)
    public void testPublishMessageException()
            throws MqttPersistenceException, MqttException, AWSIotException, AwsIotRetryableException {
        AWSIotMessage message = new AWSIotMessage("test/topic", AWSIotQos.QOS1, "payload");

        when(mqttClient.publish(nullable(String.class), nullable(MqttMessage.class), nullable(Object.class),
                nullable(IMqttActionListener.class))).thenThrow(new MqttException(0));

        connection.publishMessage(message);
    }

    @Test(expected = AwsIotRetryableException.class)
    public void testPublishMessageRetryableException()
            throws MqttPersistenceException, MqttException, AWSIotException, AwsIotRetryableException {
        AWSIotMessage message = new AWSIotMessage("test/topic", AWSIotQos.QOS1, "payload");

        when(mqttClient.publish(nullable(String.class), nullable(MqttMessage.class), nullable(Object.class),
                nullable(IMqttActionListener.class)))
                        .thenThrow(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED));

        connection.publishMessage(message);
    }

    @Test
    public void testSubscribeTopic()
            throws MqttPersistenceException, MqttException, AWSIotException, AwsIotRetryableException {
        AWSIotMessage message = new AWSIotMessage("test/topic", AWSIotQos.QOS1);

        doAnswer(new Answer<IMqttToken>() {
            @Override
            public IMqttToken answer(InvocationOnMock invocation) throws Throwable {
                mqttTopic = (String) invocation.getArguments()[0];
                mqttQos = (int) invocation.getArguments()[1];
                return null;
            }
        }).when(mqttClient).subscribe(nullable(String.class), anyInt(), nullable(Object.class), nullable(IMqttActionListener.class));

        connection.subscribeTopic(message);

        verify(mqttClient).subscribe(nullable(String.class), anyInt(), nullable(Object.class), nullable(IMqttActionListener.class));
        assertEquals("test/topic", mqttTopic);
        assertEquals(1, mqttQos);
    }

    @Test(expected = AWSIotException.class)
    public void testSubscribeTopicException()
            throws MqttPersistenceException, MqttException, AWSIotException, AwsIotRetryableException {
        AWSIotMessage message = new AWSIotMessage("test/topic", AWSIotQos.QOS1);

        when(mqttClient.subscribe(nullable(String.class), anyInt(), nullable(Object.class), nullable(IMqttActionListener.class)))
                .thenThrow(new MqttException(0));

        connection.subscribeTopic(message);
    }

    @Test(expected = AwsIotRetryableException.class)
    public void testSubscribeTopicRetryableException()
            throws MqttPersistenceException, MqttException, AWSIotException, AwsIotRetryableException {
        AWSIotMessage message = new AWSIotMessage("test/topic", AWSIotQos.QOS1);

        when(mqttClient.subscribe(nullable(String.class), anyInt(), nullable(Object.class), nullable(IMqttActionListener.class)))
                .thenThrow(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED));

        connection.subscribeTopic(message);
    }

    @Test
    public void testUnsubscribeTopic()
            throws MqttPersistenceException, MqttException, AWSIotException, AwsIotRetryableException {
        AWSIotMessage message = new AWSIotMessage("test/topic", AWSIotQos.QOS1);

        doAnswer(new Answer<IMqttToken>() {
            @Override
            public IMqttToken answer(InvocationOnMock invocation) throws Throwable {
                mqttTopic = (String) invocation.getArguments()[0];
                return null;
            }
        }).when(mqttClient).unsubscribe(nullable(String.class), nullable(Object.class), nullable(IMqttActionListener.class));

        connection.unsubscribeTopic(message);

        verify(mqttClient).unsubscribe(nullable(String.class), nullable(Object.class), nullable(IMqttActionListener.class));
        assertEquals("test/topic", mqttTopic);
    }

    @Test(expected = AWSIotException.class)
    public void testUnsubscribeTopicException()
            throws MqttPersistenceException, MqttException, AWSIotException, AwsIotRetryableException {
        AWSIotMessage message = new AWSIotMessage("test/topic", AWSIotQos.QOS1);

        when(mqttClient.unsubscribe(nullable(String.class), nullable(Object.class), nullable(IMqttActionListener.class)))
                .thenThrow(new MqttException(0));

        connection.unsubscribeTopic(message);
    }

    @Test(expected = AwsIotRetryableException.class)
    public void testUnsubscribeTopicRetryableException()
            throws MqttPersistenceException, MqttException, AWSIotException, AwsIotRetryableException {
        AWSIotMessage message = new AWSIotMessage("test/topic", AWSIotQos.QOS1);

        when(mqttClient.unsubscribe(nullable(String.class), nullable(Object.class), nullable(IMqttActionListener.class)))
                .thenThrow(new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED));

        connection.unsubscribeTopic(message);
    }

}
