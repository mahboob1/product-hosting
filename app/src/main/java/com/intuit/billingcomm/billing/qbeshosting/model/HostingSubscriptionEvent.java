package com.intuit.billingcomm.billing.qbeshosting.model;

import com.intuit.billingcomm.billing.qbeshosting.enums.SubscriptionEventTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
public class HostingSubscriptionEvent {
    private BigInteger accountId;
    private BigInteger subscriptionId;
    private BigInteger productId;
    private String license;
    private String eoc;
    private BigInteger hostingFeatureId;
    private SubscriptionEventTypeEnum changeType;
    private boolean isCompleted;
    private boolean isOnHold;
    private boolean isConversion;
    private int numberOfUsers;
}
