package com.intuit.billingcomm.billing.qbeshosting.service;

import com.intuit.billingcomm.billing.qbeshosting.exception.UnsupportedEventException;
import com.intuit.billingcomm.billing.qbeshosting.model.HostingSubscriptionEvent;
import org.springframework.messaging.Message;

public interface MessageParsingService {
    HostingSubscriptionEvent parseMessage(Message<String> message) throws UnsupportedEventException;
}
