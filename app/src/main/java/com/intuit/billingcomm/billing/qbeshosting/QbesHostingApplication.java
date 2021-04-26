package com.intuit.billingcomm.billing.qbeshosting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({
        "com.intuit.billingcomm.billing.qbeshosting",
        "com.intuit.billingcomm.billing.qbescommon",
        "com.intuit.billingcomm.billing.qbespfts",
        "com.intuit.platform.webs.conversion.model.config"
})
public class QbesHostingApplication
{
    public static void main(String[] args) {
        SpringApplication.run(QbesHostingApplication.class, args);
    }
}
