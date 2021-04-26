package com.intuit.billingcomm.billing.qbeshosting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;

@EnableJms
@Configuration
@ConfigurationProperties(prefix = "qbeshosting.config")
@Getter
@Setter
public class QbesHostingConfig {
    private String appId;
    private String appSecret;
    private String jmsBrokerEndpoint;
}
