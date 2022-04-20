package com.amazonaws.services.iot.client.shadow;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.core.AbstractAwsIotClient;
import com.amazonaws.services.iot.client.shadow.AwsIotDeviceCommandManager.Command;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotDeviceCommandTest {

    @Mock
    private AwsIotDeviceCommandManager commandManager;
    @Mock
    private AbstractAwsIotClient client;
    @Mock
    private AbstractAwsIotDevice device;
    @Mock
    private AWSIotMessage request;

    private AWSIotMessage response;

    @Before
    public void setup() {
        when(device.getClient()).thenReturn(client);

        response = new AWSIotMessage("test/topic", AWSIotQos.QOS0, "response payload");
    }

    @Test
    public void testPutWhenDeviceReady() throws AWSIotException {
        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.GET, "1", request, 0, true);

        when(device.isCommandReady(Command.GET)).thenReturn(true);

        command.put(device);

        verify(client, times(1)).publish(command, 0);
    }

    @Test
    public void testPutWhenDeviceNotReady() throws AWSIotException {
        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.GET, "1", request, 0, true);

        when(device.isCommandReady(Command.GET)).thenReturn(false);

        command.put(device);

        verify(client, times(0)).publish(command, 0);
    }

    @Test
    public void testGetWithPayload() throws AWSIotException, AWSIotTimeoutException {
        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.GET, "1", request, 0, true);
        command.setResponse(response);
        command.onSuccess();

        String payload = command.get(device);

        assertEquals("response payload", payload);
    }

    @Test
    public void testGetWithoutPayload() throws AWSIotException, AWSIotTimeoutException {
        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.GET, "1", request, 0, true);
        command.onSuccess();

        String payload = command.get(device);

        assertEquals(null, payload);
        verify(request, times(0)).onSuccess();
    }

    @Test
    public void testGetFailure() throws AWSIotException, AWSIotTimeoutException {
        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.GET, "1", request, 0, true);
        command.onFailure();

        command.get(device);

        verify(request, times(1)).onFailure();
    }

    @Test
    public void testGetTimeout() throws AWSIotException, AWSIotTimeoutException {
        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.GET, "1", request, 0, true);
        command.onTimeout();

        command.get(device);

        verify(request, times(1)).onTimeout();
    }

    @Test(expected = AWSIotException.class)
    public void testGetFailureSync() throws AWSIotException, AWSIotTimeoutException {
        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.GET, "1", request, 0, false);
        command.onFailure();

        command.get(device);
    }

    @Test(expected = AWSIotTimeoutException.class)
    public void testGetTimeoutSync() throws AWSIotException, AWSIotTimeoutException {
        AwsIotDeviceCommand command = new AwsIotDeviceCommand(commandManager, Command.GET, "1", request, 0, false);
        command.onTimeout();

        try {
            command.get(device);
        } finally{
            verify(commandManager, times(1)).onCommandTimeout(command);
        }
    }

}
