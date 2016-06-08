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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * This is a customized JSON serializer for the Jackson databind module. It is
 * used for serializing the device properties to be reported to the shadow.
 */
public class AwsIotJsonSerializer extends JsonSerializer<AbstractAwsIotDevice> {

    @Override
    public void serialize(AbstractAwsIotDevice device, JsonGenerator generator, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        generator.writeStartObject();

        try {
            for (String property : device.getReportedProperties().keySet()) {
                Field field = device.getReportedProperties().get(property);

                Object value = invokeGetterMethod(device, field);
                generator.writeObjectField(property, value);
            }
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }

        generator.writeEndObject();
    }

    private static Object invokeGetterMethod(Object target, Field field) throws IOException {
        String fieldName = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        String getter = "get" + fieldName;

        Method method;
        try {
            method = target.getClass().getMethod(getter);
        } catch (NoSuchMethodException | SecurityException e) {
            if (e instanceof NoSuchMethodException && boolean.class.equals(field.getType())) {
                getter = "is" + fieldName;
                try {
                    method = target.getClass().getMethod(getter);
                } catch (NoSuchMethodException | SecurityException ie) {
                    throw new IllegalArgumentException(ie);
                }
            } else {
                throw new IllegalArgumentException(e);
            }
        }

        Object value;
        try {
            value = method.invoke(target);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IOException(e);
        }
        return value;
    }

}
