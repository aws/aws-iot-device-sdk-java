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

import java.util.logging.Logger;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.core.AwsIotCompletion;
import com.amazonaws.services.iot.client.shadow.AwsIotDeviceCommandManager.Command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * This is a helper class that can be used to manage the execution result of a
 * shadow command, i.e. get, update, and delete. It makes sure that the command
 * is not published until the subscription requests for the acknowledgment
 * topics, namely accepted and rejected, have completed successfully.
 * 
 * @see com.amazonaws.services.iot.client.core.AwsIotCompletion
 */
@Getter
@Setter
public class AwsIotDeviceCommand extends AwsIotCompletion {

    private static final Logger LOGGER = Logger.getLogger(AwsIotDeviceCommand.class.getName());

    private final AwsIotDeviceCommandManager commandManager;
    private final Command command;
    private final String commandId;

    private AWSIotMessage response;

    @Setter(AccessLevel.NONE)
    private Boolean requestSent;

    public AwsIotDeviceCommand(AwsIotDeviceCommandManager commandManager, Command command, String commandId,
            AWSIotMessage request, long commandTimeout, boolean isAsync) {
        super(request, commandTimeout, isAsync);
        this.commandManager = commandManager;
        this.command = command;
        this.commandId = commandId;
        this.requestSent = false;
    }

    public void put(AbstractAwsIotDevice device) throws AWSIotException {
        if (device.isCommandReady(command)) {
            _put(device);
        } else {
            LOGGER.info("Request is pending: " + command.name() + "/" + commandId);
        }
    }

    public String get(AbstractAwsIotDevice device) throws AWSIotException, AWSIotTimeoutException {
        super.get(device.getClient());
        return (response != null) ? response.getStringPayload() : null;
    }

    public boolean onReady(AbstractAwsIotDevice device) {
        try {
            LOGGER.info("Request is resumed: " + command.name() + "/" + commandId);
            _put(device);
            return true;
        } catch (AWSIotException e) {
            return false;
        }
    }

    @Override
    public void onSuccess() {
        // first callback is for the command ack, which we ignore
        if (response == null) {
            return;
        } else {
            request.setPayload(response.getPayload());
        }

        super.onSuccess();
    }

    @Override
    public void onFailure() {
        super.onFailure();
    }

    @Override
    public void onTimeout() {
        commandManager.onCommandTimeout(this);
        super.onTimeout();
    }

    private void _put(AbstractAwsIotDevice device) throws AWSIotException {
        synchronized (this) {
            if (requestSent) {
                LOGGER.warning("Request was already sent: " + command.name() + "/" + commandId);
                return;
            } else {
                requestSent = true;
            }
        }

        device.getClient().publish(this, timeout);
    }

}
