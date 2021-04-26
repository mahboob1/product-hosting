package com.intuit.billingcomm.billing.qbeshosting.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class DlqListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DlqListener.class);

    @JmsListener(destination = "${qbeshosting.jms.dlq.name}", containerFactory = "jmsDlqListenerContainerFactory")
    public void receive(Message<String> message) {
        LOGGER.error("DLQ message: {}", message.getPayload());
    }
}
