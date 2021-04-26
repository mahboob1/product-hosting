package com.intuit.billingcomm.billing.qbeshosting.jms;

import com.intuit.billingcomm.billing.qbeshosting.exception.IncompleteEventException;
import com.intuit.billingcomm.billing.qbeshosting.exception.UnsupportedEventException;
import com.intuit.billingcomm.billing.qbeshosting.model.HostingSubscriptionEvent;
import com.intuit.billingcomm.billing.qbeshosting.service.MessageParsingService;
import com.intuit.billingcomm.billing.qbeshosting.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionEventListener.class);

    @Autowired
    private MessageParsingService messageParsingService;

    @Autowired
    private SubscriptionService subscriptionService;

    @JmsListener(destination = "${qbeshosting.jms.queue.name}", containerFactory = "jmsListenerContainerFactory")
    public void receive(Message<String> message) throws Exception {
        try {
            HostingSubscriptionEvent event = messageParsingService.parseMessage(message);
            subscriptionService.processEvent(event);
            LOGGER.info("Event processed successfully: {}", message.getPayload());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Message {} cannot be processed due to illegal argument error {}", message.getPayload(), e);
        } catch (IllegalStateException e) {
            LOGGER.error("Message {} cannot be processed due to illegal state error {}", message.getPayload(), e);
        }  catch (UnsupportedEventException e) {
            LOGGER.info("Ignoring unsupported event: {}", message.getPayload());
        }  catch (IncompleteEventException e) {
            LOGGER.info("Ignoring incomplete transaction event: {}", message.getPayload());
        } catch (Exception e) {
            LOGGER.error("Failed to process event {} due to error {}", message.getPayload(), e);
            throw e;
        }
    }
}
