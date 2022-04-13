package com.amazonaws.services.iot.client;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class AWSIotMessageTest {

    @Test
    public void testRawPayload() {
        byte[] testBytes = "test string".getBytes();

        AWSIotMessage message = new AWSIotMessage("topic", AWSIotQos.QOS0);

        message.setPayload(testBytes);

        assertTrue(Arrays.equals(testBytes, message.getPayload()));
    }

    @Test
    public void testBytesToStringPayload() {
        byte[] testBytes = "test string".getBytes();

        AWSIotMessage message = new AWSIotMessage("topic", AWSIotQos.QOS0);

        message.setPayload(testBytes);

        assertEquals("test string", message.getStringPayload());
    }

    @Test
    public void testStringSerialization() {
        String testString = "test string";

        AWSIotMessage message = new AWSIotMessage("topic", AWSIotQos.QOS0);

        message.setStringPayload(testString);

        assertEquals(testString, message.getStringPayload());
    }

    @Test
    public void testMultiByteStringSerialization() {
        byte[] chineseLetterInUtf8 = { (byte) 0xE7, (byte) 0x9A, (byte) 0x84 };

        AWSIotMessage message = new AWSIotMessage("topic", AWSIotQos.QOS0, chineseLetterInUtf8);

        assertEquals("的", message.getStringPayload());
    }

    @Test
    public void testMultiByteStringDeserialization() {
        byte[] chineseLetterInUtf8 = { (byte) 0xE7, (byte) 0x9A, (byte) 0x84 };

        AWSIotMessage message = new AWSIotMessage("topic", AWSIotQos.QOS0, "的");

        assertTrue(Arrays.equals(chineseLetterInUtf8, message.getPayload()));
    }

    @Test
    public void testInvalidMultiByteString() {
        byte[] chineseLetterInUtf8 = { (byte) 0xE7, (byte) 0x9A };

        AWSIotMessage message = new AWSIotMessage("topic", AWSIotQos.QOS0, chineseLetterInUtf8);

        assertEquals("�", message.getStringPayload());
    }

}
