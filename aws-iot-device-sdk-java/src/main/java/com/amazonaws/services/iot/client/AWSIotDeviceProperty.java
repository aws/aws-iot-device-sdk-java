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

package com.amazonaws.services.iot.client;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation class that is used to annotate properties in {@link AWSIotDevice}.
 * Properties annotated must be accessible via corresponding getter and setter
 * methods.
 * <p>
 * With optional values provided by this annotation class, properties can also
 * be configured to disable reporting to the shadow or not to accept updates
 * from the shadow. If {@link #enableReport()} is disabled, property getter
 * function is not required. Likewise, if {@link #allowUpdate()} is disabled for
 * a property, its setter method is not required.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AWSIotDeviceProperty {

    /**
     * An optional name can be provided to the annotated property, which will be
     * used for publishing to the shadow as well as receiving updates from the
     * shadow. If not provided, the actual property name will be used.
     *
     * @return the name of the property.
     */
    String name() default "";

    /**
     * Enable reporting the annotated property to the shadow. It's enabled by
     * default.
     *
     * @return true to enable reporting to the shadow, false otherwise.
     */
    boolean enableReport() default true;

    /**
     * Allow updates from the shadow. It's enabled by default.
     *
     * @return true to allow updates from the shadow, false otherwise.
     */
    boolean allowUpdate() default true;

}
