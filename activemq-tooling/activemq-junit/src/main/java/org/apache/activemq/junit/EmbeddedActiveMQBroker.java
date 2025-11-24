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
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.apache.activemq.command.ActiveMQDestination.QUEUE_TYPE;

/**
 * A JUnit Extension that embeds an ActiveMQ broker into a test.
 */
public class EmbeddedActiveMQBroker implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedActiveMQBroker.class);

    private final Supplier<BrokerService> brokerServiceFactory;
    private BrokerService brokerService;
    private InternalClient internalClient;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public EmbeddedActiveMQBroker() {
        this(() -> new EmbeddedActiveMQBrokerBuilder().buildBrokerService());
    }

    public EmbeddedActiveMQBroker(String configurationURI) {
        this(() -> createBrokerFromUri(configurationURI));
    }

    public EmbeddedActiveMQBroker(URI configurationURI) {
        this(() -> createBrokerFromUri(configurationURI));
    }

    public EmbeddedActiveMQBroker(BrokerService brokerService) {
        this(() -> cloneBrokerService(Objects.requireNonNull(brokerService, "BrokerService cannot be null")));
    }

    public EmbeddedActiveMQBroker(Supplier<BrokerService> brokerServiceFactory) {
        this.brokerServiceFactory = Objects.requireNonNull(brokerServiceFactory, "BrokerService factory cannot be null");
    }

    private BrokerService ensureBrokerService() {
        if (brokerService == null) {
            brokerService = brokerServiceFactory.get();
        }
        return brokerService;
    }

    private static BrokerService createBrokerFromUri(String configurationURI) {
        try {
            return BrokerFactory.createBroker(configurationURI);
        } catch (Exception ex) {
            throw new RuntimeException("Exception encountered creating embedded ActiveMQ broker from configuration URI: " + configurationURI, ex);
        }
    }

    private static BrokerService createBrokerFromUri(URI configurationURI) {
        try {
            return BrokerFactory.createBroker(configurationURI);
        } catch (Exception ex) {
            throw new RuntimeException("Exception encountered creating embedded ActiveMQ broker from configuration URI: " + configurationURI, ex);
        }
    }

    private static BrokerService cloneBrokerService(BrokerService template) {
        BrokerService clone = new BrokerService();
        clone.setBrokerName(template.getBrokerName());
        clone.setUseJmx(template.isUseJmx());
        clone.setPersistent(template.isPersistent());
        clone.setUseShutdownHook(template.isUseShutdownHook());
        clone.getManagementContext().setCreateConnector(template.getManagementContext().isCreateConnector());
        clone.setPlugins(template.getPlugins());
        return clone;
    }


    public static void setMessageProperties(Message message, Map<String, Object> properties) {
        if (properties != null && properties.size() > 0) {
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                try {
                    message.setObjectProperty(property.getKey(), property.getValue());
                } catch (JMSException jmsEx) {
                    throw new EmbeddedActiveMQBrokerException(String.format("Failed to set property {%s = %s}", property.getKey(), property.getValue().toString()), jmsEx);
                }
            }
        }
    }

    /**
     * Customize the configuration of the embedded ActiveMQ broker
     * <p>
     * This method is called before the embedded ActiveMQ broker is started, and can
     * be overridden to this method to customize the broker configuration.
     */
    protected void configure() {
    }

    /**
     * Start the embedded ActiveMQ broker, blocking until the broker has successfully started.
     * <p/>
     * The broker will normally be started by JUnit using the lifecycle callbacks. This method allows the broker to
     * be started manually to support advanced testing scenarios.
     */
    public void start() {
        if (started.getAndSet(true)) {
            LOG.debug("Embedded ActiveMQ broker: {} is already started.", getBrokerName());
            return;
        }

        LOG.info("Starting embedded ActiveMQ broker: {}", getBrokerName());
        try {
            BrokerService broker = ensureBrokerService();
            this.configure();
            broker.start();
            broker.waitUntilStarted();
            internalClient = new InternalClient(this);
        } catch (Exception ex) {
            started.set(false);
            LOG.error("Exception encountered starting embedded ActiveMQ broker: {}", this.getBrokerName(), ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Stop the embedded ActiveMQ broker, blocking until the broker has stopped.
     * <p/>
     * The broker will normally be stopped by JUnit using the lifecycle callbacks. This method allows the broker to
     * be stopped manually to support advanced testing scenarios.
     */
    public void stop() {
        if (!started.getAndSet(false)) {
            LOG.debug("Embedded ActiveMQ broker: {} is already stopped.", getBrokerName());
            return;
        }

        LOG.info("Stopping embedded ActiveMQ broker: {}", getBrokerName());
        if (internalClient != null) {
            internalClient.stop();
            internalClient = null;
        }
        if (brokerService != null && brokerService.isStarted()) {
            try {
                brokerService.stop();
                brokerService.waitUntilStopped();
            } catch (Exception ex) {
                LOG.warn("Exception encountered stopping embedded ActiveMQ broker: {}", this.getBrokerName(), ex);
            }
        }
        brokerService = null;
    }

    /**
     * Start the embedded ActiveMQ Broker
     * <p/>
     * Invoked by JUnit Jupiter to setup the resource
     */
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        this.start();
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(getBrokerName(), this);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        beforeEach(context);
    }

    /**
     * Stop the embedded ActiveMQ Broker
     * <p/>
     * Invoked by JUnit Jupiter to tear down the resource
     */
    @Override
    public void afterEach(ExtensionContext context) {
        this.stop();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        afterEach(context);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(EmbeddedActiveMQBroker.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return this;
    }

    /**
     * Create an ActiveMQConnectionFactory for the embedded ActiveMQ Broker
     *
     * @return a new ActiveMQConnectionFactory
     */
    public ActiveMQConnectionFactory createConnectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(getVmURL());
        return connectionFactory;
    }

    /**
     * Create an PooledConnectionFactory for the embedded ActiveMQ Broker
     *
     * @return a new PooledConnectionFactory
     */
    public PooledConnectionFactory createPooledConnectionFactory() {
        ActiveMQConnectionFactory connectionFactory = createConnectionFactory();

        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory(connectionFactory);

        return pooledConnectionFactory;
    }

    /**
     * Get the BrokerService for the embedded ActiveMQ broker.
     * <p/>
     * This may be required for advanced configuration of the BrokerService.
     *
     * @return the embedded ActiveMQ broker
     */
    public BrokerService getBrokerService() {
        return ensureBrokerService();
    }

    /**
     * Get the failover VM URL for the embedded ActiveMQ Broker
     * <p/>
     * NOTE:  The create=false option is appended to the URL to avoid the automatic creation of brokers
     * and the resulting duplicate broker errors
     *
     * @return the VM URL for the embedded broker
     */
    public String getVmURL() {
        return getVmURL(true);
    }

    /**
     * Get the VM URL for the embedded ActiveMQ Broker
     * <p/>
     * NOTE:  The create=false option is appended to the URL to avoid the automatic creation of brokers
     * and the resulting duplicate broker errors
     *
     * @param failoverURL if true a failover URL will be returned
     * @return the VM URL for the embedded broker
     */
    public String getVmURL(boolean failoverURL) {
        BrokerService current = ensureBrokerService();
        if (failoverURL) {
            return String.format("failover:(%s?create=false)", current.getVmConnectorURI().toString());
        }

        return current.getVmConnectorURI().toString() + "?create=false";
    }

    /**
     * Get the failover VM URI for the embedded ActiveMQ Broker
     * <p/>
     * NOTE:  The create=false option is appended to the URI to avoid the automatic creation of brokers
     * and the resulting duplicate broker errors
     *
     * @return the VM URI for the embedded broker
     */
    public URI getVmURI() {
        return getVmURI(true);
    }

    /**
     * Get the VM URI for the embedded ActiveMQ Broker
     * <p/>
     * NOTE:  The create=false option is appended to the URI to avoid the automatic creation of brokers
     * and the resulting duplicate broker errors
     *
     * @param failoverURI if true a failover URI will be returned
     * @return the VM URI for the embedded broker
     */
    public URI getVmURI(boolean failoverURI) {
        URI result;
        try {
            result = new URI(getVmURL(failoverURI));
        } catch (URISyntaxException uriEx) {
            throw new RuntimeException("Unable to create failover URI", uriEx);
        }

        return result;
    }

    public boolean isStarted() {
        return started.get() && brokerService != null && brokerService.isStarted();
    }

    /**
     * Get the name of the embedded ActiveMQ Broker
     *
     * @return name of the embedded broker
     */
    public String getBrokerName() {
        return ensureBrokerService().getBrokerName();
    }

    public void setBrokerName(String brokerName) {
        ensureBrokerService().setBrokerName(brokerName);
    }

    public boolean isStatisticsPluginEnabled() {
        BrokerPlugin[] plugins = ensureBrokerService().getPlugins();

        if (null != plugins) {
            for (BrokerPlugin plugin : plugins) {
                if (plugin instanceof StatisticsBrokerPlugin) {
                    return true;
                }
            }
        }

        return false;
    }

    public void enableStatisticsPlugin() {
        if (!isStatisticsPluginEnabled()) {
            BrokerPlugin[] newPlugins;
            BrokerPlugin[] currentPlugins = ensureBrokerService().getPlugins();
            if (null != currentPlugins && 0 < currentPlugins.length) {
                newPlugins = new BrokerPlugin[currentPlugins.length + 1];

                System.arraycopy(currentPlugins, 0, newPlugins, 0, currentPlugins.length);
            } else {
                newPlugins = new BrokerPlugin[1];
            }

            newPlugins[newPlugins.length - 1] = new StatisticsBrokerPlugin();

            ensureBrokerService().setPlugins(newPlugins);
        }
    }

    public void disableStatisticsPlugin() {
        if (isStatisticsPluginEnabled()) {
            BrokerPlugin[] currentPlugins = ensureBrokerService().getPlugins();
            if (1 < currentPlugins.length) {
                BrokerPlugin[] newPlugins = new BrokerPlugin[currentPlugins.length - 1];

                int i = 0;
                for (BrokerPlugin plugin : currentPlugins) {
                    if (!(plugin instanceof StatisticsBrokerPlugin)) {
                        newPlugins[i++] = plugin;
                    }
                }
                ensureBrokerService().setPlugins(newPlugins);
            } else {
                ensureBrokerService().setPlugins(null);
            }

        }
    }

    public boolean isAdvisoryForDeliveryEnabled() {
        return getDefaultPolicyEntry().isAdvisoryForDelivery();
    }

    public void enableAdvisoryForDelivery() {
        getDefaultPolicyEntry().setAdvisoryForDelivery(true);
    }

    public void disableAdvisoryForDelivery() {
        getDefaultPolicyEntry().setAdvisoryForDelivery(false);
    }

    public boolean isAdvisoryForConsumedEnabled() {
        return getDefaultPolicyEntry().isAdvisoryForConsumed();
    }

    public void enableAdvisoryForConsumed() {
        getDefaultPolicyEntry().setAdvisoryForConsumed(true);
    }

    public void disableAdvisoryForConsumed() {
        getDefaultPolicyEntry().setAdvisoryForConsumed(false);
    }

    public boolean isAdvisoryForDiscardingMessagesEnabled() {
        return getDefaultPolicyEntry().isAdvisoryForDiscardingMessages();
    }

    public void enableAdvisoryForDiscardingMessages() {
        getDefaultPolicyEntry().setAdvisoryForDiscardingMessages(true);
    }

    public void disableAdvisoryForDiscardingMessages() {
        getDefaultPolicyEntry().setAdvisoryForDiscardingMessages(false);
    }

    public boolean isAdvisoryForFastProducersEnabled() {
        return getDefaultPolicyEntry().isAdvisoryForFastProducers();
    }

    public void enableAdvisoryForFastProducers() {
        getDefaultPolicyEntry().setAdvisoryForFastProducers(true);
    }

    public void disableAdvisoryForFastProducers() {
        getDefaultPolicyEntry().setAdvisoryForFastProducers(false);
    }

    public boolean isAdvisoryForSlowConsumersEnabled() {
        return getDefaultPolicyEntry().isAdvisoryForSlowConsumers();
    }

    public void enableAdvisoryForSlowConsumers() {
        getDefaultPolicyEntry().setAdvisoryForSlowConsumers(true);
    }

    public void disableAdvisoryForSlowConsumers() {
        getDefaultPolicyEntry().setAdvisoryForSlowConsumers(false);
    }

    /**
     * Get the number of messages in a specific JMS Destination.
     * <p/>
     * The full name of the JMS destination including the prefix should be provided - i.e. queue://myQueue
     * or topic://myTopic.  If the destination type prefix is not included in the destination name, a prefix
     * of "queue://" is assumed.
     *
     * @param destinationName the full name of the JMS Destination
     * @return the number of messages in the JMS Destination
     */
    public long getMessageCount(String destinationName) {
        BrokerService current = ensureBrokerService();

        // TODO: Figure out how to do this for Topics
        Destination destination = getDestination(destinationName);
        if (destination == null) {
            throw new RuntimeException("Failed to find destination: " + destinationName);
        }

        // return destination.getMessageStore().getMessageCount();
        return destination.getDestinationStatistics().getMessages().getCount();
    }

    /**
     * Get the ActiveMQ destination
     * <p/>
     * The full name of the JMS destination including the prefix should be provided - i.e. queue://myQueue
     * or topic://myTopic.  If the destination type prefix is not included in the destination name, a prefix
     * of "queue://" is assumed.
     *
     * @param destinationName the full name of the JMS Destination
     * @return the ActiveMQ destination, null if not found
     */
    public Destination getDestination(String destinationName) {
        BrokerService current = ensureBrokerService();

        Destination destination = null;
        try {
            destination = current.getDestination(ActiveMQDestination.createDestination(destinationName, QUEUE_TYPE));
        } catch (RuntimeException runtimeEx) {
            throw runtimeEx;
        } catch (Exception ex) {
            throw new EmbeddedActiveMQBrokerException("Unexpected exception getting destination from broker", ex);
        }

        return destination;
    }

    private PolicyEntry getDefaultPolicyEntry() {
        BrokerService current = ensureBrokerService();
        PolicyMap destinationPolicy = current.getDestinationPolicy();
        if (null == destinationPolicy) {
            destinationPolicy = new PolicyMap();
            current.setDestinationPolicy(destinationPolicy);
        }

        PolicyEntry defaultEntry = destinationPolicy.getDefaultEntry();
        if (null == defaultEntry) {
            defaultEntry = new PolicyEntry();
            destinationPolicy.setDefaultEntry(defaultEntry);
        }

        return defaultEntry;
    }

    public BytesMessage createBytesMessage() {
        return internalClient.createBytesMessage();
    }

    public TextMessage createTextMessage() {
        return internalClient.createTextMessage();
    }

    public MapMessage createMapMessage() {
        return internalClient.createMapMessage();
    }

    public ObjectMessage createObjectMessage() {
        return internalClient.createObjectMessage();
    }

    public StreamMessage createStreamMessage() {
        return internalClient.createStreamMessage();
    }

    public BytesMessage createMessage(byte[] body) {
        return this.createMessage(body, null);
    }

    public TextMessage createMessage(String body) {
        return this.createMessage(body, null);
    }

    public MapMessage createMessage(Map<String, Object> body) {
        return this.createMessage(body, null);
    }

    public ObjectMessage createMessage(Serializable body) {
        return this.createMessage(body, null);
    }

    public BytesMessage createMessage(byte[] body, Map<String, Object> properties) {
        BytesMessage message = this.createBytesMessage();
        if (body != null) {
            try {
                message.writeBytes(body);
            } catch (JMSException jmsEx) {
                throw new EmbeddedActiveMQBrokerException(String.format("Failed to set body {%s} on BytesMessage", new String(body)), jmsEx);
            }
        }

        setMessageProperties(message, properties);

        return message;
    }

    public TextMessage createMessage(String body, Map<String, Object> properties) {
        TextMessage message = this.createTextMessage();
        if (body != null) {
            try {
                message.setText(body);
            } catch (JMSException jmsEx) {
                throw new EmbeddedActiveMQBrokerException(String.format("Failed to set body {%s} on TextMessage", body), jmsEx);
            }
        }

        setMessageProperties(message, properties);

        return message;
    }

    public MapMessage createMessage(Map<String, Object> body, Map<String, Object> properties) {
        MapMessage message = this.createMapMessage();

        if (body != null) {
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                try {
                    message.setObject(entry.getKey(), entry.getValue());
                } catch (JMSException jmsEx) {
                    throw new EmbeddedActiveMQBrokerException(String.format("Failed to set body entry {%s = %s} on MapMessage", entry.getKey(), entry.getValue().toString()), jmsEx);
                }
            }
        }

        setMessageProperties(message, properties);

        return message;
    }

    public ObjectMessage createMessage(Serializable body, Map<String, Object> properties) {
        ObjectMessage message = this.createObjectMessage();

        if (body != null) {
            try {
                message.setObject(body);
            } catch (JMSException jmsEx) {
                throw new EmbeddedActiveMQBrokerException(String.format("Failed to set body {%s} on ObjectMessage", body.toString()), jmsEx);
            }
        }

        setMessageProperties(message, properties);

        return message;
    }

    public void pushMessage(String destinationName, Message message) {
        if (destinationName == null) {
            throw new IllegalArgumentException("pushMessage failure - destination name is required");
        } else if (message == null) {
            throw new IllegalArgumentException("pushMessage failure - a Message is required");
        }
        ActiveMQDestination destination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);

        internalClient.pushMessage(destination, message);
    }

    public BytesMessage pushMessage(String destinationName, byte[] body) {
        BytesMessage message = createMessage(body, null);
        pushMessage(destinationName, message);
        return message;
    }

    public TextMessage pushMessage(String destinationName, String body) {
        TextMessage message = createMessage(body, null);
        pushMessage(destinationName, message);
        return message;
    }

    public MapMessage pushMessage(String destinationName, Map<String, Object> body) {
        MapMessage message = createMessage(body, null);
        pushMessage(destinationName, message);
        return message;
    }

    public ObjectMessage pushMessage(String destinationName, Serializable body) {
        ObjectMessage message = createMessage(body, null);
        pushMessage(destinationName, message);
        return message;
    }

    public BytesMessage pushMessageWithProperties(String destinationName, byte[] body, Map<String, Object> properties) {
        BytesMessage message = createMessage(body, properties);
        pushMessage(destinationName, message);
        return message;
    }

    public TextMessage pushMessageWithProperties(String destinationName, String body, Map<String, Object> properties) {
        TextMessage message = createMessage(body, properties);
        pushMessage(destinationName, message);
        return message;
    }

    public MapMessage pushMessageWithProperties(String destinationName, Map<String, Object> body, Map<String, Object> properties) {
        MapMessage message = createMessage(body, properties);
        pushMessage(destinationName, message);
        return message;
    }

    public ObjectMessage pushMessageWithProperties(String destinationName, Serializable body, Map<String, Object> properties) {
        ObjectMessage message = createMessage(body, properties);
        pushMessage(destinationName, message);
        return message;
    }


    public Message peekMessage(String destinationName) {
        BrokerService current = ensureBrokerService();

        if (destinationName == null) {
            throw new IllegalArgumentException("peekMessage failure - destination name is required");
        }

        ActiveMQDestination destination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        Destination brokerDestination = null;

        try {
            brokerDestination = current.getDestination(destination);
        } catch (Exception ex) {
            throw new EmbeddedActiveMQBrokerException("peekMessage failure - unexpected exception getting destination from BrokerService", ex);
        }

        if (brokerDestination == null) {
            throw new IllegalStateException(String.format("peekMessage failure - destination %s not found in broker %s", destination.toString(), current.getBrokerName()));
        }

        org.apache.activemq.command.Message[] messages = brokerDestination.browse();
        if (messages != null && messages.length > 0) {
            return (Message) messages[0];
        }

        return null;
    }

    public BytesMessage peekBytesMessage(String destinationName) {
        return (BytesMessage) peekMessage(destinationName);
    }

    public TextMessage peekTextMessage(String destinationName) {
        return (TextMessage) peekMessage(destinationName);
    }

    public MapMessage peekMapMessage(String destinationName) {
        return (MapMessage) peekMessage(destinationName);
    }

    public ObjectMessage peekObjectMessage(String destinationName) {
        return (ObjectMessage) peekMessage(destinationName);
    }

    public StreamMessage peekStreamMessage(String destinationName) {
        return (StreamMessage) peekMessage(destinationName);
    }

    public static class EmbeddedActiveMQBrokerException extends RuntimeException {
        public EmbeddedActiveMQBrokerException(String message) {
            super(message);
        }

        public EmbeddedActiveMQBrokerException(String message, Exception cause) {
            super(message, cause);
        }
    }

    public InternalClient getInternalClient() {
        return internalClient;
    }
}
