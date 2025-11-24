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

import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class InternalClient {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;

    public InternalClient(final EmbeddedActiveMQBroker broker) {
        Objects.requireNonNull(broker, "EmbeddedActiveMQBroker cannot be null");
        ActiveMQConnectionFactory connectionFactory = broker.createConnectionFactory();
        try {
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            producer = session.createProducer(null);
            connection.start();
        } catch (JMSException jmsEx) {
            throw new EmbeddedActiveMQBroker.EmbeddedActiveMQBrokerException("Internal Client creation failure", jmsEx);
        }
    }

    void stop() {
        if (null != connection) {
            try {
                connection.close();
            } catch (JMSException jmsEx) {
                log.warn("JMSException encounter closing InternalClient connection - ignoring", jmsEx);
            }
        }
    }

    public BytesMessage createBytesMessage() {
        checkSession();

        try {
            return session.createBytesMessage();
        } catch (JMSException jmsEx) {
            throw new EmbeddedActiveMQBroker.EmbeddedActiveMQBrokerException("Failed to create BytesMessage", jmsEx);
        }
    }

    public TextMessage createTextMessage() {
        checkSession();

        try {
            return session.createTextMessage();
        } catch (JMSException jmsEx) {
            throw new EmbeddedActiveMQBroker.EmbeddedActiveMQBrokerException("Failed to create TextMessage", jmsEx);
        }
    }

    public MapMessage createMapMessage() {
        checkSession();

        try {
            return session.createMapMessage();
        } catch (JMSException jmsEx) {
            throw new EmbeddedActiveMQBroker.EmbeddedActiveMQBrokerException("Failed to create MapMessage", jmsEx);
        }
    }

    public ObjectMessage createObjectMessage() {
        checkSession();

        try {
            return session.createObjectMessage();
        } catch (JMSException jmsEx) {
            throw new EmbeddedActiveMQBroker.EmbeddedActiveMQBrokerException("Failed to create ObjectMessage", jmsEx);
        }
    }

    public StreamMessage createStreamMessage() {
        checkSession();
        try {
            return session.createStreamMessage();
        } catch (JMSException jmsEx) {
            throw new EmbeddedActiveMQBroker.EmbeddedActiveMQBrokerException("Failed to create StreamMessage", jmsEx);
        }
    }

    public void pushMessage(ActiveMQDestination destination, Message message) {
        if (producer == null) {
            throw new IllegalStateException("JMS MessageProducer is null - has the InternalClient been started?");
        }

        try {
            producer.send(destination, message);
        } catch (JMSException jmsEx) {
            throw new EmbeddedActiveMQBroker.EmbeddedActiveMQBrokerException(String.format("Failed to push %s to %s", message.getClass().getSimpleName(), destination.toString()), jmsEx);
        }
    }

    void checkSession() {
        if (session == null) {
            throw new IllegalStateException("JMS Session is null - has the InternalClient been started?");
        }
    }
}
