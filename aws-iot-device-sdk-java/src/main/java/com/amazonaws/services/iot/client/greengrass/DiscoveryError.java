package com.amazonaws.services.iot.client.greengrass;

public enum DiscoveryError {
    /**
     * The Greengrass group has not populated its discovery information. This is normally done by the GGIPDetector. If
     * the GGIPDetector is not in use the discovery information needs to be populated manually. If the GGIPDetector is
     * in use it may be experiencing an issue (no connectivity, more than 10 IPs detected).
     */
    GreengrassGroupsDiscoveryInfoHasNotBeenPopulated,

    /**
     * The Greengrass discovery information could not be found for this thing. This could be because the thing does not
     * exist, the thing is not associated with a group, or the thing's policy is not permissive enough for it to perform
     * discovery
     */
    DiscoveryInfoForThingNotFound,

    /**
     * The certificate used by the application is not properly registered in AWS IoT
     */
    DeviceCertificateNotKnownToAwsIoT,

    /**
     * The reason for the failure is unknown. Inspecting the associated exception may provide additional details.
     */
    Unknown;
}
