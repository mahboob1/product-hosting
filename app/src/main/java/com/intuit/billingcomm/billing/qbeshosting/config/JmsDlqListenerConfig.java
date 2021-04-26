package com.intuit.billingcomm.billing.qbeshosting.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

@Configuration
@ConfigurationProperties(prefix = "qbeshosting.jms.dlq.listener")
public class JmsDlqListenerConfig extends AbstractJmsListenerConfig {

    @Bean
    public DefaultJmsListenerContainerFactory jmsDlqListenerContainerFactory(
            ActiveMQConnectionFactory activeMQConnectionFactoryForDlq
    ) {
        return buildJmsListenerContainerFactory(activeMQConnectionFactoryForDlq);
    }

    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactoryForDlq() {
        return buildActiveMQConnectionFactory();
    }
}
