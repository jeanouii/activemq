/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.junit;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

public class EmbeddedActiveMQBrokerBuilder {

    private String brokerName = "embedded-broker";
    private boolean useJmx = true;
    private boolean persistent = false;
    private String configurationUrl;

    public EmbeddedActiveMQBrokerBuilder() {
    }

    public EmbeddedActiveMQBrokerBuilder withBrokerName(String brokerName) {
        this.brokerName = brokerName;
        return this;
    }

    public EmbeddedActiveMQBrokerBuilder withUseJmx(boolean useJmx) {
        this.useJmx = useJmx;
        return this;
    }

    public EmbeddedActiveMQBrokerBuilder withPersistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    public EmbeddedActiveMQBrokerBuilder withConfigurationUrl(String configurationUrl) {
        this.configurationUrl = configurationUrl;
        return this;
    }

    public BrokerService buildBrokerService() {
        if (configurationUrl != null) {
            try {
                return BrokerFactory.createBroker(configurationUrl);
            } catch (Exception ex) {
                throw new RuntimeException("Exception encountered creating embedded ActiveMQ broker from configuration URI: " + configurationUrl, ex);
            }
        }
        BrokerService brokerService = new BrokerService();
        brokerService.setBrokerName(brokerName);
        brokerService.setUseJmx(useJmx);
        brokerService.setPersistent(persistent);
        brokerService.getManagementContext().setCreateConnector(false);
        brokerService.setUseShutdownHook(false);

        return brokerService;
    }

    public EmbeddedActiveMQBroker build() {
        return new EmbeddedActiveMQBroker(this::buildBrokerService);
    }
}
