package com.amazonaws.services.iot.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class AWSIotDeviceErrorCodeTest {

    @Test
    public void testValueOf() {
        assertEquals(AWSIotDeviceErrorCode.BAD_REQUEST, AWSIotDeviceErrorCode.valueOf(400));
    }

    @Test
    public void testgetValue() {
        assertEquals(400, AWSIotDeviceErrorCode.BAD_REQUEST.getValue());
    }

    @Test
    public void testInvalidValue() {
        assertNull(AWSIotDeviceErrorCode.valueOf(10));
    }

}
