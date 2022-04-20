package com.amazonaws.services.iot.client.core;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.amazonaws.services.iot.client.AWSIotConnectionStatus;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotConnectionTest {

    private static final String TEST_TOPIC = "test/topic";
    private static final AWSIotQos TEST_QOS = AWSIotQos.QOS0;

    private final ScheduledExecutorService executionService = Executors.newScheduledThreadPool(2);

    @Mock
    private AbstractAwsIotClient client;

    @Before
    public void setup() {
        doAnswer(new Answer<Future<?>>() {
            @Override
            public Future<?> answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                long timeout = (long) invocation.getArguments()[1];
                return scheduleTimeoutTask(runnable, timeout);
            }
        }).when(client).scheduleTimeoutTask(any(Runnable.class), anyLong());

        when(client.getMaxOfflineQueueSize()).thenReturn(64);
        when(client.getMaxConnectionRetries()).thenReturn(3);
        when(client.getBaseRetryDelay()).thenReturn(1);
        when(client.getMaxRetryDelay()).thenReturn(10);
    }

    @Test
    public void testConnectedPublish() throws AWSIotException {
        TestConnection connection = new TestConnection(client);
        connection.setConnectionStatus(AWSIotConnectionStatus.CONNECTED);

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS);
        connection.publish(message);

        assertEquals(1, connection.publishCount);
        assertEquals(0, connection.getPublishQueue().size());
    }

    @Test
    public void testDisconnectedPublish() throws AWSIotException {
        TestConnection connection = new TestConnection(client);
        connection.setConnectionStatus(AWSIotConnectionStatus.DISCONNECTED);

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS);
        connection.publish(message);

        assertEquals(0, connection.publishCount);
        assertEquals(1, connection.getPublishQueue().size());
    }

    @Test(expected = AWSIotException.class)
    public void testOfflinePublishQueueFull() throws AWSIotException {
        when(client.getMaxOfflineQueueSize()).thenReturn(2);

        TestConnection connection = new TestConnection(client);
        connection.setConnectionStatus(AWSIotConnectionStatus.DISCONNECTED);

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS);
        try {
            connection.publish(message);
            connection.publish(message);
            connection.publish(message);
        } finally {
            assertEquals(0, connection.publishCount);
            assertEquals(2, connection.getPublishQueue().size());
        }
    }

    @Test
    public void testConnectedSubscribe() throws AWSIotException {
        TestConnection connection = new TestConnection(client);
        connection.setConnectionStatus(AWSIotConnectionStatus.CONNECTED);

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS);
        connection.subscribe(message);

        assertEquals(1, connection.subscribeCount);
        assertEquals(0, connection.getSubscribeQueue().size());
    }

    @Test
    public void testDisconnectedSubscribe() throws AWSIotException {
        TestConnection connection = new TestConnection(client);
        connection.setConnectionStatus(AWSIotConnectionStatus.DISCONNECTED);

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS);
        connection.subscribe(message);

        assertEquals(0, connection.subscribeCount);
        assertEquals(1, connection.getSubscribeQueue().size());
    }

    @Test
    public void testConnectedUnsubscribe() throws AWSIotException {
        TestConnection connection = new TestConnection(client);
        connection.setConnectionStatus(AWSIotConnectionStatus.CONNECTED);

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS);
        connection.unsubscribe(message);

        assertEquals(1, connection.unsubscribeCount);
        assertEquals(0, connection.getUnsubscribeQueue().size());
    }

    @Test
    public void testDisconnectedUnsubscribe() throws AWSIotException {
        TestConnection connection = new TestConnection(client);
        connection.setConnectionStatus(AWSIotConnectionStatus.DISCONNECTED);

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS);
        connection.unsubscribe(message);

        assertEquals(0, connection.unsubscribeCount);
        assertEquals(1, connection.getUnsubscribeQueue().size());
    }

    @Test
    public void testConnect() throws AWSIotException {
        TestConnection connection = new TestConnection(client);
        connection.shouldConnect = true;

        connection.connect(null);

        waitBeforeShutdown(200);

        assertEquals(AWSIotConnectionStatus.CONNECTED, connection.connectionStatus);
    }

    @Test
    public void testDisconnect() throws AWSIotException {
        TestConnection connection = new TestConnection(client);
        connection.connectionStatus = AWSIotConnectionStatus.CONNECTED;

        connection.disconnect(null);

        waitBeforeShutdown(200);

        assertEquals(AWSIotConnectionStatus.DISCONNECTED, connection.connectionStatus);
    }

    @Test
    public void testConnectRetries() throws AWSIotException {
        when(client.getMaxConnectionRetries()).thenReturn(3);
        when(client.getBaseRetryDelay()).thenReturn(1);
        when(client.getMaxRetryDelay()).thenReturn(10);

        TestConnection connection = new TestConnection(client);
        connection.shouldConnect = false;

        connection.connect(null);

        waitBeforeShutdown(200);

        assertEquals(AWSIotConnectionStatus.DISCONNECTED, connection.connectionStatus);
        assertEquals(3, connection.getRetryTimes());
    }

    @Test
    public void testRetryDelay() throws AWSIotException, NoSuchFieldException, IllegalAccessException {
        when(client.getBaseRetryDelay()).thenReturn(3000);
        when(client.getMaxRetryDelay()).thenReturn(30000);

        TestConnection connection = new TestConnection(client);
        Field retryTimesSetter = AwsIotConnection.class.getDeclaredField("retryTimes");
        retryTimesSetter.setAccessible(true);
        //FieldSetter retryTimesSetter = new FieldSetter(connection, AwsIotConnection.class.getDeclaredField("retryTimes"));

        retryTimesSetter.set(connection,0);
        assertEquals(client.getBaseRetryDelay(), connection.getRetryDelay());

        // Try a huge range of values. Take exponential steps so the test doesn't take too long.
        for (int retryTimes = 1; retryTimes < Integer.MAX_VALUE / 2; retryTimes *= 2) {
            retryTimesSetter.set(connection, retryTimes);
            assertTrue(client.getBaseRetryDelay() <= connection.getRetryDelay());
            assertTrue(client.getMaxRetryDelay() >= connection.getRetryDelay());
        }

        // Try largest possible value
        retryTimesSetter.set(connection, Integer.MAX_VALUE);
        assertEquals(client.getMaxRetryDelay(), connection.getRetryDelay());
    }

    @Test
    public void testOnConnectionSuccess() throws AWSIotException {
        TestConnection connection = new TestConnection(client);

        connection.publish(new AWSIotMessage(TEST_TOPIC, TEST_QOS));
        connection.subscribe(new AWSIotMessage(TEST_TOPIC, TEST_QOS));
        connection.unsubscribe(new AWSIotMessage(TEST_TOPIC, TEST_QOS));

        connection.onConnectionSuccess();

        assertEquals(AWSIotConnectionStatus.CONNECTED, connection.connectionStatus);
        assertEquals(1, connection.publishCount);
        assertEquals(1, connection.subscribeCount);
        assertEquals(1, connection.unsubscribeCount);
        verify(client, times(1)).onConnectionSuccess();
    }

    @Test
    public void testOnConnectionFailure() {
        when(client.getMaxConnectionRetries()).thenReturn(0);

        TestConnection connection = new TestConnection(client);

        connection.connectionStatus = AWSIotConnectionStatus.CONNECTED;

        connection.onConnectionFailure();

        assertEquals(AWSIotConnectionStatus.DISCONNECTED, connection.connectionStatus);
        verify(client, times(1)).onConnectionClosed();
    }

    @Test
    public void testOnConnectionClosed() {
        lenient().when(client.getMaxConnectionRetries()).thenReturn(0);

        TestConnection connection = new TestConnection(client);

        connection.connectionStatus = AWSIotConnectionStatus.CONNECTED;

        connection.onConnectionClosed();

        assertEquals(AWSIotConnectionStatus.DISCONNECTED, connection.connectionStatus);
        verify(client, times(1)).onConnectionClosed();
    }

    private void waitBeforeShutdown(long timeout) {
        try {
            Thread.sleep(timeout);
            executionService.shutdown();
            boolean terminated = executionService.awaitTermination(1, TimeUnit.SECONDS);
            assertTrue("Pending task not completed after 1s", terminated);
        } catch (InterruptedException e) {
            throw new RuntimeException("test thread interrupted");
        }
    }

    class TestConnection extends AwsIotConnection {
        public int publishCount;
        public int subscribeCount;
        public int unsubscribeCount;
        public boolean shouldConnect;
        public int connectDelay = 10;
        public int disconnectDelay = 10;

        public TestConnection(AbstractAwsIotClient client) {
            super(client);
        }

        @Override
        protected void openConnection(AwsIotMessageCallback callback) throws AWSIotException {
            scheduleTimeoutTask(new Runnable() {
                @Override
                public void run() {
                    if (shouldConnect) {
                        onConnectionSuccess();
                    } else {
                        onConnectionFailure();
                    }
                }
            }, connectDelay);
        }

        @Override
        protected void closeConnection(AwsIotMessageCallback callback) throws AWSIotException {
            scheduleTimeoutTask(new Runnable() {
                @Override
                public void run() {
                    onConnectionFailure();
                }
            }, disconnectDelay);
        }

        @Override
        protected void publishMessage(AWSIotMessage message) throws AWSIotException, AwsIotRetryableException {
            if (AWSIotConnectionStatus.CONNECTED.equals(connectionStatus)) {
                publishCount++;
            } else {
                throw new AwsIotRetryableException("connection is down");
            }
        }

        @Override
        protected void subscribeTopic(AWSIotMessage message) throws AWSIotException, AwsIotRetryableException {
            if (AWSIotConnectionStatus.CONNECTED.equals(connectionStatus)) {
                subscribeCount++;
            } else {
                throw new AwsIotRetryableException("connection is down");
            }
        }

        @Override
        protected void unsubscribeTopic(AWSIotMessage message) throws AWSIotException, AwsIotRetryableException {
            if (AWSIotConnectionStatus.CONNECTED.equals(connectionStatus)) {
                unsubscribeCount++;
            } else {
                throw new AwsIotRetryableException("connection is down");
            }
        }
    }

    private Future<?> scheduleTimeoutTask(Runnable runnable, long timeout) {
        return executionService.schedule(runnable, timeout, TimeUnit.MILLISECONDS);
    }

}
