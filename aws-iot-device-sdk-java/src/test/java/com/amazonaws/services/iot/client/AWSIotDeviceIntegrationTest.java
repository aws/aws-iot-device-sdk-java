package com.amazonaws.services.iot.client;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.UUID;

public class AWSIotDeviceIntegrationTest implements TestDeviceNotifier {

    private static final Logger LOGGER = Logger.getLogger(AWSIotDeviceIntegrationTest.class.getName());
    private static final String UPDATE_TOPIC = "$aws/things/?/shadow/update";
    // Added UID to distinguish concurrent running test 
    private static final String THING_NAME = System.getProperty("thingName") + UUID.randomUUID().toString();
    private static final String STABILITY_TEST_ITERATIONS = System.getProperty("stabilityTestIterations");

    private AWSIotMqttClient client;
    private TestDevice device;

    @BeforeClass
    public static void init() {
        AWSIotMqttClientIntegrationUtil.enableConsoleLogging(LOGGER);
    }

    @Before
    public void setup() {
        client = AWSIotMqttClientIntegrationUtil.getClient();
        assertNotNull("Client not initialized likely due to required system properties not being provided", client);
        assertNotNull("thingName was not provided", THING_NAME);
    }

    @After
    public void cleanup() {
        try {
            if (client != null) {
                client.disconnect(500);
                client = null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "disconnect failed with exception", e);
        }
    }

    @Test
    public void testDeviceReport() throws AWSIotException, InterruptedException, UnsupportedEncodingException {
        String serailNumber = "123-456-789";
        Float threshold = Float.valueOf(0f);
        Map<String, Float> sensors = new HashMap<>();
        sensors.put("sensor-0", Float.valueOf(0f));
        sensors.put("sensor-1", Float.valueOf(0f));
        sensors.put("sensor-2", Float.valueOf(0f));

        device = new TestDevice(THING_NAME, serailNumber, threshold, sensors);
        device.setReportInterval(1000);
        client.attach(device);

        client.connect();

        device.delete();

        String deltaTopic = UPDATE_TOPIC.replace("?", THING_NAME);
        TestTopic topic = new TestTopic(deltaTopic, null);
        client.subscribe(topic);

        Thread.sleep(2000);

        assertNotNull(topic.lastPayload);
        String update = new String(topic.lastPayload, "UTF-8");
        assertTrue(update.contains("\"threshold\":0.0"));
        assertTrue(update.contains("\"serialNumber\":\"123-456-789\""));
        assertTrue(update.contains("\"sensor-0\":0.0"));
        assertTrue(update.contains("\"sensor-1\":0.0"));
        assertTrue(update.contains("\"sensor-2\":0.0"));
    }

    @Test
    public void testDeviceUpdate() throws AWSIotException, InterruptedException, UnsupportedEncodingException {
        String serailNumber = "123-456-789";
        Float threshold = Float.valueOf(0f);
        Map<String, Float> sensors = new HashMap<>();
        sensors.put("sensor-0", Float.valueOf(0f));
        sensors.put("sensor-1", Float.valueOf(0f));
        sensors.put("sensor-2", Float.valueOf(0f));

        device = new TestDevice(THING_NAME, serailNumber, threshold, sensors);
        device.setReportInterval(1000);
        client.attach(device);

        client.connect();

        device.delete();

        Thread.sleep(1000);

        String desired = "{\"state\":{\"desired\":{\"threshold\":3.0}}}";
        device.update(desired);

        Thread.sleep(2000);
        assertEquals(Float.valueOf(3.0f), device.getThreshold());
    }

    @Test
    public void testDeviceReportAndUpdate() throws AWSIotException, InterruptedException, UnsupportedEncodingException {
        String serailNumber = "123-456-789";
        Float threshold = Float.valueOf(0f);
        Map<String, Float> sensors = new HashMap<>();
        sensors.put("sensor-0", Float.valueOf(0f));
        sensors.put("sensor-1", Float.valueOf(0f));
        sensors.put("sensor-2", Float.valueOf(0f));

        device = new TestDevice(THING_NAME, serailNumber, threshold, sensors);
        device.setReportInterval(1000);
        device.addNotifier(this);
        device.startMonitoring();

        client.attach(device);

        client.connect();

        device.delete();

        String desired = "{\"state\":{\"desired\":{\"threshold\":-10.0}}}";
        device.update(desired);

        Thread.sleep(3000);
        device.stopMonitoring();

        assertEquals(Float.valueOf(-10f), device.getSensorValue("sensor-0"));
        assertEquals(Float.valueOf(-10f), device.getSensorValue("sensor-1"));
        assertEquals(Float.valueOf(-10f), device.getSensorValue("sensor-2"));

        String state = device.get();
        assertTrue(state.contains("\"sensor-0\":-10.0"));
        assertTrue(state.contains("\"sensor-1\":-10.0"));
        assertTrue(state.contains("\"sensor-2\":-10.0"));
    }

    @Test
    public void testDeviceStability() throws AWSIotException, InterruptedException, UnsupportedEncodingException {
        String serailNumber = "123-456-789";
        Float threshold = Float.valueOf(0f);
        Map<String, Float> sensors = new HashMap<>();
        sensors.put("sensor-0", Float.valueOf(0f));
        sensors.put("sensor-1", Float.valueOf(0f));
        sensors.put("sensor-2", Float.valueOf(0f));

        device = new TestDevice(THING_NAME, serailNumber, threshold, sensors);
        device.setReportInterval(1000);
        device.addNotifier(this);
        device.startMonitoring();

        client.attach(device);

        AWSIotMessage willMessage = new AWSIotMessage("test/disconnect", AWSIotQos.QOS0, client.getClientId());
        client.setWillMessage(willMessage);
        client.connect();

        device.delete();

        Float value = Float.valueOf(0.0f);
        long testIterations = STABILITY_TEST_ITERATIONS == null ? 10 : Long.parseLong(STABILITY_TEST_ITERATIONS);
        while (testIterations-- > 0) {
            value -= 10.0f;
            device.update("{\"state\":{\"desired\":{\"threshold\":" + value.toString() + "}}}");
            Thread.sleep(3000);
        }

        String state = device.get();
        assertTrue(state.contains("\"sensor-0\":" + value));
        assertTrue(state.contains("\"sensor-1\":" + value));
        assertTrue(state.contains("\"sensor-2\":" + value));
    }

    @Override
    public void reportAlarm(String sensorName, Float sensorValue, Float threshold) {
        device.setSensorValue(sensorName, sensorValue - 1.0f);
    }

}
