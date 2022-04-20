package com.amazonaws.services.iot.client.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.amazonaws.services.iot.client.AWSIotConnectionStatus;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.AWSIotTopic;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAwsIotClientTest {

    private static final String TEST_ENDPOINT = "iot.us-east-1.amazonaws.com";
    private static final String TEST_CLIENTID = "client";
    private static final String TEST_TOPIC = "test/topic";
    private static final String TEST_THING = "thing";
    private static final AWSIotQos TEST_QOS = AWSIotQos.QOS0;
    private static final String AccessKeyId = "123";
    private static final String SecretAccessKey = "456";
    private static final String SessionToken = "abc";

    private boolean requestSuccess;
    private boolean requestFailure;
    private boolean requestTimeout;

    @Mock
    private AwsIotConnection connection;
    @Mock
    private AWSIotDevice device;
    @Mock
    private ScheduledExecutorService executionService;
    
    private AbstractAwsIotClient client;

    @Before
    public void setup() throws AWSIotException {
        client = new AbstractAwsIotClient(TEST_ENDPOINT, TEST_CLIENTID, connection) {
        };

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                AwsIotCompletion completion = (AwsIotCompletion) invocation.getArguments()[0];
                if (requestSuccess) {
                    completion.onSuccess();
                } else if (requestFailure) {
                    completion.onFailure();
                } else if (requestTimeout) {
                    completion.onTimeout();
                }
                return null;
            }
        }).when(connection).publish(any(AwsIotCompletion.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                AwsIotCompletion completion = (AwsIotCompletion) invocation.getArguments()[0];
                if (requestSuccess) {
                    completion.onSuccess();
                } else if (requestFailure) {
                    completion.onFailure();
                } else if (requestTimeout) {
                    completion.onTimeout();
                }
                return null;
            }
        }).when(connection).subscribe(any(AwsIotCompletion.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                AwsIotCompletion completion = (AwsIotCompletion) invocation.getArguments()[0];
                if (requestSuccess) {
                    completion.onSuccess();
                } else if (requestFailure) {
                    completion.onFailure();
                } else if (requestTimeout) {
                    completion.onTimeout();
                }
                return null;
            }
        }).when(connection).unsubscribe(any(AwsIotCompletion.class));
    }

    @Test
    public void testWebsocketClientCreation() {
        client = new AbstractAwsIotClient(TEST_ENDPOINT, TEST_CLIENTID, AccessKeyId, SecretAccessKey, SessionToken) {
        };

        assertEquals(AwsIotConnectionType.MQTT_OVER_WEBSOCKET, client.getConnectionType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWebsocketClientCreationFailure() {
        client = new AbstractAwsIotClient(null, null, null, null, null) {
        };
    }

    @Test
    public void testUpdateCredentials() {
        client.updateCredentials(AccessKeyId, SecretAccessKey, SessionToken);

        verify(connection, times(1)).updateCredentials(anyString(), anyString(), anyString());
    }

    @Test
    public void testPublishSuccess() throws AWSIotException {
        requestSuccess = true;
        requestFailure = requestTimeout = false;

        client.publish(TEST_TOPIC, "test");

        verify(connection, times(1)).publish(any(AwsIotCompletion.class));
    }

    @Test(expected = AWSIotException.class)
    public void testPublishFailure() throws AWSIotException, AWSIotTimeoutException {
        requestFailure = true;
        requestSuccess = requestTimeout = false;

        client.publish(TEST_TOPIC, "test", 10);
    }

    @Test(expected = AWSIotTimeoutException.class)
    public void testPublishTimeout() throws AWSIotException, AWSIotTimeoutException {
        requestTimeout = true;
        requestSuccess = requestFailure = false;

        client.publish(TEST_TOPIC, "test", 10);
    }

    @Test
    public void testSubscribeSuccess() throws AWSIotException {
        requestSuccess = true;
        requestFailure = requestTimeout = false;

        AWSIotTopic topic = new AWSIotTopic(TEST_TOPIC, TEST_QOS);
        client.subscribe(topic, true);

        verify(connection, times(1)).subscribe(any(AwsIotCompletion.class));
        assertEquals(1, client.getSubscriptions().size());
    }

    @Test(expected = AWSIotException.class)
    public void testSubscribeFailure() throws AWSIotException, AWSIotTimeoutException {
        requestFailure = true;
        requestSuccess = requestTimeout = false;

        AWSIotTopic topic = new AWSIotTopic(TEST_TOPIC, TEST_QOS);
        client.subscribe(topic, 10, true);
        assertEquals(0, client.getSubscriptions().size());
    }

    @Test(expected = AWSIotTimeoutException.class)
    public void testSubscribeTimeout() throws AWSIotException, AWSIotTimeoutException {
        requestTimeout = true;
        requestSuccess = requestFailure = false;

        AWSIotTopic topic = new AWSIotTopic(TEST_TOPIC, TEST_QOS);
        client.subscribe(topic, 10, true);
        assertEquals(0, client.getSubscriptions().size());
    }

    @Test
    public void testUnsubscribeSuccess() throws AWSIotException {
        client.getSubscriptions().put(TEST_TOPIC, new AWSIotTopic(TEST_TOPIC, TEST_QOS));
        requestSuccess = true;
        requestFailure = requestTimeout = false;

        client.unsubscribe(TEST_TOPIC);

        verify(connection, times(1)).unsubscribe(any(AwsIotCompletion.class));
        assertEquals(0, client.getSubscriptions().size());
    }

    @Test(expected = AWSIotException.class)
    public void testUnsubscribeFailure() throws AWSIotException, AWSIotTimeoutException {
        client.getSubscriptions().put(TEST_TOPIC, new AWSIotTopic(TEST_TOPIC, TEST_QOS));
        requestFailure = true;
        requestSuccess = requestTimeout = false;

        client.unsubscribe(TEST_TOPIC, 10);
        assertEquals(1, client.getSubscriptions().size());
    }

    @Test(expected = AWSIotTimeoutException.class)
    public void testUnsubscribeTimeout() throws AWSIotException, AWSIotTimeoutException {
        client.getSubscriptions().put(TEST_TOPIC, new AWSIotTopic(TEST_TOPIC, TEST_QOS));
        requestTimeout = true;
        requestSuccess = requestFailure = false;

        client.unsubscribe(TEST_TOPIC, 10);
        assertEquals(1, client.getSubscriptions().size());
    }

    @Test
    public void testOnConnectionSuccess() throws AWSIotException {
        when(connection.getConnectionStatus()).thenReturn(AWSIotConnectionStatus.DISCONNECTED);
        when(device.getThingName()).thenReturn(TEST_THING);

        requestSuccess = true;
        requestFailure = requestTimeout = false;
        AWSIotTopic topic = new AWSIotTopic(TEST_TOPIC, TEST_QOS);
        client.subscribe(topic, true);

        client.attach(device);

        client.onConnectionSuccess();

        verify(device, times(1)).activate();
        assertEquals(1, client.getSubscriptions().size());
    }

    @Test
    public void testOnConnectionFailure() throws AWSIotException {
        when(connection.getConnectionStatus()).thenReturn(AWSIotConnectionStatus.CONNECTED);
        when(device.getThingName()).thenReturn(TEST_THING);

        requestSuccess = true;
        requestFailure = requestTimeout = false;
        AWSIotTopic topic = new AWSIotTopic(TEST_TOPIC, TEST_QOS);
        client.subscribe(topic, true);

        client.attach(device);

        client.onConnectionFailure();

        verify(device, times(1)).deactivate();
        assertEquals(1, client.getSubscriptions().size());
    }

    @Test
    public void testOnConnectionClosed() throws AWSIotException {
        when(connection.getConnectionStatus()).thenReturn(AWSIotConnectionStatus.CONNECTED);
        when(device.getThingName()).thenReturn(TEST_THING);

        client.setExecutionService(executionService);

        requestSuccess = true;
        requestFailure = requestTimeout = false;
        AWSIotTopic topic = new AWSIotTopic(TEST_TOPIC, TEST_QOS);
        client.subscribe(topic, true);

        client.attach(device);

        client.onConnectionClosed();

        verify(device, times(1)).deactivate();
        verify(executionService, times(1)).shutdown();
        assertEquals(0, client.getSubscriptions().size());
    }

    @Test
    public void testTopicFilterMatch() {
        assertTrue(client.topicFilterMatch("/a", "/a"));
        assertTrue(client.topicFilterMatch("/a/b/c", "/a/b/c"));
        assertTrue(client.topicFilterMatch("/a/+/c", "/a/b/c"));
        assertTrue(client.topicFilterMatch("/a/b/+", "/a/b/c"));
        assertTrue(client.topicFilterMatch("/a/+/+", "/a/b/c"));
        assertTrue(client.topicFilterMatch("/a/#", "/a/b/c"));
        assertTrue(client.topicFilterMatch("#", "/a/b/c"));

        assertFalse(client.topicFilterMatch("/a/b", null));
        assertFalse(client.topicFilterMatch("/#/b", "/a/b"));
        assertFalse(client.topicFilterMatch("/a/+", "/b/a"));
        assertFalse(client.topicFilterMatch("/a/b", "/A/b"));
        assertFalse(client.topicFilterMatch("/a/b", "/a/b/c"));
        assertFalse(client.topicFilterMatch("/a/b/c", "/a/b"));
        assertFalse(client.topicFilterMatch("/a/b/#", "/a/b"));
        assertFalse(client.topicFilterMatch("/a/b/+", "/a/b"));
    }

    @Test
    public void testDispatch() throws AWSIotException {
        requestSuccess = true;
        requestFailure = requestTimeout = false;

        client.setExecutionService(executionService);

        AWSIotTopic topic = new AWSIotTopic("/a/b", TEST_QOS);
        client.subscribe(topic, true);

        topic = new AWSIotTopic("/a/#", TEST_QOS);
        client.subscribe(topic, true);

        AWSIotMessage message = new AWSIotMessage("/a/b", TEST_QOS);
        client.dispatch(message);

        verify(executionService, times(2)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

}
