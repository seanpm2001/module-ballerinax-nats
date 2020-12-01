/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.nats.basic.consumer;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.observability.NatsMetricsReporter;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unsubscribe the consumer from the subject.
 *
 * @since 1.0.4
 */
public class Detach {
    private static final PrintStream console;

    public static Object basicDetach(BObject listener, BObject service) {
        @SuppressWarnings("unchecked")
        List<BObject> serviceList =
                (List<BObject>) listener.getNativeData(Constants.SERVICE_LIST);
        NatsMetricsReporter natsMetricsReporter =
                (NatsMetricsReporter) listener.getNativeData(Constants.NATS_METRIC_UTIL);
        BMap<BString, Object> subscriptionConfig = Utils
                .getSubscriptionConfig(service.getType().getAnnotation(
                        StringUtils.fromString(Constants.NATS_PACKAGE + ":" + Constants.SUBSCRIPTION_CONFIG)));
        if (subscriptionConfig == null) {
            return Utils.createNatsError(
                    "Error occurred while un-subscribing, Cannot find subscription configuration");
        }
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Dispatcher> dispatcherList = (ConcurrentHashMap<String, Dispatcher>)
                listener.getNativeData(Constants.DISPATCHER_LIST);
        String subject = subscriptionConfig.getStringValue(Constants.SUBJECT).getValue();
        Dispatcher dispatcher = dispatcherList.get(service.getType().getName());
        try {
            dispatcher.unsubscribe(subject);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return Utils.createNatsError("Error occurred while un-subscribing " + ex.getMessage());
        }
        console.println(Constants.NATS_CLIENT_UNSUBSCRIBED + subject);
        serviceList.remove(service);
        dispatcherList.remove(service.getType().getName());
        Connection natsConnection = (Connection) listener.getNativeData(Constants.NATS_CONNECTION);
        if (natsConnection != null) {
            natsMetricsReporter.reportUnsubscription(subject);
        }
        return null;
    }

    static {
        console = System.out;
    }
}
