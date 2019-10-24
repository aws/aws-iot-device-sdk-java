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
 * Class that stores the connectivity information for a specific Greengrass group.
 */
public class GroupConnectivityInfo implements Serializable {

    /**
     * <p>
     * Id for this Greengrass group.
     * </p>
     */
    @JsonProperty(value = "GGGroupId")
    private String ggGroupId;

    /**
     * <p>
     * A list of Greengrass cores that belong to this Greengrass group.
     * </p>
     */
    @JsonProperty(value = "Cores")
    private List<CoreConnectivityInfo> cores;

    /**
     * <p>
     * A list of CA content strings for this Greengrass group.
     * </p>
     */
    @JsonProperty(value = "CAs")
    private List<String> cas;

    /**
     * <p>
     * Id for this Greengrass group.
     * </p>
     *
     * @return Id for this Greengrass group.
     */
    public String getGgGroupId() {
        return ggGroupId;
    }

    /**
     * <p>
     * Id for this Greengrass group.
     * </p>
     *
     * @param ggGroupId
     *            Id for this Greengrass group.
     */
    public void setGgGroupId(String ggGroupId) {
        this.ggGroupId = ggGroupId;
    }

    /**
     * <p>
     * Id for this Greengrass group.
     * </p>
     *
     * @param ggGroupId
     *            Id for this Greengrass group.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public GroupConnectivityInfo withGgGroupId(String ggGroupId) {
        this.ggGroupId = ggGroupId;
        return this;
    }

    /**
     * <p>
     * A list of Greengrass cores that belong to this Greengrass group.
     * </p>
     *
     * @return A list of Greengrass cores that belong to this Greengrass group.
     */
    public List<CoreConnectivityInfo> getCores() {
        return cores;
    }

    /**
     * <p>
     * A list of Greengrass cores that belong to this Greengrass group.
     * </p>
     *
     * @param cores
     *            A list of Greengrass cores.
     */
    public void setCores(List<CoreConnectivityInfo> cores) {
        this.cores = cores;
    }

    /**
     * <p>
     * A list of Greengrass cores that belong to this Greengrass group.
     * </p>
     *
     * @param cores
     *            A list of Greengrass cores.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public GroupConnectivityInfo withCores(List<CoreConnectivityInfo> cores) {
        this.cores = cores;
        return this;
    }

    /**
     * <p>
     * A list of Greengrass cores that belong to this Greengrass group.
     * </p>
     *
     * @param cores
     *            A list of Greengrass cores.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public GroupConnectivityInfo withCores(CoreConnectivityInfo ... cores) {
        this.cores = Arrays.asList(cores);
        return this;
    }

    /**
     * <p>
     * A list of CA content strings for this Greengrass group.
     * </p>
     *
     * @return A list of CA content strings.
     */
    public List<String> getCas() {
        return cas;
    }

    /**
     * <p>
     * A list of CA content strings for this Greengrass group.
     * </p>
     *
     * @param cas
     *            A list of CA content strings.
     */
    public void setCas(List<String> cas) {
        this.cas = cas;
    }

    /**
     * <p>
     * A list of CA content strings for this Greengrass group.
     * </p>
     *
     * @param cas
     *            A list of CA content strings.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public GroupConnectivityInfo withCas(List<String> cas) {
        this.cas = cas;
        return this;
    }

    /**
     * <p>
     * A list of CA content strings for this Greengrass group.
     * </p>
     *
     * @param cas
     *            A list of CA content strings.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public GroupConnectivityInfo withCas(String ... cas) {
        this.cas = Arrays.asList(cas);
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ggGroupId, cores, cas);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupConnectivityInfo that = (GroupConnectivityInfo) o;
        return Objects.equals(ggGroupId, that.ggGroupId) &&
            Objects.equals(cores, that.cores) &&
            Objects.equals(cas, that.cas);
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
        if (getGgGroupId() != null)
            sb.append("GGGroupId: ").append(getGgGroupId()).append(',');
        if (getCores() != null)
            sb.append("Cores: ").append(getCores()).append(',');
        if (getCas() != null)
            sb.append("CAs: ").append(getCas());
        sb.append("}");
        return sb.toString();
    }

}
