/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.services.iot.client.greengrass;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Class that stores the connectivity information for a Greengrass core.
 */
public class CoreConnectivityInfo implements Serializable {

    /**
     * <p>
     * Thing arn for this Greengrass core.
     * </p>
     */
    @JsonProperty(value = "thingArn")
    private String thingArn;

    /**
     * <p>
     * The list of connectivity information that this Greengrass core has.
     * </p>
     */
    @JsonProperty(value = "Connectivity")
    private List<ConnectivityInfo> connectivity;

    /**
     * <p>
     * Thing arn for this Greengrass core.
     * </p>
     *
     * @return Thing arn for this Greengrass core.
     */
    public String getThingArn() {
        return thingArn;
    }

    /**
     * <p>
     * Thing arn for this Greengrass core.
     * </p>
     *
     * @param thingArn
     *            Thing arn for this Greengrass core.
     */
    public void setThingArn(String thingArn) {
        this.thingArn = thingArn;
    }

    /**
     * <p>
     * Thing arn for this Greengrass core.
     * </p>
     *
     * @param thingArn
     *            Thing arn for this Greengrass core.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public CoreConnectivityInfo withThingArn(String thingArn) {
        this.thingArn = thingArn;
        return this;
    }

    /**
     * <p>
     * The list of connectivity information that this Greengrass core has.
     * </p>
     *
     * @return The list of connectivity information.
     */
    public List<ConnectivityInfo> getConnectivity() {
        return connectivity;
    }

    /**
     * <p>
     * The list of connectivity information that this Greengrass core has.
     * </p>
     *
     * @param connectivity
     *            The list of connectivity information.
     */
    public void setConnectivity(List<ConnectivityInfo> connectivity) {
        this.connectivity = connectivity;
    }

    /**
     * <p>
     * The list of connectivity information that this Greengrass core has.
     * </p>
     *
     * @param connectivity
     *            The list of connectivity information.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public CoreConnectivityInfo withConnectivity(List<ConnectivityInfo> connectivity) {
        this.connectivity = connectivity;
        return this;
    }

    /**
     * <p>
     * The list of connectivity information that this Greengrass core has.
     * </p>
     *
     * @param connectivity
     *            The list of connectivity information.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public CoreConnectivityInfo withConnectivity(ConnectivityInfo ... connectivity) {
        this.connectivity = Arrays.asList(connectivity);
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(thingArn, connectivity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoreConnectivityInfo that = (CoreConnectivityInfo) o;
        return Objects.equals(thingArn, that.thingArn) &&
            Objects.equals(connectivity, that.connectivity);
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     *
     * @return A string representation of this object.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getThingArn() != null)
            sb.append("thingArn: ").append(getThingArn()).append(',');
        if (getConnectivity() != null)
            sb.append("Connectivity: ").append(getConnectivity());
        sb.append("}");
        return sb.toString();
    }

}
