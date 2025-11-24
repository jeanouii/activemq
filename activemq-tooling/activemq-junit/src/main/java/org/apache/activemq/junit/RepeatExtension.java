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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class RepeatExtension implements TestTemplateInvocationContextProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RepeatExtension.class);

    @Override
    public boolean supportsTestTemplate(final ExtensionContext context) {
        return AnnotationSupport.isAnnotated(context.getRequiredTestMethod(), Repeat.class);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(final ExtensionContext context) {
        final Repeat repeat = AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), Repeat.class)
            .orElseThrow(() -> new IllegalStateException("@Repeat not found"));

        final int repetitions = Math.max(1, repeat.repetitions());
        final boolean untilFailure = repeat.untilFailure();

        return IntStream.range(0, repetitions)
            .mapToObj(i -> new RepeatInvocationContext(i + 1, repetitions, untilFailure));
    }

    private static final class RepeatInvocationContext implements TestTemplateInvocationContext {
        private final int iteration;
        private final int total;
        private final boolean untilFailure;

        RepeatInvocationContext(final int iteration, final int total, final boolean untilFailure) {
            this.iteration = iteration;
            this.total = total;
            this.untilFailure = untilFailure;
        }

        @Override
        public String getDisplayName(final int invocationIndex) {
            if (untilFailure) {
                return "iteration " + iteration + " (untilFailure)";
            }
            return "iteration " + iteration + " of " + total;
        }

        @Override
        public List<org.junit.jupiter.api.extension.Extension> getAdditionalExtensions() {
            return Collections.singletonList(new TestExecutionExceptionHandler() {
                @Override
                public void handleTestExecutionException(final ExtensionContext context, final Throwable throwable) throws Throwable {
                    LOG.warn("Iteration {} failed", iteration, throwable);
                    if (untilFailure || iteration == total) {
                        throw throwable;
                    }
                }
            });
        }
    }
}
