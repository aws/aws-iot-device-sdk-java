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

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Class that stores the discovery information coming back from the discovery request.
 */
public class DiscoveryInfo implements Serializable {

    /**
     * <p>
     * The list of AWS Greengrass groups that device is a member of.
     * </p>
     */
    @JsonProperty(value = "GGGroups")
    private List<GroupConnectivityInfo> ggGroups;

    /**
     * <p>
     * The list of AWS Greengrass groups that device is a member of.
     * </p>
     *
     * @return The list of AWS Greengrass groups.
     */
    public List<GroupConnectivityInfo> getGgGroups() {
        return ggGroups;
    }

    /**
     * <p>
     * The list of AWS Greengrass groups that device is a member of.
     * </p>
     *
     * @param ggGroups
     *            The list of AWS Greengrass groups.
     */
    public void setGgGroups(List<GroupConnectivityInfo> ggGroups) {
        this.ggGroups = ggGroups;
    }

    /**
     * <p>
     * The list of AWS Greengrass groups that device is a member of.
     * </p>
     *
     * @param ggGroups
     *            The list of AWS Greengrass groups.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public DiscoveryInfo withGgGroups(List<GroupConnectivityInfo> ggGroups) {
        this.ggGroups = ggGroups;
        return this;
    }

    /**
     * <p>
     * The list of AWS Greengrass groups that device is a member of.
     * </p>
     *
     * @param ggGroups
     *            The list of AWS Greengrass groups.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    public DiscoveryInfo withGgGroups(GroupConnectivityInfo... ggGroups) {
        this.ggGroups = Arrays.asList(ggGroups);
        return this;
    }

    /**
     * <p>
     * Used to retrieve the list of CAs for this discovery information
     * </p>
     *
     * @return List Certificate objects
     * @throws CertificateException on parsing errors.
     */
    public List<Certificate> getAllCas() throws CertificateException {
        if (ggGroups == null) {
            return Collections.emptyList();
        }

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        List<Certificate> cas = new ArrayList<>();
        for (GroupConnectivityInfo group : ggGroups) {
            for (String ca : group.getCas()) {
                cas.addAll(factory.generateCertificates(new ByteArrayInputStream(ca.getBytes())));
            }
        }

        return cas;
    }

    /**
     * <p>
     * Used to retrieve the list of CoreConnectivityInfo object for this discovery information.
     * </p>
     *
     * @return List CoreConnectivityInfo objects
     */
    public List<CoreConnectivityInfo> getAllCores() {
        if (ggGroups == null) {
            return Collections.emptyList();
        }

        List<CoreConnectivityInfo> cores = new ArrayList<>();
        for (GroupConnectivityInfo group : ggGroups) {
            cores.addAll(group.getCores());
        }

        return cores;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ggGroups);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscoveryInfo that = (DiscoveryInfo) o;
        return Objects.equals(ggGroups, that.ggGroups);
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
        if (getGgGroups() != null)
            sb.append("GGGroups: ").append(getGgGroups());
        sb.append("}");
        return sb.toString();
    }

}
