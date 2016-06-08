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

package com.amazonaws.services.iot.client.shadow;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is a customized JSON deserializer for deserializing the delta update
 * document from the shadow.
 */
public class AwsIotJsonDeserializer {

    public static void deserialize(AbstractAwsIotDevice device, String jsonState) throws IOException {
        ObjectMapper jsonObjectMapper = device.getJsonObjectMapper();

        JsonNode node = jsonObjectMapper.readTree(jsonState);
        if (node == null) {
            throw new IOException("Invalid delta update received for " + device.getThingName());
        }

        for (Iterator<String> it = node.fieldNames(); it.hasNext();) {
            String property = it.next();
            Field field = device.getUpdatableProperties().get(property);
            JsonNode fieldNode = node.get(property);
            if (field == null || fieldNode == null) {
                continue;
            }

            updateDeviceProperty(jsonObjectMapper, fieldNode, device, field);
        }
    }

    public static long deserializeVersion(AbstractAwsIotDevice device, String jsonState) throws IOException {
        ObjectMapper jsonObjectMapper = device.getJsonObjectMapper();

        JsonNode node = jsonObjectMapper.readTree(jsonState);
        if (node == null) {
            throw new IOException("Invalid shadow document received for " + device.getThingName());
        }

        JsonNode versionNode = node.get("version");
        if (versionNode == null) {
            throw new IOException("Missing version field from shadow document for " + device.getThingName());
        }

        return versionNode.asLong();
    }

    private static void updateDeviceProperty(ObjectMapper jsonObjectMapper, JsonNode node, AbstractAwsIotDevice device,
            Field field) throws IOException {
        Object value = jsonObjectMapper.treeToValue(node, field.getType());
        invokeSetterMethod(device, field.getName(), field.getType(), value);
    }

    private static void invokeSetterMethod(Object target, String name, Class<?> type, Object value) throws IOException {
        String setter = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);

        Method method;
        try {
            method = target.getClass().getMethod(setter, type);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }

        try {
            method.invoke(target, value);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IOException(e);
        }
    }

}
