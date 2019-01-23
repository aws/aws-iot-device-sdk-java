package com.amazonaws.services.iot.client.greengrass;

import java.util.List;

public class DiscoveryInfo {
    public List<DiscoveryGroupInfo> GGGroups;
    public DiscoveryError discoveryError;
    public Throwable discoveryThrowable;
}
