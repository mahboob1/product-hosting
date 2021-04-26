package com.intuit.billingcomm.billing.qbeshosting.service;

import com.intuit.billingcomm.billing.qbescommon.config.QbesCommonConfig;
import com.intuit.billingcomm.billing.qbescommon.service.HostingFeatureService;
import com.intuit.billingcomm.billing.qbescommon.service.ProductLicenseService;
import com.intuit.billingcomm.billing.qbeshosting.TestHelpers;
import com.intuit.billingcomm.billing.qbeshosting.exception.UnsupportedEventException;
import com.intuit.billingcomm.billing.qbeshosting.model.HostingSubscriptionEvent;
import com.intuit.platform.common.exception.v2.NotFoundException;
import com.intuit.platform.webs.subscription.internal.model.entity.EntitledProductFeatureEntity;
import com.intuit.platform.webs.subscription.internal.model.entity.EntitledProductLicenseEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.Message;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class MessageParsingServiceImplTest {

    @InjectMocks
    private MessageParsingServiceImpl messageParsingService;

    @Mock
    private QbesCommonConfig qbesCommonConfig;

    @Mock
    private ProductLicenseService productLicenseService;

    @Mock
    private HostingFeatureService hostingFeatureService;

    @Test
    public void parseMessage_ValidMessage_Success() throws IOException {
        Mockito.when(qbesCommonConfig.getQbesHostingFeatureCodes())
                .thenReturn(Collections.singletonList(TestHelpers.QBES_HOSTING_FEATURE_CODE));
        Message<String> message = TestHelpers.generateValidSubscriptionMessage();
        HostingSubscriptionEvent subscriptionEvent = messageParsingService.parseMessage(message);
        assertNotNull(subscriptionEvent);
    }

    @Test(expected = UnsupportedEventException.class)
    public void parseMessage_UnsupportedMessage_ExceptionThrown() throws IOException {
        Message<String> message = TestHelpers.generateUnsupportedMessage();
        messageParsingService.parseMessage(message);
    }

    @Test(expected = UnsupportedEventException.class)
    public void parseMessage_UnsupportedMessage_Billing_ExceptionThrown() throws IOException {
        Message<String> message = TestHelpers.generateUnsupportedBillingMessage();
        messageParsingService.parseMessage(message);
    }

    @Test
    public void parseMessage_NoLicInEvent() throws IOException {
        EntitledProductLicenseEntity testLic = TestHelpers.generateEntitledProductLicense();
        Mockito.when(qbesCommonConfig.getQbesHostingFeatureCodes())
                .thenReturn(Collections.singletonList(TestHelpers.QBES_HOSTING_FEATURE_CODE));
        Mockito.when(productLicenseService.findCurrentBaseLicenseByProductId(Mockito.any())).thenReturn(testLic);
        Message<String> message = TestHelpers.generateValidSubscriptionMessageWithoutLicense();
        HostingSubscriptionEvent subscriptionEvent = messageParsingService.parseMessage(message);
        assertEquals(subscriptionEvent.getLicense(), testLic.getId().getLicense());
        assertEquals(subscriptionEvent.getEoc(), testLic.getId().getEoc());
    }

    @Test
    public void parseMessage_NoFeatureCodeInEvent() throws IOException {
        EntitledProductFeatureEntity testFeature = TestHelpers.generateHostingFeature();
        Mockito.when(qbesCommonConfig.getQbesHostingFeatureCodes())
                .thenReturn(Collections.singletonList(testFeature.getFeatureCode()));
        Mockito.when(hostingFeatureService.findHostingFeatureById(Mockito.any())).thenReturn(testFeature);
        Message<String> message = TestHelpers.generateValidSubscriptionMessageWithoutFeatureCode();
        HostingSubscriptionEvent subscriptionEvent = messageParsingService.parseMessage(message);
        assertEquals(5, subscriptionEvent.getNumberOfUsers());
    }

    @Test(expected = UnsupportedEventException.class)
    public void parseMessage_NoFeatureCodeInEvent_Unsupported() throws IOException {
        Mockito.when(hostingFeatureService.findHostingFeatureById(Mockito.any())).thenThrow(NotFoundException.class);
        Message<String> message = TestHelpers.generateValidSubscriptionMessageWithoutFeatureCode();
        messageParsingService.parseMessage(message);
    }
}
