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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class ActiveMQTestExtensionTest {

    private static final String TIMEOUT_PROPERTY = "org.apache.activemq.junit.testTimeoutMultiplier";

    @BeforeAll
    static void enableTimeoutMultiplier() {
        System.setProperty(TIMEOUT_PROPERTY, "2");
    }

    @AfterAll
    static void clearTimeoutMultiplier() {
        System.clearProperty(TIMEOUT_PROPERTY);
    }

    @Test
    @ActiveMQTimeout(value = 50, unit = TimeUnit.MILLISECONDS)
    void timeoutIsAdjustedByMultiplier() throws Exception {
        // With the multiplier disabled this sleep would exceed the configured timeout.
        Thread.sleep(75);
    }
}
