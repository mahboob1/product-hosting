package com.intuit.billingcomm.billing.qbeshosting;

import com.intuit.billingcomm.billing.qbescommon.model.CustomerContactInfo;
import com.intuit.billingcomm.billing.qbeshosting.enums.SubscriptionEventTypeEnum;
import com.intuit.billingcomm.billing.qbeshosting.model.HostingSubscriptionEvent;
import com.intuit.platform.webs.subscription.data.facade.enums.EntitlementStateEnum;
import com.intuit.platform.webs.subscription.internal.model.entity.EntitledProductFeatureEntity;
import com.intuit.platform.webs.subscription.internal.model.entity.EntitledProductLicenseEntity;
import com.intuit.platform.webs.subscription.internal.model.entity.EntitledProductLicensePK;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;

public class TestHelpers {

    public static final String QBES_HOSTING_FEATURE_CODE = "HOST_5_USER";

    public static Message<String> generateValidSubscriptionMessage() throws IOException {
        return MessageBuilder
                .withPayload(loadStringResource("payloads/signupWithFeature2.xml"))
                .build();
    }

    public static Message<String> generateValidSubscriptionMessageWithoutLicense() throws IOException {
        return MessageBuilder
                .withPayload(loadStringResource("payloads/updateFeatureNoLic.xml"))
                .build();
    }

    public static Message<String> generateValidSubscriptionMessageWithoutFeatureCode() throws IOException {
        return MessageBuilder
                .withPayload(loadStringResource("payloads/signupWithFeatureNoCode.xml"))
                .build();
    }

    public static Message<String> generateUnsupportedMessage() throws IOException {
        return MessageBuilder
                .withPayload(loadStringResource("payloads/signupBaseOnly1.xml"))
                .build();
    }

    public static Message<String> generateUnsupportedBillingMessage() throws IOException {
        return MessageBuilder
                .withPayload(loadStringResource("payloads/billing.xml"))
                .build();
    }

    public static EntitledProductLicenseEntity generateEntitledProductLicense() {
        return new EntitledProductLicenseEntity() {{
            setId(new EntitledProductLicensePK() {{
                setLicense("license");
                setEoc("eoc");
            }});
        }};
    }

    public static HostingSubscriptionEvent generateSubscriptionEvent() {
        return new HostingSubscriptionEvent() {{
        	setAccountId(BigInteger.ONE);
        	setSubscriptionId(BigInteger.TEN);
        	setProductId(BigInteger.TEN);
        	setLicense("License1");
        	setEoc("Eoc1");
        	setHostingFeatureId(BigInteger.ONE);
        	setChangeType(SubscriptionEventTypeEnum.CREATE);
        	setCompleted(true);
        	setConversion(false);
        	setNumberOfUsers(5);
        }};
    }

    public static HostingSubscriptionEvent generateConversionSubscriptionEvent() {
        HostingSubscriptionEvent event = generateSubscriptionEvent();
        event.setConversion(true);
        return event;
    }

    public static HostingSubscriptionEvent generateIncompleteSubscriptionEvent() {
        HostingSubscriptionEvent event = generateSubscriptionEvent();
        event.setCompleted(false);
        return event;
    }

    public static HostingSubscriptionEvent generateOnHoldSubscriptionEvent() {
        HostingSubscriptionEvent event = generateSubscriptionEvent();
        event.setOnHold(true);
        return event;
    }

    public static HostingSubscriptionEvent generateUpdateSubscriptionEvent() {
        HostingSubscriptionEvent event = generateSubscriptionEvent();
        event.setChangeType(SubscriptionEventTypeEnum.UPDATE);
        return event;
    }

    public static EntitledProductFeatureEntity generateHostingFeature() {
        return new EntitledProductFeatureEntity() {{
            setEntitledFeatureState(EntitlementStateEnum.ACTIVE.name());
            setFeatureCode(QBES_HOSTING_FEATURE_CODE);
        }};
    }

    public static EntitledProductFeatureEntity generateInactiveHostingFeature() {
        return new EntitledProductFeatureEntity() {{
            setEntitledFeatureState(EntitlementStateEnum.INACTIVE.name());
        }};
    }

    public static String loadStringResource(String resourcePath) throws IOException {
        return new String(
                Files.readAllBytes(ResourceUtils.getFile("classpath:" + resourcePath).toPath())
        );
    }
    
    public static CustomerContactInfo generateCustomerContactInfo() {
    	return new CustomerContactInfo() {{
    		setCompanyName("Company1");
    		setFirstName("firstName1");
    		setLastName("lastName1");
    		setCity("City1");
    		setProvince("Province1");
    		setPostalCode("PostalCode1");
    		setPhone("getPhone1");
    		setEmail("Email1");
    		setLegacyId("oldId");
    	}};
    	
    }
}
