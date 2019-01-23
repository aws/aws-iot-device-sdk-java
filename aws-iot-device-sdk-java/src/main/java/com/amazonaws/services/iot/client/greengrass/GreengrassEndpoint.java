package com.amazonaws.services.iot.client.greengrass;

public class GreengrassEndpoint {
    public final String address;
    public final int port;

    public GreengrassEndpoint(String address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public String toString() {
        return String.join(":", address, String.valueOf(port));
    }
}
