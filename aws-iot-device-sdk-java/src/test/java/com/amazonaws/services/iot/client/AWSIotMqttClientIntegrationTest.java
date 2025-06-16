package com.amazonaws.services.iot.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.UUID;

public class AWSIotMqttClientIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(AWSIotMqttClientIntegrationTest.class.getName());
    // Use UID to distinguish concurrent running test 
    private static final String TOPIC_UID = UUID.randomUUID().toString();
    private static final String WILL_TOPIC = TOPIC_UID+"client/status";

    private AWSIotMqttClient client;
    private AWSIotMqttClient receiverClient;
    private AWSIotMqttClient competingClient;

    @BeforeClass
    public static void init() {
        AWSIotMqttClientIntegrationUtil.enableConsoleLogging(LOGGER);
    }

    @Before
    public void setup() {
        client = AWSIotMqttClientIntegrationUtil.getClient();
        assertNotNull("Client not initialized likely due to required system properties not being provided", client);
    }

    @After
    public void cleanup() {
        try {
            if (client != null) {
                client.disconnect();
                client = null;
            }
            if (receiverClient != null) {
                receiverClient.disconnect();
                receiverClient = null;
            }
            if (competingClient != null) {
                competingClient.disconnect();
                competingClient = null;
            }
        } catch (Exception e) {
            // ignore cleanup errors
        }
    }

    @Test
    public void testPublishQos0() throws AWSIotException, InterruptedException, AWSIotTimeoutException {
        boolean success = publishToTopics(10, AWSIotQos.QOS0, 32, true, 0, 5000);

        assertTrue("not all the topics were received", success);
    }

    @Test
    public void testPublishQos1() throws AWSIotException, InterruptedException, AWSIotTimeoutException {
        boolean success = publishToTopics(10, AWSIotQos.QOS1, 32, true, 0, 5000);

        assertTrue("not all the topics were received", success);
    }

    @Test
    public void testPublishFatMessageQos0() throws AWSIotException, InterruptedException, AWSIotTimeoutException {
        boolean success = publishToTopics(1, AWSIotQos.QOS0, 32000, true, 0, 5000);

        assertTrue("not all the topics were received", success);
    }

    @Test
    public void testPublishFatMessageQos1() throws AWSIotException, InterruptedException, AWSIotTimeoutException {
        boolean success = publishToTopics(1, AWSIotQos.QOS1, 32000, true, 0, 5000);

        assertTrue("not all the topics were received", success);
    }

    @Test
    public void testAsyncPublishQos0() throws AWSIotException, InterruptedException, AWSIotTimeoutException {
        boolean success = publishToTopics(10, AWSIotQos.QOS0, 32, false, 0, 5000);

        assertTrue("not all the topics were received", success);
    }

    @Test
    public void testAsyncPublishQos1() throws AWSIotException, InterruptedException, AWSIotTimeoutException {
        boolean success = publishToTopics(10, AWSIotQos.QOS1, 32, false, 0, 5000);

        assertTrue("not all the topics were received", success);
    }

    @Test
    public void testLastWill() throws AWSIotException, InterruptedException {
        receiverClient = AWSIotMqttClientIntegrationUtil.getClient("-receiver");
        receiverClient.connect();

        TestTopic willTopic = new TestTopic(WILL_TOPIC, "will payload".getBytes());
        receiverClient.subscribe(willTopic);

        AWSIotMessage lastWill = new AWSIotMessage(willTopic.getTopic(), AWSIotQos.QOS0, willTopic.expected);
        client = AWSIotMqttClientIntegrationUtil.getClient("-victim");
        client.setWillMessage(lastWill);
        client.connect();

        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000
                && !AWSIotConnectionStatus.CONNECTED.equals(client.getConnectionStatus())) {
            Thread.sleep(250);
        }
        assertEquals(AWSIotConnectionStatus.CONNECTED, client.getConnectionStatus());

        // create a competing client with the same client Id, so the previous
        // one will be kicked off by the server
        competingClient = AWSIotMqttClientIntegrationUtil.getClient("-victim");
        competingClient.connect();

        start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000
                && !(AWSIotConnectionStatus.DISCONNECTED.equals(client.getConnectionStatus())
                        && AWSIotConnectionStatus.CONNECTED.equals(competingClient.getConnectionStatus()))) {
            Thread.sleep(250);
        }
        assertEquals(AWSIotConnectionStatus.DISCONNECTED, client.getConnectionStatus());
        assertEquals(AWSIotConnectionStatus.CONNECTED, competingClient.getConnectionStatus());

        // sleep 2s to make sure we receive the will message.
        Thread.sleep(2000);

        receiverClient.disconnect();
        competingClient.disconnect();

        assertEquals(1, willTopic.expectedMessageCount);
        assertEquals(0, willTopic.unexpectedMessageCount);
    }

    @Test(expected = AWSIotException.class)
    public void testExceptionOnSubscribeToMoreThanMaxAllowedTopics() throws AWSIotException, InterruptedException {
        client.connect();

        // IoT service has a soft limit at 50, using 100 to ensure we definitely exceed permitted maximum
        for (int i = 0; i < 100; i++) {
            String topic = "test/topic/" + i;

            LOGGER.info("Subscribing to topic: " + topic);
            TestTopic listener = new TestTopic(topic, "test".getBytes());
            client.subscribe(listener, true);
        }
    }

    @Test
    public void testWildcardTopicFilters() throws AWSIotException, InterruptedException, AWSIotTimeoutException {
        Random random = new Random();
        byte[] payload = new byte[256];
        random.nextBytes(payload);

        List<TestTopic> topics = new ArrayList<>();
        TestTopic topic1 = new TestTopic(TOPIC_UID+"one/test/topic", payload);
        topics.add(topic1);
        TestTopic topic2 = new TestTopic(TOPIC_UID+"one/+/topic", payload);
        topics.add(topic2);
        TestTopic topic3 = new TestTopic(TOPIC_UID+"one/test/+", payload);
        topics.add(topic3);
        TestTopic topic4 = new TestTopic(TOPIC_UID+"one/#", payload);
        topics.add(topic4);

        client.connect();

        client.subscribe(topic1, true);
        client.subscribe(topic2, true);
        client.subscribe(topic3, true);
        client.subscribe(topic4, true);

        client.publish(topic1.getTopic(), AWSIotQos.QOS0, topic1.expected);

        long start = System.currentTimeMillis();
        Set<TestTopic> completedTopics = new HashSet<>();
        while (System.currentTimeMillis() - start < 5000 && topics.size() > completedTopics.size()) {
            for (TestTopic topic : topics) {
                if (topic.expectedMessageCount > 0 || topic.unexpectedMessageCount > 0) {
                    completedTopics.add(topic);
                }
            }
            Thread.sleep(250);
        }

        LOGGER.fine("unsubscribing to topics");
        for (TestTopic topic : topics) {
            client.unsubscribe(topic.getTopic());
        }

        client.disconnect();

        for (TestTopic topic : topics) {
            assertTrue("Topic " + topic.getTopic(), topic.expectedMessageCount > 0 && topic.unexpectedMessageCount == 0);
        }
    }

    private boolean publishToTopics(int numOfTopics, AWSIotQos qos, int payloadSize, boolean blocking,
            long publishTimeout, long receiveTimeout)
            throws AWSIotException, InterruptedException, AWSIotTimeoutException {
        List<TestTopic> topics = new ArrayList<>();

        Random random = new Random();
        for (int i = 0; i < numOfTopics; i++) {
            byte[] payload = new byte[payloadSize];
            random.nextBytes(payload);
            TestTopic topic = new TestTopic("test/topic/"+TOPIC_UID+"/" + i, payload);
            topics.add(topic);
        }

        client.connect();

        LOGGER.fine("subscribing to topics");
        for (TestTopic topic : topics) {
            client.subscribe(topic, true);
        }

        LOGGER.fine("publishing topics");
        for (TestTopic topic : topics) {
            if (blocking) {
                client.publish(topic.getTopic(), qos, topic.expected, publishTimeout);
            } else {
                topic.setPayload(topic.expected);
                topic.setQos(qos);
                client.publish(topic, publishTimeout);
            }
        }

        long start = System.currentTimeMillis();
        Set<TestTopic> completedTopics = new HashSet<>();
        while (System.currentTimeMillis() - start < receiveTimeout && topics.size() > completedTopics.size()) {
            for (TestTopic topic : topics) {
                if (topic.expectedMessageCount > 0 || topic.unexpectedMessageCount > 0) {
                    completedTopics.add(topic);
                }
            }
            Thread.sleep(250);
        }

        LOGGER.fine("unsubscribing to topics");
        for (TestTopic topic : topics) {
            client.unsubscribe(topic.getTopic());
        }

        client.disconnect();

        boolean success = true;
        for (TestTopic topic : topics) {
            if (!blocking) {
                if (!topic.isSuccess || topic.isFailure || topic.isTimeout) {
                    LOGGER.warning("async publish failed for " + topic.topic);
                    success = false;
                }
            }
            if (topic.expectedMessageCount != 1 || topic.unexpectedMessageCount != 0) {
                LOGGER.warning("test failed for " + topic.topic);
                success = false;
            }
        }

        return success;
    }

}
