package com.intuit.billingcomm.billing.qbeshosting.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

@Getter
@Setter
public abstract class AbstractJmsListenerConfig {

    @Autowired
    protected QbesHostingConfig qbesHostingConfig;

    protected Integer maxMessagesPerTask;
    protected String concurrency;
    protected Boolean sessionTransacted;
    protected long initialRedeliveryDelay;
    protected long redeliveryDelay;
    protected int maxRedeliveries;
    protected boolean useExponentialBackoff;
    protected double backOffMultiplier;
    protected int queuePrefetch;

    protected DefaultJmsListenerContainerFactory buildJmsListenerContainerFactory(
            ActiveMQConnectionFactory activeMQConnectionFactory
    ) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(activeMQConnectionFactory);
        factory.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
        factory.setMaxMessagesPerTask(maxMessagesPerTask);
        factory.setConcurrency(concurrency);
        factory.setSessionTransacted(sessionTransacted);
        return factory;
    }

    protected ActiveMQConnectionFactory buildActiveMQConnectionFactory() {
        ActiveMQConnectionFactory activeMQ = new ActiveMQConnectionFactory(
                qbesHostingConfig.getAppId(), qbesHostingConfig.getAppSecret(), qbesHostingConfig.getJmsBrokerEndpoint()
        );
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setInitialRedeliveryDelay(initialRedeliveryDelay);
        redeliveryPolicy.setRedeliveryDelay(redeliveryDelay);
        redeliveryPolicy.setMaximumRedeliveries(maxRedeliveries);
        redeliveryPolicy.setUseExponentialBackOff(useExponentialBackoff);
        redeliveryPolicy.setBackOffMultiplier(backOffMultiplier);
        activeMQ.setRedeliveryPolicy(redeliveryPolicy);
        ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
        prefetchPolicy.setQueuePrefetch(queuePrefetch);
        activeMQ.setPrefetchPolicy(prefetchPolicy);
        return activeMQ;
    }
}
