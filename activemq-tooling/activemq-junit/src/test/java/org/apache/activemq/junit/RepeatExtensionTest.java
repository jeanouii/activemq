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

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepeatExtensionTest {

    private static final AtomicInteger INVOCATION_COUNTER = new AtomicInteger();

    @AfterAll
    static void verifyRepetitions() {
        assertEquals(3, INVOCATION_COUNTER.get(), "Repeat should run the test three times");
    }

    @Repeat(repetitions = 3)
    void repeatsConfiguredNumberOfTimes() {
        INVOCATION_COUNTER.incrementAndGet();
    }
}
