package com.amazonaws.services.iot.client.core;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;
import com.amazonaws.services.iot.client.core.AwsIotCompletion;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotCompletionTest {

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
    }

    @Test
    public void testSyncWithoutTimeoutSuccess() throws AWSIotException, AWSIotTimeoutException {
        AwsIotCompletion completion = new AwsIotCompletion(TEST_TOPIC, TEST_QOS, 0);

        // schedule a delayed task to simulate the callback
        scheduleTimeoutTask(completion, "success", 100);

        completion.get(client);
    }

    @Test(expected = AWSIotException.class)
    public void testSyncWithoutTimeoutFailure() throws AWSIotException, AWSIotTimeoutException {
        AwsIotCompletion completion = new AwsIotCompletion(TEST_TOPIC, TEST_QOS, 0);

        // schedule a delayed task to simulate the callback
        scheduleTimeoutTask(completion, "failure", 100);

        completion.get(client);
    }

    @Test
    public void testSyncWithTimeoutSuccess() throws AWSIotException, AWSIotTimeoutException {
        AwsIotCompletion completion = new AwsIotCompletion(TEST_TOPIC, TEST_QOS, 100);

        // schedule a delayed task to simulate the callback
        scheduleTimeoutTask(completion, "success", 50);

        completion.get(client);

        assertTrue(completion.timeoutTask.isCancelled());
    }

    @Test(expected = AWSIotException.class)
    public void testSyncWithTimeoutFailure() throws AWSIotException, AWSIotTimeoutException {
        AwsIotCompletion completion = new AwsIotCompletion(TEST_TOPIC, TEST_QOS, (String) null, 100);

        // schedule a delayed task to simulate the callback
        scheduleTimeoutTask(completion, "failure", 50);

        try {
            completion.get(client);
        } finally {
            assertTrue(completion.timeoutTask.isCancelled());
        }
    }

    @Test(expected = AWSIotTimeoutException.class)
    public void testSyncWithTimeoutTimeout() throws AWSIotException, AWSIotTimeoutException {
        AwsIotCompletion completion = new AwsIotCompletion(TEST_TOPIC, TEST_QOS, (byte[]) null, 100);

        try {
            completion.get(client);
        } finally {
            assertTrue(completion.timeoutTask.isCancelled());
        }
    }

    @Test
    public void testAsyncWithoutTimeoutSuccess() throws AWSIotException, AWSIotTimeoutException, InterruptedException {
        TestMessage request = new TestMessage(TEST_TOPIC, TEST_QOS);
        AwsIotCompletion completion = new AwsIotCompletion(request, 0, true);

        // schedule a delayed task to simulate the callback
        scheduleTimeoutTask(completion, "success", 100);

        completion.get(client);

        synchronized (request) {
            while (!request.isSuccess && !request.isFailure && !request.isTimeout) {
                request.wait(150);
                break;
            }
        }

        assertTrue(request.isSuccess);
        assertFalse(request.isFailure);
        assertFalse(request.isTimeout);
    }

    @Test
    public void testAsyncWithoutTimeoutFailure() throws AWSIotException, AWSIotTimeoutException, InterruptedException {
        TestMessage request = new TestMessage(TEST_TOPIC, TEST_QOS);
        AwsIotCompletion completion = new AwsIotCompletion(request, 0, true);

        // schedule a delayed task to simulate the callback
        scheduleTimeoutTask(completion, "failure", 100);

        completion.get(client);

        synchronized (request) {
            while (!request.isSuccess && !request.isFailure && !request.isTimeout) {
                request.wait(200);
                break;
            }
        }

        assertTrue(request.isFailure);
        assertFalse(request.isSuccess);
        assertFalse(request.isTimeout);
    }

    @Test
    public void testAsyncWithTimeoutSuccess() throws AWSIotException, AWSIotTimeoutException, InterruptedException {
        TestMessage request = new TestMessage(TEST_TOPIC, TEST_QOS);
        AwsIotCompletion completion = new AwsIotCompletion(request, 100, true);

        // schedule a delayed task to simulate the callback
        scheduleTimeoutTask(completion, "success", 50);

        completion.get(client);

        synchronized (request) {
            while (!request.isSuccess && !request.isFailure && !request.isTimeout) {
                request.wait(200);
                break;
            }
        }

        assertTrue(request.isSuccess);
        assertFalse(request.isFailure);
        assertFalse(request.isTimeout);
        assertTrue(completion.timeoutTask.isCancelled());
    }

    @Test
    public void testAsyncWithTimeoutFailure() throws AWSIotException, AWSIotTimeoutException, InterruptedException {
        TestMessage request = new TestMessage(TEST_TOPIC, TEST_QOS);
        AwsIotCompletion completion = new AwsIotCompletion(request, 100, true);

        // schedule a delayed task to simulate the callback
        scheduleTimeoutTask(completion, "failure", 50);

        completion.get(client);

        synchronized (request) {
            while (!request.isSuccess && !request.isFailure && !request.isTimeout) {
                request.wait(200);
                break;
            }
        }

        assertTrue(request.isFailure);
        assertFalse(request.isSuccess);
        assertFalse(request.isTimeout);
        assertTrue(completion.timeoutTask.isCancelled());
    }

    @Test
    public void testAsyncWithTimeoutTimeout() throws AWSIotException, AWSIotTimeoutException, InterruptedException {
        TestMessage request = new TestMessage(TEST_TOPIC, TEST_QOS);
        AwsIotCompletion completion = new AwsIotCompletion(request, 100, true);

        completion.get(client);

        synchronized (request) {
            while (!request.isSuccess && !request.isFailure && !request.isTimeout) {
                request.wait(200);
                break;
            }
        }

        assertTrue(request.isTimeout);
        assertFalse(request.isSuccess);
        assertFalse(request.isFailure);
        assertTrue(completion.timeoutTask.isCancelled());
    }

    class TestMessage extends AWSIotMessage {
        public boolean isSuccess;
        public boolean isFailure;
        public boolean isTimeout;

        public TestMessage(String topic, AWSIotQos qos) {
            super(topic, qos);
        }

        @Override
        public void onSuccess() {
            synchronized (this) {
                isSuccess = true;
                notify();
            }
        }

        @Override
        public void onFailure() {
            synchronized (this) {
                isFailure = true;
                notify();
            }
        }

        @Override
        public void onTimeout() {
            synchronized (this) {
                isTimeout = true;
                notify();
            }
        }
    }

    private Future<?> scheduleTimeoutTask(Runnable runnable, long timeout) {
        return executionService.schedule(runnable, timeout, TimeUnit.MILLISECONDS);
    }

    private Future<?> scheduleTimeoutTask(final AwsIotCompletion completion, final String whichCallback, long timeout) {
        return executionService.schedule(new Runnable() {
            @Override
            public void run() {
                if (whichCallback.equals("success")) {
                    completion.onSuccess();
                } else if (whichCallback.equals("failure")) {
                    completion.onFailure();
                } else if (whichCallback.equals("timeout")) {
                    completion.onTimeout();
                }
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

}
