package com.intuit.billingcomm.billing.qbeshosting.service;

import com.intuit.billingcomm.billing.qbeshosting.model.HostingSubscriptionEvent;

public interface SubscriptionService {
    void processEvent(HostingSubscriptionEvent event) throws Exception;
}
