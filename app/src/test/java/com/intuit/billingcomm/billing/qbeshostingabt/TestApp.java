package com.intuit.billingcomm.billing.qbeshostingabt;

import com.intuit.billingcomm.billing.qbescommon.config.DataConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.intuit.billingcomm.billing.qbeshostingabt.config",
        "com.intuit.billingcomm.billing.qbeshosting.config",
        "com.intuit.billingcomm.billing.qbeshosting.service",
        "com.intuit.billingcomm.billing.qbeshosting.jms",
        "com.intuit.billingcomm.billing.qbescommon",
        "com.intuit.billingcomm.billing.qbespfts"
},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DataConfig.class)
)
public class TestApp {}
