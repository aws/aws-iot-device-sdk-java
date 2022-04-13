package com.amazonaws.services.iot.client.shadow;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.iot.client.AWSIotDeviceErrorCode;
import com.amazonaws.services.iot.client.AWSIotQos;

@RunWith(MockitoJUnitRunner.class)
public class AwsIotDeviceReportMessageTest {

    private static final String TEST_TOPIC = "test/topic";
    private static final AWSIotQos TEST_QOS = AWSIotQos.QOS0;

    @Mock
    private AbstractAwsIotDevice device;

    @Test
    public void testOnSuccessLocalVersionUnchanged() {
        AtomicLong localVersion = new AtomicLong(2);
        when(device.getLocalVersion()).thenReturn(localVersion);

        long reportVersion = 2;
        AwsIotDeviceReportMessage message = new AwsIotDeviceReportMessage(TEST_TOPIC, TEST_QOS, reportVersion, "",
                device);
        message.onSuccess();

        assertEquals(3, localVersion.get());
    }

    @Test
    public void testOnSuccessLocalVersionChanged() {
        AtomicLong localVersion = new AtomicLong(4);
        when(device.getLocalVersion()).thenReturn(localVersion);

        long reportVersion = 2;
        AwsIotDeviceReportMessage message = new AwsIotDeviceReportMessage(TEST_TOPIC, TEST_QOS, reportVersion, "",
                device);
        message.onSuccess();

        assertEquals(4, localVersion.get());
    }

    @Test
    public void testOnFailureVersionConflict() {
        AwsIotDeviceReportMessage message = new AwsIotDeviceReportMessage(TEST_TOPIC, TEST_QOS, 1, "", device);
        message.setErrorCode(AWSIotDeviceErrorCode.CONFLICT);

        message.onFailure();

        verify(device, times(1)).startVersionSync();
    }

    @Test
    public void testOnFailureNonVersionConflict() {
        AwsIotDeviceReportMessage message = new AwsIotDeviceReportMessage(TEST_TOPIC, TEST_QOS, 1, "", device);
        message.setErrorCode(AWSIotDeviceErrorCode.NOT_FOUND);

        message.onFailure();

        verify(device, never()).startVersionSync();
    }

}
