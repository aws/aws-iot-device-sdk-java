package com.amazonaws.services.iot.client.shadow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.amazonaws.services.iot.client.AWSIotDeviceProperty;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;
import com.amazonaws.services.iot.client.shadow.AwsIotDeviceCommandManager.Command;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAwsIotDeviceTest {
    private static final String SHADOW_NAME = "shadow";

    private final ScheduledExecutorService executionService = Executors.newScheduledThreadPool(2);

    @Mock
    private AbstractAwsIotClient client;

    private List<AWSIotMessage> publishedMessages;
    private List<AWSIotTopic> subscribedTopics;
    private List<AWSIotTopic> unsubscribedTopics;

    @Before
    public void setup() throws AWSIotException {
        publishedMessages = new ArrayList<>();
        subscribedTopics = new ArrayList<>();
        unsubscribedTopics = new ArrayList<>();

        doAnswer(new Answer<Future<?>>() {
            @Override
            public Future<?> answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                long timeout = (long) invocation.getArguments()[2];
                // run only once for unit test purpose
                return executionService.schedule(runnable, timeout, TimeUnit.MILLISECONDS);
            }
        }).when(client).scheduleRoutineTask(any(Runnable.class), anyLong(), anyLong());

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                AWSIotMessage message = (AWSIotMessage) invocation.getArguments()[0];
                // simulate a success call
                message.onSuccess();
                publishedMessages.add(message);
                return null;
            }
        }).when(client).publish(any(AWSIotMessage.class), anyLong());

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                AWSIotTopic topic = (AWSIotTopic) invocation.getArguments()[0];
                // simulate a success call
                topic.onSuccess();
                subscribedTopics.add(topic);
                return null;
            }
        }).when(client).subscribe(any(AWSIotTopic.class), anyLong());

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                AWSIotTopic topic = (AWSIotTopic) invocation.getArguments()[0];
                // simulate a success call
                topic.onSuccess();
                unsubscribedTopics.add(topic);
                return null;
            }
        }).when(client).unsubscribe(any(AWSIotTopic.class), anyLong());
    }

    @Test
    public void testSerializationDeserialization() {
        TestDevice device = newTestDevice(SHADOW_NAME);

        String jsonState = device.onDeviceReport();

        TestDevice anotherDevice = new TestDevice(SHADOW_NAME);

        anotherDevice.onShadowUpdate(jsonState);

        assertNull(anotherDevice.getNonReportedString());
        assertNull(anotherDevice.getNonUpdatedString());

        anotherDevice.setNonReportedString(device.getNonReportedString());
        anotherDevice.setNonUpdatedString(device.getNonUpdatedString());
        assertEquals(device, anotherDevice);
    }

    @Test
    public void testDeviceSyncWithoutVersioning() {
        TestDevice device = newTestDevice(SHADOW_NAME);
        device.setClient(client);
        device.setEnableVersioning(false);
        device.setReportInterval(50);
        for (String topic : device.getDeviceSubscriptions().keySet()) {
            device.getDeviceSubscriptions().put(topic, true);
        }

        device.startSync();

        waitBeforeShutdown(100);

        assertEquals(1, publishedMessages.size());
        AwsIotDeviceCommand command = (AwsIotDeviceCommand) publishedMessages.get(0);
        assertEquals(Command.UPDATE, command.getCommand());
        assertTrue(command.getStringPayload().contains("\"state\":{\"reported\":"));
    }

    @Test
    public void testDeviceSyncWithVersioning() {
        TestDevice device = newTestDevice(SHADOW_NAME);
        device.setClient(client);
        device.setEnableVersioning(true);
        device.setReportInterval(50);
        for (String topic : device.getDeviceSubscriptions().keySet()) {
            device.getDeviceSubscriptions().put(topic, true);
        }

        device.startSync();

        waitBeforeShutdown(100);

        assertEquals(1, publishedMessages.size());
        AwsIotDeviceCommand command = (AwsIotDeviceCommand) publishedMessages.get(0);
        assertEquals(Command.GET, command.getCommand());
    }

    @Test
    public void testActivate() throws AWSIotException {
        TestDevice device = newTestDevice(SHADOW_NAME);
        device.setClient(client);
        device.setEnableVersioning(false);
        device.setReportInterval(50);

        device.activate();

        waitBeforeShutdown(100);

        assertEquals(1, publishedMessages.size());
        AwsIotDeviceCommand command = (AwsIotDeviceCommand) publishedMessages.get(0);
        assertEquals(Command.UPDATE, command.getCommand());

        assertEquals(7, subscribedTopics.size());
    }

    @Test
    public void testDeactivate() throws AWSIotException {
        TestDevice device = newTestDevice(SHADOW_NAME);
        device.setClient(client);
        device.setEnableVersioning(false);
        device.setReportInterval(50);

        device.deactivate();

        waitBeforeShutdown(100);

        assertEquals(7, unsubscribedTopics.size());
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

    private TestDevice newTestDevice(String thingName) {
        TestDevice device = new TestDevice(thingName);

        device.boolValue = true;
        device.byteValue = 12;
        device.charValue = 'a';
        device.intValue = -100;
        device.longValue = 45678999812344235l;
        device.floatValue = 0.000000000000001f;
        device.doubleValue = 9.999999999999999d;

        device.testEnum = TestEnum.BLUE;
        device.boolObject = Boolean.TRUE;
        device.integerObject = Integer.valueOf(1234445);
        device.longObject = Long.valueOf(1234234513243432l);
        device.floatObject = Float.valueOf(12.12f);
        device.doubleObject = Double.valueOf(-12.12f);
        device.stringObject = " \" test string ";

        device.stringArray = new String[] { "foo", "bar", "baz" };

        device.integerList = new ArrayList<>();
        device.integerList.add(1000);
        device.integerList.add(-1111);
        device.integerList.add(0);

        device.stringMap = new HashMap<>();
        device.stringMap.put("key1", "foo");
        device.stringMap.put("key2", "bar");
        device.stringMap.put("key3", "baz");

        device.listMaps = new ArrayList<>();
        device.listMaps.add(device.stringMap);
        device.listMaps.add(device.stringMap);
        device.listMaps.add(device.stringMap);

        device.mapLists = new HashMap<>();
        device.mapLists.put("key1", device.integerList);
        device.mapLists.put("key2", device.integerList);
        device.mapLists.put("key3", device.integerList);

        device.nonReportedString = "Should not report";
        device.nonUpdatedString = "Should not be updated";

        return device;
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = false)
    class TestDevice extends AbstractAwsIotDevice {
        @AWSIotDeviceProperty
        boolean boolValue;
        @AWSIotDeviceProperty
        byte byteValue;
        @AWSIotDeviceProperty
        char charValue;
        @AWSIotDeviceProperty
        int intValue;
        @AWSIotDeviceProperty
        long longValue;
        @AWSIotDeviceProperty
        float floatValue;
        @AWSIotDeviceProperty
        double doubleValue;

        @AWSIotDeviceProperty
        TestEnum testEnum;
        @AWSIotDeviceProperty
        Boolean boolObject;
        @AWSIotDeviceProperty
        Integer integerObject;
        @AWSIotDeviceProperty
        Long longObject;
        @AWSIotDeviceProperty
        Float floatObject;
        @AWSIotDeviceProperty
        Double doubleObject;
        @AWSIotDeviceProperty
        String stringObject;

        @AWSIotDeviceProperty
        String[] stringArray;
        @AWSIotDeviceProperty
        List<Integer> integerList;
        @AWSIotDeviceProperty
        Map<String, String> stringMap;

        @AWSIotDeviceProperty
        List<Map<String, String>> listMaps;
        @AWSIotDeviceProperty
        Map<String, List<Integer>> mapLists;

        @AWSIotDeviceProperty(enableReport = false)
        String nonReportedString;
        @AWSIotDeviceProperty(allowUpdate = false)
        String nonUpdatedString;

        protected TestDevice(String thingName) {
            super(thingName);
        }
    }

    public static enum TestEnum {
        RED, BLACK, BLUE
    }

}
