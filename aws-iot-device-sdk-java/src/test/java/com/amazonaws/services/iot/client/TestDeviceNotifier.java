package com.amazonaws.services.iot.client;

public interface TestDeviceNotifier {

    void reportAlarm(String sensorName, Float sensorValue, Float threshold);

}
