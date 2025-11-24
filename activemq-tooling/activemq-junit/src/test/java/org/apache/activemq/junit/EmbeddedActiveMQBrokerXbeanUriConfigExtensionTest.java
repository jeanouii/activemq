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
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the xbean configuration URI is working properly
 */
@ExtendWith(EmbeddedActiveMQBroker.class)
public class EmbeddedActiveMQBrokerXbeanUriConfigExtensionTest {

    @Test
    public void testGetVmURL(EmbeddedActiveMQBroker broker) throws Exception {
        assertEquals("failover:(vm://embedded-broker?create=false)", broker.getVmURL(), "Default VM URL in incorrect");
    }

    @Test
    public void testGetBrokerName(EmbeddedActiveMQBroker broker) throws Exception {
        assertEquals("embedded-broker", broker.getBrokerName(), "Default Broker Name in incorrect");
    }

    @Test
    public void testBrokerNameConfig(EmbeddedActiveMQBroker broker) throws Exception {
        String dummyName = "test-broker-name";

        broker.setBrokerName(dummyName);

        assertEquals(dummyName, broker.getBrokerName(), "Broker Name not set correctly");
    }

    @Test
    public void testStatisticsPluginConfig(EmbeddedActiveMQBroker broker) throws Exception {
        assertFalse(broker.isStatisticsPluginEnabled(), "Statistics plugin should not be enabled by default");
        broker.enableStatisticsPlugin();
        assertTrue(broker.isStatisticsPluginEnabled(), "Statistics plugin not enabled");
        broker.disableStatisticsPlugin();
        assertFalse(broker.isStatisticsPluginEnabled(), "Statistics plugin not disabled");
    }

    @Test
    public void testAdvisoryForDeliveryConfig(EmbeddedActiveMQBroker broker) throws Exception {
        assertFalse(broker.isAdvisoryForDeliveryEnabled(), "Advisory messages for delivery should not be enabled by default");
        broker.enableAdvisoryForDelivery();
        assertTrue(broker.isAdvisoryForDeliveryEnabled(), "Advisory messages for delivery not enabled");
        broker.disableAdvisoryForDelivery();
        assertFalse(broker.isAdvisoryForDeliveryEnabled(), "Advisory messages for delivery not disabled");
    }

    @Test
    public void testAdvisoryForConsumedConfig(EmbeddedActiveMQBroker broker) throws Exception {
        assertFalse(broker.isAdvisoryForConsumedEnabled(), "Advisory messages for consumed should not be enabled by default");
        broker.enableAdvisoryForConsumed();
        assertTrue(broker.isAdvisoryForConsumedEnabled(), "Advisory messages for consumed not enabled");
        broker.disableAdvisoryForConsumed();
        assertFalse(broker.isAdvisoryForConsumedEnabled(), "Advisory messages for consumed not disabled");
    }

    @Test
    public void testAdvisoryForDiscardingMessagesConfig(EmbeddedActiveMQBroker broker) throws Exception {
        assertFalse(broker.isAdvisoryForDiscardingMessagesEnabled(), "Advisory messages for discarding messages should not be enabled by default");
        broker.enableAdvisoryForDiscardingMessages();
        assertTrue(broker.isAdvisoryForDiscardingMessagesEnabled(), "Advisory messages for discarding messages not enabled");
        broker.disableAdvisoryForDiscardingMessages();
        assertFalse(broker.isAdvisoryForDiscardingMessagesEnabled(), "Advisory messages for discarding messages not disabled");
    }

    @Test
    public void testAdvisoryForFastProducersConfig(EmbeddedActiveMQBroker broker) throws Exception {
        assertFalse(broker.isAdvisoryForFastProducersEnabled(), "Advisory messages for fast producers should not be enabled by default");
        broker.enableAdvisoryForFastProducers();
        assertTrue(broker.isAdvisoryForFastProducersEnabled(), "Advisory messages for fast producers not enabled");
        broker.disableAdvisoryForFastProducers();
        assertFalse(broker.isAdvisoryForFastProducersEnabled(), "Advisory messages for fast producers not disabled");
    }

    @Test
    public void testAdvisoryForSlowConsumersConfig(EmbeddedActiveMQBroker broker) throws Exception {
        assertFalse(broker.isAdvisoryForSlowConsumersEnabled(), "Advisory messages for slow consumers should not be enabled by default");
        broker.enableAdvisoryForSlowConsumers();
        assertTrue(broker.isAdvisoryForSlowConsumersEnabled(), "Advisory messages for slow consumers not enabled");
        broker.disableAdvisoryForSlowConsumers();
        assertFalse(broker.isAdvisoryForSlowConsumersEnabled(), "Advisory messages for slow consumers not disabled");
    }

}
