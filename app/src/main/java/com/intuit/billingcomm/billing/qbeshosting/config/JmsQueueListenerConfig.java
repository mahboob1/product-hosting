package com.intuit.billingcomm.billing.qbeshosting.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

@Configuration
@ConfigurationProperties(prefix = "qbeshosting.jms.queue.listener")
public class JmsQueueListenerConfig extends AbstractJmsListenerConfig {

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ActiveMQConnectionFactory activeMQConnectionFactory
    ) {
        return buildJmsListenerContainerFactory(activeMQConnectionFactory);
    }

    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
        return buildActiveMQConnectionFactory();
    }
}
