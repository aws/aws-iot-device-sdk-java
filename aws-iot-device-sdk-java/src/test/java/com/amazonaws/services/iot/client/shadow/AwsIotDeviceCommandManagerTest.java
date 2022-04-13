package com.amazonaws.services.iot.client.shadow;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;

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
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;
import com.amazonaws.services.iot.client.shadow.AwsIotDeviceCommandManager.Command;

import lombok.Getter;
import lombok.Setter;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotDeviceCommandManagerTest {

    private static final String SHADOW_NAME = "shadow";

    private boolean requestSuccess;
    private boolean requestFailure;
    private boolean requestTimeout;
    private String requestResponse;

    @Mock
    private AbstractAwsIotClient client;

    @Test
    public void testSendCommandSync() throws AWSIotException {
        TestDevice device = newTestDevice();

        requestSuccess = true;
        requestFailure = requestTimeout = false;
        requestResponse = "test result is success";

        AwsIotDeviceCommandManager commandManager = new AwsIotDeviceCommandManager(device);
        TestRequest request = new TestRequest(null, AWSIotQos.QOS0);
        String result = commandManager.runCommandSync(Command.GET, request);

        assertEquals(requestResponse, result);
        assertEquals(1, commandManager.getPendingCommands().size());
        assertTrue(commandManager.getPendingCommands().entrySet().iterator().next().getValue().getStringPayload()
                .contains("clientToken"));
    }

    @Test(expected = AWSIotException.class)
    public void testSendCommandSyncFailure() throws AWSIotException {
        TestDevice device = newTestDevice();

        requestFailure = true;
        requestSuccess = requestTimeout = false;

        AwsIotDeviceCommandManager commandManager = new AwsIotDeviceCommandManager(device);
        TestRequest request = new TestRequest(null, AWSIotQos.QOS0);

        try {
            commandManager.runCommandSync(Command.GET, request);
        } finally {
            assertEquals(1, commandManager.getPendingCommands().size());
            assertTrue(commandManager.getPendingCommands().entrySet().iterator().next().getValue().getStringPayload()
                    .contains("clientToken"));
        }
    }

    @Test(expected = AWSIotTimeoutException.class)
    public void testSendCommandSyncTimeout() throws AWSIotException, AWSIotTimeoutException {
        TestDevice device = newTestDevice();

        requestTimeout = true;
        requestSuccess = requestFailure = false;

        AwsIotDeviceCommandManager commandManager = new AwsIotDeviceCommandManager(device);
        TestRequest request = new TestRequest(null, AWSIotQos.QOS0);

        try {
            commandManager.runCommandSync(Command.GET, request, 10);
        } finally {
            assertEquals(0, commandManager.getPendingCommands().size());
        }
    }

    @Test
    public void testSendCommand() throws AWSIotException {
        TestDevice device = newTestDevice();

        requestSuccess = true;
        requestFailure = requestTimeout = false;
        requestResponse = "test result is success";

        AwsIotDeviceCommandManager commandManager = new AwsIotDeviceCommandManager(device);
        TestRequest request = new TestRequest(null, AWSIotQos.QOS0);
        commandManager.runCommand(Command.UPDATE, request, 0);

        assertTrue(request.isSuccess);
        assertEquals(requestResponse, request.getStringPayload());
        assertEquals(1, commandManager.getPendingCommands().size());
        assertTrue(commandManager.getPendingCommands().entrySet().iterator().next().getValue().getStringPayload()
                .contains("clientToken"));
    }

    @Test
    public void testSendCommandFailure() throws AWSIotException {
        TestDevice device = newTestDevice();

        requestFailure = true;
        requestSuccess = requestTimeout = false;

        AwsIotDeviceCommandManager commandManager = new AwsIotDeviceCommandManager(device);
        TestRequest request = new TestRequest(null, AWSIotQos.QOS0);

        commandManager.runCommand(Command.UPDATE, request, 0);

        assertTrue(request.isFailure);
        assertEquals(1, commandManager.getPendingCommands().size());
        assertTrue(commandManager.getPendingCommands().entrySet().iterator().next().getValue().getStringPayload()
                .contains("clientToken"));
    }

    @Test
    public void testSendCommandTimeout() throws AWSIotException, AWSIotTimeoutException {
        TestDevice device = newTestDevice();

        requestTimeout = true;
        requestSuccess = requestFailure = false;

        AwsIotDeviceCommandManager commandManager = new AwsIotDeviceCommandManager(device);
        TestRequest request = new TestRequest(null, AWSIotQos.QOS0);

        commandManager.runCommand(Command.UPDATE, request, 10);

        assertTrue(request.isTimeout);
        assertEquals(0, commandManager.getPendingCommands().size());
    }

    @Test
    public void testCommandAccepted() {
        TestDevice device = newTestDevice();

        AwsIotDeviceCommandManager commandManager = new AwsIotDeviceCommandManager(device);
        String commandId = "test-command-id";
        TestRequest request = new TestRequest(null, AWSIotQos.QOS0);

        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.DELETE, commandId, request, 0,
                false);
        commandManager.getPendingCommands().put(commandId, command);

        String responsePayload = "{\"clientToken\":\"" + commandId + "\"}";
        AWSIotMessage response = new AWSIotMessage("$aws/things/shadow/shadow/delete/accepted", AWSIotQos.QOS0,
                responsePayload);
        commandManager.onCommandAck(response);

        assertEquals(responsePayload, request.getStringPayload());
        assertEquals(0, commandManager.getPendingCommands().size());
    }

    @Test
    public void testCommandRejected() {
        TestDevice device = newTestDevice();

        AwsIotDeviceCommandManager commandManager = new AwsIotDeviceCommandManager(device);
        String commandId = "test-command-id";
        TestRequest request = new TestRequest(null, AWSIotQos.QOS0);

        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.DELETE, commandId, request, 0,
                false);
        commandManager.getPendingCommands().put(commandId, command);

        String responsePayload = "{\"clientToken\":\"" + commandId + "\"}";
        AWSIotMessage response = new AWSIotMessage("$aws/things/shadow/shadow/delete/rejected", AWSIotQos.QOS0,
                responsePayload);
        commandManager.onCommandAck(response);

        assertEquals(null, request.getStringPayload());
        assertEquals(0, commandManager.getPendingCommands().size());
    }

    @Test
    public void testSubscriptionAckHalfComplete() {
        TestDevice device = newTestDevice();
        for (String topic : device.getDeviceSubscriptions().keySet()) {
            device.getDeviceSubscriptions().put(topic, false);
        }

        requestSuccess = true;
        requestFailure = requestTimeout = false;

        AwsIotDeviceCommandManager commandManager = new AwsIotDeviceCommandManager(device);
        String commandId = "test-get-id";
        TestRequest request = new TestRequest(null, AWSIotQos.QOS0);

        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.GET, commandId, request, 0, true);
        commandManager.getPendingCommands().put(commandId, command);

        String commandId1 = "test-update-id";
        TestRequest request1 = new TestRequest(null, AWSIotQos.QOS0);
        AwsIotDeviceCommand command1 = new AwsIotDeviceCommand(commandManager, Command.UPDATE, commandId1, request1, 0,
                true);
        commandManager.getPendingCommands().put(commandId1, command1);

        String topic = "$aws/things/shadow/shadow/get/accepted";
        device.getDeviceSubscriptions().put(topic, true);
        commandManager.onSubscriptionAck(topic, true);

        assertEquals(false, request.isSuccess);
        assertEquals(false, request1.isSuccess);
    }

    @Test
    public void testSubscriptionAckCompleteWithSuccess() {
        TestDevice device = newTestDevice();
        for (String topic : device.getDeviceSubscriptions().keySet()) {
            device.getDeviceSubscriptions().put(topic, false);
        }
        device.getDeviceSubscriptions().put("$aws/things/shadow/shadow/get/accepted", true);

        requestSuccess = true;
        requestFailure = requestTimeout = false;
        requestResponse = "test result is success";

        AwsIotDeviceCommandManager commandManager = new AwsIotDeviceCommandManager(device);
        String commandId = "test-get-id";
        TestRequest request = new TestRequest(null, AWSIotQos.QOS0);

        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.GET, commandId, request, 0, true);
        commandManager.getPendingCommands().put(commandId, command);

        String commandId1 = "test-update-id";
        TestRequest request1 = new TestRequest(null, AWSIotQos.QOS0);
        AwsIotDeviceCommand command1 = new AwsIotDeviceCommand(commandManager, Command.UPDATE, commandId1, request1, 0,
                true);
        commandManager.getPendingCommands().put(commandId1, command1);

        String topic = "$aws/things/shadow/shadow/get/rejected";
        device.getDeviceSubscriptions().put(topic, true);
        commandManager.onSubscriptionAck(topic, true);

        assertEquals(true, request.isSuccess);
        assertEquals(false, request1.isSuccess);
    }

    @Test
    public void testSubscriptionAckCompleteWithFailure() {
        TestDevice device = newTestDevice();
        for (String topic : device.getDeviceSubscriptions().keySet()) {
            device.getDeviceSubscriptions().put(topic, false);
        }
        device.getDeviceSubscriptions().put("$aws/things/shadow/shadow/get/accepted", true);

        requestFailure = true;
        requestSuccess = requestTimeout = false;

        AwsIotDeviceCommandManager commandManager = new AwsIotDeviceCommandManager(device);
        String commandId = "test-get-id";
        TestRequest request = new TestRequest(null, AWSIotQos.QOS0);

        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.GET, commandId, request, 0, true);
        commandManager.getPendingCommands().put(commandId, command);

        String commandId1 = "test-update-id";
        TestRequest request1 = new TestRequest(null, AWSIotQos.QOS0);
        AwsIotDeviceCommand command1 = new AwsIotDeviceCommand(commandManager, Command.UPDATE, commandId1, request1, 0,
                true);
        commandManager.getPendingCommands().put(commandId1, command1);

        String topic = "$aws/things/shadow/shadow/get/rejected";
        commandManager.onSubscriptionAck(topic, false);

        assertEquals(1, commandManager.getPendingCommands().size());
        assertEquals(true, request.isFailure);
        assertEquals(false, request1.isSuccess);
    }

    @Before
    public void setup() throws AWSIotException {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                AwsIotDeviceCommand message = (AwsIotDeviceCommand) invocation.getArguments()[0];
                // simulate call request
                if (requestSuccess) {
                    message.setResponse(new AWSIotMessage(null, null, requestResponse));
                    message.onSuccess();
                }
                if (requestFailure) {
                    message.onFailure();
                }
                if (requestTimeout) {
                    message.onTimeout();
                }
                return null;
            }
        }).when(client).publish(any(AWSIotMessage.class), anyLong());
    }

    private TestDevice newTestDevice() {
        TestDevice device = new TestDevice(SHADOW_NAME);

        device.setClient(client);
        device.setSomeValue(1);

        for (String topic : device.getDeviceSubscriptions().keySet()) {
            device.getDeviceSubscriptions().put(topic, true);
        }

        return device;
    }

    class TestRequest extends AWSIotMessage {
        boolean isSuccess;
        boolean isFailure;
        boolean isTimeout;

        public TestRequest(String topic, AWSIotQos qos) {
            super(topic, qos);
        }

        @Override
        public void onSuccess() {
            isSuccess = true;
        }

        @Override
        public void onFailure() {
            isFailure = true;
        }

        @Override
        public void onTimeout() {
            isTimeout = true;
        }
    }

    @Getter
    @Setter
    class TestDevice extends AbstractAwsIotDevice {
        @AWSIotDeviceProperty
        long someValue;

        protected TestDevice(String thingName) {
            super(thingName);
        }
    }
}
