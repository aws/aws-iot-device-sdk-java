package com.amazonaws.services.iot.client.shadow;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotDeviceDeltaListenerTest {

    private static final String TEST_SHADOW = "shadow";
    private static final String TEST_TOPIC = "test/topic";
    private static final AWSIotQos TEST_QOS = AWSIotQos.QOS0;

    @Mock
    private AbstractAwsIotDevice device;

    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = new ObjectMapper();
        device.enableVersioning = false;
        when(device.getThingName()).thenReturn(TEST_SHADOW);
    }

    @Test
    public void testNullPayload() {
        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS);

        AwsIotDeviceDeltaListener listener = new AwsIotDeviceDeltaListener(TEST_TOPIC, TEST_QOS, device);
        listener.onMessage(message);

        verify(device, never()).getJsonObjectMapper();
    }

    @Test
    public void testReadTreeNonObject() throws JsonProcessingException, IOException {
        when(device.getJsonObjectMapper()).thenReturn(objectMapper);

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS, "123");

        AwsIotDeviceDeltaListener listener = new AwsIotDeviceDeltaListener(TEST_TOPIC, TEST_QOS, device);
        listener.onMessage(message);

        verify(device, times(1)).getJsonObjectMapper();
        verify(device, never()).onShadowUpdate(anyString());
    }

    @Test
    public void testReadTreeMissingState() throws JsonProcessingException, IOException {
        when(device.getJsonObjectMapper()).thenReturn(objectMapper);

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS, "{}");

        AwsIotDeviceDeltaListener listener = new AwsIotDeviceDeltaListener(TEST_TOPIC, TEST_QOS, device);
        listener.onMessage(message);

        verify(device, times(1)).getJsonObjectMapper();
        verify(device, never()).onShadowUpdate(anyString());
    }

    @Test
    public void testOnShadowUpdate() throws JsonProcessingException, IOException {
        when(device.getJsonObjectMapper()).thenReturn(objectMapper);

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS, "{\"state\":{}}");

        AwsIotDeviceDeltaListener listener = new AwsIotDeviceDeltaListener(TEST_TOPIC, TEST_QOS, device);
        listener.onMessage(message);

        verify(device, times(1)).getJsonObjectMapper();
        verify(device, times(1)).onShadowUpdate(anyString());
    }

    @Test
    public void testVersioningEnabledMissingVersion() throws JsonProcessingException, IOException {
        when(device.getJsonObjectMapper()).thenReturn(objectMapper);
        device.enableVersioning = true;

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS, "{\"state\":{}}");

        AwsIotDeviceDeltaListener listener = new AwsIotDeviceDeltaListener(TEST_TOPIC, TEST_QOS, device);
        listener.onMessage(message);

        verify(device, times(1)).getJsonObjectMapper();
        verify(device, never()).getLocalVersion();
    }

    @Test
    public void testVersioningEnabledReceivedOldVersion() throws JsonProcessingException, IOException {
        when(device.getJsonObjectMapper()).thenReturn(objectMapper);
        when(device.getLocalVersion()).thenReturn(new AtomicLong(2));
        device.enableVersioning = true;

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS, "{\"version\":1, \"state\":{}}");

        AwsIotDeviceDeltaListener listener = new AwsIotDeviceDeltaListener(TEST_TOPIC, TEST_QOS, device);
        listener.onMessage(message);

        verify(device, times(1)).getJsonObjectMapper();
        verify(device, never()).onShadowUpdate(anyString());
    }

    @Test
    public void testVersioningEnabledReceivedNewerVersion() throws JsonProcessingException, IOException {
        AtomicLong localVersion = new AtomicLong(2);
        when(device.getJsonObjectMapper()).thenReturn(objectMapper);
        when(device.getLocalVersion()).thenReturn(localVersion);
        device.enableVersioning = true;

        AWSIotMessage message = new AWSIotMessage(TEST_TOPIC, TEST_QOS, "{\"version\":3, \"state\":{}}");

        AwsIotDeviceDeltaListener listener = new AwsIotDeviceDeltaListener(TEST_TOPIC, TEST_QOS, device);
        listener.onMessage(message);

        assertEquals(3, localVersion.get());
        verify(device, times(1)).getJsonObjectMapper();
        verify(device, times(1)).onShadowUpdate(anyString());
    }

}
