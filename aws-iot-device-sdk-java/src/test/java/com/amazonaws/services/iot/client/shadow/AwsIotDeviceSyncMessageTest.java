package com.amazonaws.services.iot.client.shadow;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.iot.client.AWSIotDeviceErrorCode;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotDeviceSyncMessageTest {

    private static final String TEST_TOPIC = "test/topic";
    private static final AWSIotQos TEST_QOS = AWSIotQos.QOS0;

    @Mock
    private AbstractAwsIotDevice device;

    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = new ObjectMapper();
        when(device.getJsonObjectMapper()).thenReturn(objectMapper);

    }

    @Test
    public void testOnSuccessInvalidPayload() {
        AwsIotDeviceSyncMessage message = new AwsIotDeviceSyncMessage(TEST_TOPIC, TEST_QOS, device);
        message.setStringPayload("123");

        message.onSuccess();
        verify(device, never()).getLocalVersion();
    }

    @Test
    public void testOnSuccessVersionAlreadyUpdated() {
        AtomicLong localVersion = new AtomicLong(2);
        when(device.getLocalVersion()).thenReturn(localVersion);

        AwsIotDeviceSyncMessage message = new AwsIotDeviceSyncMessage(TEST_TOPIC, TEST_QOS, device);
        message.setStringPayload("{\"version\":1}");

        message.onSuccess();

        assertEquals(2, localVersion.get());
    }

    @Test
    public void testOnSuccessVersionNotUpdated() {
        AtomicLong localVersion = new AtomicLong(-1);
        when(device.getLocalVersion()).thenReturn(localVersion);

        AwsIotDeviceSyncMessage message = new AwsIotDeviceSyncMessage(TEST_TOPIC, TEST_QOS, device);
        message.setStringPayload("{\"version\":1}");

        message.onSuccess();

        assertEquals(1, localVersion.get());
    }

    @Test
    public void testOnFailureDeviceNotFound() {
        AtomicLong localVersion = new AtomicLong(-1);
        when(device.getLocalVersion()).thenReturn(localVersion);

        AwsIotDeviceSyncMessage message = new AwsIotDeviceSyncMessage(TEST_TOPIC, TEST_QOS, device);
        message.setErrorCode(AWSIotDeviceErrorCode.NOT_FOUND);

        message.onFailure();

        assertEquals(0, localVersion.get());
    }

    @Test
    public void testOnFailureOtherError() {
        AtomicLong localVersion = new AtomicLong(-1);
        lenient().when(device.getLocalVersion()).thenReturn(localVersion);

        AwsIotDeviceSyncMessage message = new AwsIotDeviceSyncMessage(TEST_TOPIC, TEST_QOS, device);
        message.setErrorCode(AWSIotDeviceErrorCode.INTERNAL_SERVICE_FAILURE);

        message.onFailure();

        assertEquals(-1, localVersion.get());
    }

}
