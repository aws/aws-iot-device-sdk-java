package com.amazonaws.services.iot.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class AWSIotQosTest {

    @Test
    public void testValueOf0() {
        assertEquals(AWSIotQos.QOS0, AWSIotQos.valueOf(0));
    }

    @Test
    public void testValueOf1() {
        assertEquals(AWSIotQos.QOS1, AWSIotQos.valueOf(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOf2() {
        AWSIotQos.valueOf(2);
    }

    @Test
    public void testgetValueOf0() {
        assertEquals(0, AWSIotQos.QOS0.getValue());
    }

    @Test
    public void testgetValueOf1() {
        assertEquals(1, AWSIotQos.QOS1.getValue());
    }

}
