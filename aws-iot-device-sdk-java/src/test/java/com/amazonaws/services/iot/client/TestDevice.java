package com.amazonaws.services.iot.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotDeviceProperty;

public class TestDevice extends AWSIotDevice {

    private static final Logger LOGGER = Logger.getLogger(AWSIotDeviceIntegrationTest.class.getName());

    @AWSIotDeviceProperty(allowUpdate = false)
    private String serialNumber;

    @AWSIotDeviceProperty
    private Float threshold;

    @AWSIotDeviceProperty
    private Map<String, Float> sensors;

    private final List<TestDeviceNotifier> notifiers = new ArrayList<>();

    private boolean enableMonitoring;
    private Thread monitoringThread;

    public TestDevice(String thingName, String serialNumber, Float threshold, Map<String, Float> sensors) {
        super(thingName);
        this.serialNumber = serialNumber;
        this.threshold = threshold;
        this.sensors = new HashMap<>(sensors);
        this.threshold = Float.valueOf(0f);
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Float getThreshold() {
        return threshold;
    }

    public void setThreshold(Float threshold) {
        this.threshold = threshold;
    }

    public Map<String, Float> getSensors() {
        return new HashMap<String, Float>(sensors);
    }

    public void setSensors(Map<String, Float> sensors) {
        this.sensors = new HashMap<String, Float>(sensors);
    }

    public Float getSensorValue(String sensorName) {
        return sensors.get(sensorName);
    }

    public void setSensorValue(String sensorName, Float sensorValue) {
        sensors.put(sensorName, sensorValue);
    }

    public Thread startMonitoring() {
        if (monitoringThread != null) {
            return monitoringThread;
        }

        enableMonitoring = true;
        monitoringThread = new Thread(new Runnable() {
            @Override
            public void run() {
                monitoringThread();
            }
        });

        monitoringThread.start();
        return monitoringThread;
    }

    public void stopMonitoring() {
        if (monitoringThread != null) {
            enableMonitoring = false;
            try {
                monitoringThread.join();
            } catch (InterruptedException e) {
                LOGGER.warning("joining monitoring thread got interrupted");
            }
            monitoringThread = null;
        }
    }

    public void addNotifier(TestDeviceNotifier notifier) {
        notifiers.add(notifier);
    }

    public void removeNotifier(TestDeviceNotifier notifier) {
        notifiers.remove(notifier);
    }

    private void monitoringThread() {
        while (enableMonitoring) {
            for (Entry<String, Float> entry : sensors.entrySet()) {
                if (entry.getValue().compareTo(threshold) > 0) {
                    LOGGER.finer("sensor " + entry.getKey() + " exceeded threshould (" + threshold + "): "
                            + entry.getValue());

                    for (TestDeviceNotifier notifier : notifiers) {
                        notifier.reportAlarm(entry.getKey(), entry.getValue(), threshold);
                    }
                }
            }

            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                LOGGER.warning("monitoring thread got interrupted");
                break;
            }
        }
    }
}
