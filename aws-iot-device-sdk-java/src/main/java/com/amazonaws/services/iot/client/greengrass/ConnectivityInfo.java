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
import java.util.Objects;

/**
 * Class the stores one set of the connectivity information.
 */
public class ConnectivityInfo implements Serializable {

    /**
     * <p>
     * Connectivity Information Id.
     * </p>
     */
    @JsonProperty(value = "Id")
    private String id;

    /**
     * <p>
     * Host address.
     * </p>
     */
    @JsonProperty(value = "HostAddress")
    private String hostAddress;

    /**
     * <p>
     * Port number.
     * </p>
     */
    @JsonProperty(value = "PortNumber")
    private Integer portNumber;

    /**
     * <p>
     * Metadata string.
     * </p>
     */
    @JsonProperty(value = "Metadata")
    private String metadata;

    /**
     * <p>
     * Connectivity Information Id.
     * </p>
     *
     * @return Connectivity Information Id.
     */
    public String getId() {
        return id;
    }

    /**
     * <p>
     * Connectivity Information Id.
     * </p>
     *
     * @param id
     *            Connectivity Information Id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * <p>
     * Connectivity Information Id.
     * </p>
     *
     * @param id
     *            Connectivity Information Id.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public ConnectivityInfo withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * <p>
     * Host address.
     * </p>
     *
     * @return Host address.
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * <p>
     * Host address.
     * </p>
     *
     * @param hostAddress
     *            Host address.
     */
    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    /**
     * <p>
     * Host address.
     * </p>
     *
     * @param hostAddress
     *            Host address.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public ConnectivityInfo withHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
        return this;
    }

    /**
     * <p>
     * Port number.
     * </p>
     *
     * @return Port number.
     */
    public Integer getPortNumber() {
        return portNumber;
    }

    /**
     * <p>
     * Port number.
     * </p>
     *
     * @param portNumber
     *            Port number.
     */
    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    /**
     * <p>
     * Port number.
     * </p>
     *
     * @param portNumber
     *            Port number.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public ConnectivityInfo withPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
        return this;
    }

    /**
     * <p>
     * Metadata string.
     * </p>
     *
     * @return Metadata string.
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * <p>
     * Metadata string.
     * </p>
     *
     * @param metadata
     *            Metadata string.
     */
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * <p>
     * Metadata string.
     * </p>
     *
     * @param metadata
     *            Metadata string.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public ConnectivityInfo withMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hostAddress, portNumber, metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectivityInfo that = (ConnectivityInfo) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(hostAddress, that.hostAddress) &&
            Objects.equals(portNumber, that.portNumber) &&
            Objects.equals(metadata, that.metadata);
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
        if (getId() != null)
            sb.append("Id: ").append(getId()).append(',');
        if (getHostAddress() != null)
            sb.append("HostAddress: ").append(getHostAddress()).append(',');
        if (getPortNumber() != null)
            sb.append("PortNumber: ").append(getPortNumber()).append(',');
        if (getMetadata() != null)
            sb.append("Metadata: ").append(getMetadata());
        sb.append("}");
        return sb.toString();
    }

}
