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

package com.amazonaws.services.iot.client.sample.shadowEcho;

/**
 * This POJO class defines a simple document for communicating with the AWS IoT
 * thing. It only contains one property ({@code counter}).
 */
public class Thing {

    public State state = new State();

    public static class State {
        public Document reported = new Document();
        public Document desired = new Document();
    }

    public static class Document {
        public long counter = 1;
    }

}
