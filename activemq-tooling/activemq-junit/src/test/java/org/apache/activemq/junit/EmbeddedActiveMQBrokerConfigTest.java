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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the get/set operations are working properly
 */
public class EmbeddedActiveMQBrokerConfigTest {

    @Test
    public void testGetVmURL() throws Exception {
        EmbeddedActiveMQBroker instance = new EmbeddedActiveMQBroker();
        assertEquals("failover:(vm://embedded-broker?create=false)", instance.getVmURL(), "Default VM URL in incorrect");
    }

    @Test
    public void testGetBrokerName() throws Exception {
        EmbeddedActiveMQBroker instance = new EmbeddedActiveMQBroker();
        assertEquals("embedded-broker", instance.getBrokerName(), "Default Broker Name in incorrect");
    }

    @Test
    public void testBrokerNameConfig() throws Exception {
        String dummyName = "test-broker-name";

        EmbeddedActiveMQBroker instance = new EmbeddedActiveMQBrokerBuilder().withBrokerName(dummyName).build();

        assertEquals(dummyName, instance.getBrokerName(), "Broker Name not set correctly");
    }

    @Test
    public void testStatisticsPluginConfig() throws Exception {
        EmbeddedActiveMQBroker instance = new EmbeddedActiveMQBroker();
        assertFalse(instance.isStatisticsPluginEnabled(), "Statistics plugin should not be enabled by default");
        instance.enableStatisticsPlugin();
        assertTrue(instance.isStatisticsPluginEnabled(), "Statistics plugin not enabled");
        instance.disableStatisticsPlugin();
        assertFalse(instance.isStatisticsPluginEnabled(), "Statistics plugin not disabled");
    }

    @Test
    public void testAdvisoryForDeliveryConfig() throws Exception {
        EmbeddedActiveMQBroker instance = new EmbeddedActiveMQBroker();
        assertFalse(instance.isAdvisoryForDeliveryEnabled(), "Advisory messages for delivery should not be enabled by default");
        instance.enableAdvisoryForDelivery();
        assertTrue(instance.isAdvisoryForDeliveryEnabled(), "Advisory messages for delivery not enabled");
        instance.disableAdvisoryForDelivery();
        assertFalse(instance.isAdvisoryForDeliveryEnabled(), "Advisory messages for delivery not disabled");
    }

    @Test
    public void testAdvisoryForConsumedConfig() throws Exception {
        EmbeddedActiveMQBroker instance = new EmbeddedActiveMQBroker();
        assertFalse(instance.isAdvisoryForConsumedEnabled(), "Advisory messages for consumed should not be enabled by default");
        instance.enableAdvisoryForConsumed();
        assertTrue(instance.isAdvisoryForConsumedEnabled(), "Advisory messages for consumed not enabled");
        instance.disableAdvisoryForConsumed();
        assertFalse(instance.isAdvisoryForConsumedEnabled(), "Advisory messages for consumed not disabled");
    }

    @Test
    public void testAdvisoryForDiscardingMessagesConfig() throws Exception {
        EmbeddedActiveMQBroker instance = new EmbeddedActiveMQBroker();
        assertFalse(instance.isAdvisoryForDiscardingMessagesEnabled(), "Advisory messages for discarding messages should not be enabled by default");
        instance.enableAdvisoryForDiscardingMessages();
        assertTrue(instance.isAdvisoryForDiscardingMessagesEnabled(), "Advisory messages for discarding messages not enabled");
        instance.disableAdvisoryForDiscardingMessages();
        assertFalse(instance.isAdvisoryForDiscardingMessagesEnabled(), "Advisory messages for discarding messages not disabled");
    }

    @Test
    public void testAdvisoryForFastProducersConfig() throws Exception {
        EmbeddedActiveMQBroker instance = new EmbeddedActiveMQBroker();
        assertFalse(instance.isAdvisoryForFastProducersEnabled(), "Advisory messages for fast producers should not be enabled by default");
        instance.enableAdvisoryForFastProducers();
        assertTrue(instance.isAdvisoryForFastProducersEnabled(), "Advisory messages for fast producers not enabled");
        instance.disableAdvisoryForFastProducers();
        assertFalse(instance.isAdvisoryForFastProducersEnabled(), "Advisory messages for fast producers not disabled");
    }

    @Test
    public void testAdvisoryForSlowConsumersConfig() throws Exception {
        EmbeddedActiveMQBroker instance = new EmbeddedActiveMQBroker();
        assertFalse(instance.isAdvisoryForSlowConsumersEnabled(), "Advisory messages for slow consumers should not be enabled by default");
        instance.enableAdvisoryForSlowConsumers();
        assertTrue(instance.isAdvisoryForSlowConsumersEnabled(), "Advisory messages for slow consumers not enabled");
        instance.disableAdvisoryForSlowConsumers();
        assertFalse(instance.isAdvisoryForSlowConsumersEnabled(), "Advisory messages for slow consumers not disabled");
    }

}
