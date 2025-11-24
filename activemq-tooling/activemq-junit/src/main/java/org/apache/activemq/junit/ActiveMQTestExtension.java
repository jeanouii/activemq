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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

/**
 * Jupiter {@link InvocationInterceptor} that applies {@link ActiveMQTimeout} semantics to test methods.
 */
public final class ActiveMQTestExtension implements InvocationInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveMQTestExtension.class);
    private static final String TIMEOUT_MULTIPLIER_PROPERTY = "org.apache.activemq.junit.testTimeoutMultiplier";

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {

        Optional<ActiveMQTimeout> timeout = AnnotationSupport.findAnnotation(invocationContext.getExecutable(), ActiveMQTimeout.class);
        if (!timeout.isPresent()) {
            timeout = AnnotationSupport.findAnnotation(extensionContext.getRequiredTestClass(), ActiveMQTimeout.class);
        }

        if (!timeout.isPresent()) {
            invocation.proceed();
            return;
        }

        Duration duration = Duration.ofMillis(timeout.get().unit().toMillis(timeout.get().value()));
        Duration effective = applyMultiplier(duration);

        Assertions.assertTimeoutPreemptively(effective, () -> {
            invocation.proceed();
            return null;
        });
    }

    private Duration applyMultiplier(Duration original) {
        String multiplierString = System.getProperty(TIMEOUT_MULTIPLIER_PROPERTY);
        if (multiplierString == null) {
            return original;
        }

        double multiplier = 0.0;
        try {
            multiplier = Double.parseDouble(multiplierString);
        } catch (NumberFormatException nfe) {
            LOG.warn("Ignoring testTimeoutMultiplier not set to a valid value: {}", multiplierString);
            return original;
        }

        if (multiplier <= 0.0) {
            return original;
        }

        long nanos = (long) (original.toNanos() * multiplier);
        LOG.info("Test timeout multiple {} applied to test timeout {}ms: new timeout = {}ms",
            multiplier, original.toMillis(), Duration.ofNanos(nanos).toMillis());

        return Duration.ofNanos(nanos);
    }
}
