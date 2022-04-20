package com.amazonaws.services.iot.client;

import java.util.Arrays;
import java.util.logging.Logger;

public class TestTopic extends AWSIotTopic {

    private static final Logger LOGGER = Logger.getLogger(TestTopic.class.getName());

    public boolean isSuccess;
    public boolean isFailure;
    public boolean isTimeout;
    public long expectedMessageCount;
    public long unexpectedMessageCount;
    public byte[] lastPayload;

    public final byte[] expected;
    
    public TestTopic(String topic, byte[] expected) {
        super(topic);
        this.expected = expected;
    }

    @Override
    public void onMessage(AWSIotMessage message) {
        LOGGER.finer("message received for " + topic);
        lastPayload = message.getPayload();
        if (Arrays.equals(expected, lastPayload)) {
            expectedMessageCount++;
        } else {
            unexpectedMessageCount++;
        }
    }

    @Override
    public void onSuccess() {
        LOGGER.finer("request success for " + topic);
        isSuccess = true;
    }

    @Override
    public void onFailure() {
        LOGGER.finer("request failure for " + topic);
        isFailure = true;
    }

    @Override
    public void onTimeout() {
        LOGGER.finer("request timeout for " + topic);
        isTimeout = true;
    }

}
