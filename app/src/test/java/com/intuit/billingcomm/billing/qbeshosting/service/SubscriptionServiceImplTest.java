// TODO: uncomment when SubscriptionServiceImpl is implemented

package com.intuit.billingcomm.billing.qbeshosting.service;

import com.intuit.billingcomm.billing.qbescommon.model.CustomerContactInfo;
import com.intuit.billingcomm.billing.qbescommon.service.CustomerContactService;
import com.intuit.billingcomm.billing.qbescommon.service.HostingFeatureService;
import com.intuit.billingcomm.billing.qbescommon.service.ProductLicenseService;
import com.intuit.billingcomm.billing.qbescommon.service.QbesHostingRequestService;
import com.intuit.billingcomm.billing.qbeshosting.TestHelpers;
import com.intuit.billingcomm.billing.qbeshosting.enums.SubscriptionEventTypeEnum;
import com.intuit.billingcomm.billing.qbeshosting.exception.IncompleteEventException;
import com.intuit.billingcomm.billing.qbeshosting.model.HostingSubscriptionEvent;
import com.intuit.billingcomm.billing.qbespfts.enums.HostingRequestType;
import com.intuit.billingcomm.billing.qbespfts.model.HostingRequestFile;
import com.intuit.billingcomm.billing.qbespfts.service.QbesPftsService;
import com.intuit.platform.webs.subscription.data.facade.enums.EntitlementStateChangeReasonEnum;
import com.intuit.platform.webs.subscription.internal.model.entity.EntitledProductFeatureEntity;
import com.intuit.platform.webs.subscription.internal.model.entity.EntitledProductLicenseEntity;
import com.intuit.platform.webs.subscription.internal.model.entity.QbesHostingRequestStagingEntity;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionServiceImplTest {

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;
    
    @Mock
    private QbesHostingRequestService qbesHostingRequestService;

    @Mock
    private HostingFeatureService hostingFeatureService;
    
    @Mock
	private CustomerContactService customerContactService;
	
    @Mock
    private QbesPftsService qbesPftsService;
    
    @Mock
    ProductLicenseService productLicenseService;

    @Test
    public void processEvent_NewSignup() throws Exception {
	    BigInteger reqId = BigInteger.valueOf(100);
	    ArgumentCaptor<HostingRequestFile> reqFileCaptor = ArgumentCaptor.forClass(HostingRequestFile.class);
        ArgumentCaptor<BigInteger> reqIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
	    HostingSubscriptionEvent hostingEvent = TestHelpers.generateSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateHostingFeature();
        CustomerContactInfo custInfo = TestHelpers.generateCustomerContactInfo();
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);
        when(customerContactService.fetchCustomerContactInfoAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(custInfo));
        when(qbesHostingRequestService.initiateRequestEntity(any(), any())).thenAnswer(i -> {
            QbesHostingRequestStagingEntity req = (QbesHostingRequestStagingEntity) i.getArguments()[0];
            req.setQbesHostingRequestStagingId(reqId);
            return req;
        });

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, times(1)).submitIrpHostingRequest(reqFileCaptor.capture());
        HostingRequestFile.Row row = reqFileCaptor.getValue().getRows().get(0);
        assertEquals(HostingRequestType.NEW.name(), row.getTransaction().getActivityType());
        assertEquals(reqId.toString(), row.getTransaction().getId());
        assertNotNull(row.getTransaction().getCaptureTime());
        assertNotNull(row.getTransaction().getEffectiveDate());
        assertEquals(hostingEvent.getAccountId().toString(), row.getCustomer().getId());
        assertEquals(custInfo.getCompanyName(), row.getCustomer().getCompanyName());
        assertNotEquals(custInfo.getLastName(), row.getCustomer().getLastName());
        assertEquals(StringUtils.capitalize(custInfo.getLastName()), row.getCustomer().getLastName());
        assertNotEquals(custInfo.getFirstName(), row.getCustomer().getFirstName());
        assertEquals(StringUtils.capitalize(custInfo.getFirstName()), row.getCustomer().getFirstName());
        assertEquals(custInfo.getCity(), row.getCustomer().getCity());
        assertEquals(custInfo.getProvince(), row.getCustomer().getProvince());
        assertEquals(custInfo.getPostalCode(), row.getCustomer().getPostalCode());
        assertEquals(custInfo.getPhone(), row.getCustomer().getPhone());
        assertEquals(custInfo.getEmail(), row.getCustomer().getEmail());
        assertEquals(custInfo.getLegacyId(), row.getCustomer().getOldId());
        assertEquals(hostingEvent.getSubscriptionId().toString(), row.getSubscription().getSubscriptionId());
        assertEquals(hostingEvent.getNumberOfUsers(), row.getSubscription().getNumberOfUsers());
        assertEquals(hostingEvent.getLicense(), row.getProduct().getLicense());
        assertEquals(hostingEvent.getEoc(), row.getProduct().getEoc());
        verify(qbesHostingRequestService, times(1)).markSubmitted(reqIdCaptor.capture());
        assertEquals(reqId, reqIdCaptor.getValue());
    }

    @Test
    public void processEvent_NewSignup_AlreadySubmitted() throws Exception {
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateHostingFeature();
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);
        when(qbesHostingRequestService.findMostRecentSubmittedReqBySubscriptionId(any()))
                .thenReturn(new QbesHostingRequestStagingEntity() {{
                    setActivityType(HostingRequestType.NEW.name());
                }});

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, never()).submitIrpHostingRequest(any());
        verify(qbesHostingRequestService, never()).markSubmitted(any());
        verify(qbesHostingRequestService, never()).markAborted(any(), any());
        verify(qbesHostingRequestService, never()).initiateRequestEntity(any(), any());
    }

    @Test
    public void processEvent_Transfer() throws Exception {
        BigInteger reqId = BigInteger.valueOf(100);
        BigInteger cancelReqId = BigInteger.valueOf(200);
        ArgumentCaptor<HostingRequestFile> reqFileCaptor = ArgumentCaptor.forClass(HostingRequestFile.class);
        ArgumentCaptor<BigInteger> reqIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        ArgumentCaptor<BigInteger> cancelReqIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateHostingFeature();
        CustomerContactInfo custInfo = TestHelpers.generateCustomerContactInfo();
        when(qbesHostingRequestService.findUnsubmittedCancellationBySubscriptionId(any()))
                .thenReturn(new QbesHostingRequestStagingEntity() {{ setQbesHostingRequestStagingId(cancelReqId); }});
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);
        when(customerContactService.fetchCustomerContactInfoAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(custInfo));
        when(qbesHostingRequestService.initiateRequestEntity(any(), any())).thenAnswer(i -> {
            QbesHostingRequestStagingEntity req = (QbesHostingRequestStagingEntity) i.getArguments()[0];
            req.setQbesHostingRequestStagingId(reqId);
            return req;
        });

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, times(1)).submitIrpHostingRequest(reqFileCaptor.capture());
        HostingRequestFile.Row row = reqFileCaptor.getValue().getRows().get(0);
        assertEquals(HostingRequestType.TRANSFERRED.name(), row.getTransaction().getActivityType());
        assertEquals(reqId.toString(), row.getTransaction().getId());
        assertNotNull(row.getTransaction().getCaptureTime());
        assertNotNull(row.getTransaction().getEffectiveDate());
        assertEquals(hostingEvent.getAccountId().toString(), row.getCustomer().getId());
        assertEquals(custInfo.getCompanyName(), row.getCustomer().getCompanyName());
        assertNotEquals(custInfo.getLastName(), row.getCustomer().getLastName());
        assertEquals(StringUtils.capitalize(custInfo.getLastName()), row.getCustomer().getLastName());
        assertNotEquals(custInfo.getFirstName(), row.getCustomer().getFirstName());
        assertEquals(StringUtils.capitalize(custInfo.getFirstName()), row.getCustomer().getFirstName());
        assertEquals(custInfo.getCity(), row.getCustomer().getCity());
        assertEquals(custInfo.getProvince(), row.getCustomer().getProvince());
        assertEquals(custInfo.getPostalCode(), row.getCustomer().getPostalCode());
        assertEquals(custInfo.getPhone(), row.getCustomer().getPhone());
        assertEquals(custInfo.getEmail(), row.getCustomer().getEmail());
        assertEquals(hostingEvent.getSubscriptionId().toString(), row.getSubscription().getSubscriptionId());
        assertEquals(hostingEvent.getNumberOfUsers(), row.getSubscription().getNumberOfUsers());
        assertEquals(hostingEvent.getLicense(), row.getProduct().getLicense());
        assertEquals(hostingEvent.getEoc(), row.getProduct().getEoc());
        verify(qbesHostingRequestService, times(1)).markSubmitted(reqIdCaptor.capture());
        assertEquals(reqId, reqIdCaptor.getValue());
        verify(qbesHostingRequestService, times(1)).markAborted(cancelReqIdCaptor.capture(), any());
        assertEquals(cancelReqId, cancelReqIdCaptor.getValue());
    }

    @Test
    public void processEvent_ResubBeforeCancelSubmission() throws Exception {
        BigInteger cancelReqId = BigInteger.valueOf(200);
        ArgumentCaptor<BigInteger> cancelReqIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateSubscriptionEvent();
        when(qbesHostingRequestService.findUnsubmittedCancellationBySubscriptionId(any()))
                .thenReturn(new QbesHostingRequestStagingEntity() {{
                    setQbesHostingRequestStagingId(cancelReqId);
                    setLicense(hostingEvent.getLicense());
                    setEoc(hostingEvent.getEoc());
                }});
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(TestHelpers.generateHostingFeature());

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, never()).submitIrpHostingRequest(any());
        verify(qbesHostingRequestService, never()).markSubmitted(any());
        verify(qbesHostingRequestService, times(1)).markAborted(cancelReqIdCaptor.capture(), any());
        assertEquals(cancelReqId, cancelReqIdCaptor.getValue());
    }

    @Test
    public void processEvent_ReinstateBeforeDelinquentSubmission() throws Exception {
        BigInteger delinquentReqId = BigInteger.valueOf(200);
        ArgumentCaptor<BigInteger> cancelReqIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateHostingFeature();
        when(qbesHostingRequestService.findUnsubmittedDelinquentReqBySubscriptionId(any()))
                .thenReturn(new QbesHostingRequestStagingEntity() {{
                    setQbesHostingRequestStagingId(delinquentReqId);
                    setLicense(hostingEvent.getLicense());
                    setEoc(hostingEvent.getEoc());
                }});
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, never()).submitIrpHostingRequest(any());
        verify(qbesHostingRequestService, never()).initiateRequestEntity(any(), any());
        verify(qbesHostingRequestService, never()).markSubmitted(any());
        verify(qbesHostingRequestService, times(1)).markAborted(cancelReqIdCaptor.capture(), any());
        assertEquals(delinquentReqId, cancelReqIdCaptor.getValue());
    }

    @Test
    public void processEvent_ReinstateBeforeDelinquentSubmission_WithEocChange() throws Exception {
        BigInteger delinquentReqId = BigInteger.valueOf(200);
        BigInteger transferReqId = BigInteger.valueOf(100);
        ArgumentCaptor<BigInteger> delinquentReqIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        ArgumentCaptor<BigInteger> transferReqIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        ArgumentCaptor<HostingRequestFile> transferReqFileCaptor = ArgumentCaptor.forClass(HostingRequestFile.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateHostingFeature();
        CustomerContactInfo custInfo = TestHelpers.generateCustomerContactInfo();
        when(qbesHostingRequestService.findUnsubmittedDelinquentReqBySubscriptionId(any()))
                .thenReturn(new QbesHostingRequestStagingEntity() {{ setQbesHostingRequestStagingId(delinquentReqId); }});
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);
        when(customerContactService.fetchCustomerContactInfoAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(custInfo));
        when(qbesHostingRequestService.initiateRequestEntity(any(), any())).thenAnswer(i -> {
            QbesHostingRequestStagingEntity req = (QbesHostingRequestStagingEntity) i.getArguments()[0];
            req.setQbesHostingRequestStagingId(transferReqId);
            return req;
        });

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, times(1)).submitIrpHostingRequest(transferReqFileCaptor.capture());
        HostingRequestFile.Row row = transferReqFileCaptor.getValue().getRows().get(0);
        assertEquals(HostingRequestType.TRANSFERRED.name(), row.getTransaction().getActivityType());
        assertEquals(transferReqId.toString(), row.getTransaction().getId());
        assertNotNull(row.getTransaction().getCaptureTime());
        assertNotNull(row.getTransaction().getEffectiveDate());
        assertEquals(hostingEvent.getAccountId().toString(), row.getCustomer().getId());
        assertEquals(custInfo.getCompanyName(), row.getCustomer().getCompanyName());
        assertNotEquals(custInfo.getLastName(), row.getCustomer().getLastName());
        assertEquals(StringUtils.capitalize(custInfo.getLastName()), row.getCustomer().getLastName());
        assertNotEquals(custInfo.getFirstName(), row.getCustomer().getFirstName());
        assertEquals(StringUtils.capitalize(custInfo.getFirstName()), row.getCustomer().getFirstName());
        assertEquals(custInfo.getCity(), row.getCustomer().getCity());
        assertEquals(custInfo.getProvince(), row.getCustomer().getProvince());
        assertEquals(custInfo.getPostalCode(), row.getCustomer().getPostalCode());
        assertEquals(custInfo.getPhone(), row.getCustomer().getPhone());
        assertEquals(custInfo.getEmail(), row.getCustomer().getEmail());
        assertEquals(hostingEvent.getSubscriptionId().toString(), row.getSubscription().getSubscriptionId());
        assertEquals(hostingEvent.getNumberOfUsers(), row.getSubscription().getNumberOfUsers());
        assertEquals(hostingEvent.getLicense(), row.getProduct().getLicense());
        assertEquals(hostingEvent.getEoc(), row.getProduct().getEoc());
        verify(qbesHostingRequestService, times(1)).markSubmitted(transferReqIdCaptor.capture());
        assertEquals(transferReqId, transferReqIdCaptor.getValue());
        verify(qbesHostingRequestService, times(1)).markAborted(delinquentReqIdCaptor.capture(), any());
        assertEquals(delinquentReqId, delinquentReqIdCaptor.getValue());
    }

    @Test
    public void processEvent_ReinstateAfterDelinquentSubmission() throws Exception {
        BigInteger delinquentReqId = BigInteger.valueOf(200);
        BigInteger reinstateReqId = BigInteger.valueOf(100);
        ArgumentCaptor<HostingRequestFile> reqFileCaptor = ArgumentCaptor.forClass(HostingRequestFile.class);
        ArgumentCaptor<BigInteger> reqIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateHostingFeature();
        CustomerContactInfo custInfo = TestHelpers.generateCustomerContactInfo();
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);
        when(customerContactService.fetchCustomerContactInfoAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(custInfo));
        when(qbesHostingRequestService.findMostRecentSubmittedReqBySubscriptionId(any()))
                .thenReturn(new QbesHostingRequestStagingEntity() {{
                    setQbesHostingRequestStagingId(delinquentReqId);
                    setActivityType(HostingRequestType.DELINQUENT.name());
                }});
        when(qbesHostingRequestService.initiateRequestEntity(any(), any())).thenAnswer(i -> {
            QbesHostingRequestStagingEntity req = (QbesHostingRequestStagingEntity) i.getArguments()[0];
            req.setQbesHostingRequestStagingId(reinstateReqId);
            return req;
        });

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, times(1)).submitIrpHostingRequest(reqFileCaptor.capture());
        HostingRequestFile.Row row = reqFileCaptor.getValue().getRows().get(0);
        assertEquals(HostingRequestType.REINSTATED.name(), row.getTransaction().getActivityType());
        assertEquals(reinstateReqId.toString(), row.getTransaction().getId());
        assertNotNull(row.getTransaction().getCaptureTime());
        assertNotNull(row.getTransaction().getEffectiveDate());
        assertEquals(hostingEvent.getAccountId().toString(), row.getCustomer().getId());
        assertEquals(custInfo.getCompanyName(), row.getCustomer().getCompanyName());
        assertNotEquals(custInfo.getLastName(), row.getCustomer().getLastName());
        assertEquals(StringUtils.capitalize(custInfo.getLastName()), row.getCustomer().getLastName());
        assertNotEquals(custInfo.getFirstName(), row.getCustomer().getFirstName());
        assertEquals(StringUtils.capitalize(custInfo.getFirstName()), row.getCustomer().getFirstName());
        assertEquals(custInfo.getCity(), row.getCustomer().getCity());
        assertEquals(custInfo.getProvince(), row.getCustomer().getProvince());
        assertEquals(custInfo.getPostalCode(), row.getCustomer().getPostalCode());
        assertEquals(custInfo.getPhone(), row.getCustomer().getPhone());
        assertEquals(custInfo.getEmail(), row.getCustomer().getEmail());
        assertEquals(custInfo.getLegacyId(), row.getCustomer().getOldId());
        assertEquals(hostingEvent.getSubscriptionId().toString(), row.getSubscription().getSubscriptionId());
        assertEquals(hostingEvent.getNumberOfUsers(), row.getSubscription().getNumberOfUsers());
        assertEquals(hostingEvent.getLicense(), row.getProduct().getLicense());
        assertEquals(hostingEvent.getEoc(), row.getProduct().getEoc());
        verify(qbesHostingRequestService, times(1)).markSubmitted(reqIdCaptor.capture());
        assertEquals(reinstateReqId, reqIdCaptor.getValue());
    }

    @Test
    public void processEvent_Delinquent_PaymentFailure() throws Exception {
        ArgumentCaptor<QbesHostingRequestStagingEntity> reqCaptor =
                ArgumentCaptor.forClass(QbesHostingRequestStagingEntity.class);
        ArgumentCaptor<BigInteger> featureIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateUpdateSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateInactiveHostingFeature();
        hostingFeature
                .setEntitlestateChangeReasonId(EntitlementStateChangeReasonEnum.PAYMENT_FAILURE.getId());
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, never()).submitIrpHostingRequest(any());
        verify(qbesHostingRequestService, never()).markSubmitted(any());
        verify(qbesHostingRequestService, never()).markAborted(any(), any());
        verify(qbesHostingRequestService, times(1))
                .initiateRequestEntity(reqCaptor.capture(), featureIdCaptor.capture());
        assertEquals(HostingRequestType.DELINQUENT.name(), reqCaptor.getValue().getActivityType());
        assertEquals(hostingEvent.getAccountId(), reqCaptor.getValue().getCustomerId());
        assertEquals(hostingEvent.getSubscriptionId(), reqCaptor.getValue().getSubscriptionId());
        assertEquals(hostingEvent.getLicense(), reqCaptor.getValue().getLicense());
        assertEquals(hostingEvent.getEoc(), reqCaptor.getValue().getEoc());
        assertEquals(hostingEvent.getHostingFeatureId(), featureIdCaptor.getValue());
    }

    @Test
    public void processEvent_Delinquent_PaymentFailureGraceExpired() throws Exception {
        ArgumentCaptor<QbesHostingRequestStagingEntity> reqCaptor =
                ArgumentCaptor.forClass(QbesHostingRequestStagingEntity.class);
        ArgumentCaptor<BigInteger> featureIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateUpdateSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateInactiveHostingFeature();
        hostingFeature
                .setEntitlestateChangeReasonId(EntitlementStateChangeReasonEnum.PAYMENT_FAILURE_GRACE_EXPIRED.getId());
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, never()).submitIrpHostingRequest(any());
        verify(qbesHostingRequestService, never()).markSubmitted(any());
        verify(qbesHostingRequestService, never()).markAborted(any(), any());
        verify(qbesHostingRequestService, times(1))
                .initiateRequestEntity(reqCaptor.capture(), featureIdCaptor.capture());
        assertEquals(HostingRequestType.DELINQUENT.name(), reqCaptor.getValue().getActivityType());
        assertEquals(hostingEvent.getAccountId(), reqCaptor.getValue().getCustomerId());
        assertEquals(hostingEvent.getSubscriptionId(), reqCaptor.getValue().getSubscriptionId());
        assertEquals(hostingEvent.getLicense(), reqCaptor.getValue().getLicense());
        assertEquals(hostingEvent.getEoc(), reqCaptor.getValue().getEoc());
        assertEquals(hostingEvent.getHostingFeatureId(), featureIdCaptor.getValue());
    }

    @Test
    public void processEvent_Delinquent_Chargeback() throws Exception {
        ArgumentCaptor<QbesHostingRequestStagingEntity> reqCaptor =
                ArgumentCaptor.forClass(QbesHostingRequestStagingEntity.class);
        ArgumentCaptor<BigInteger> featureIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateUpdateSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateInactiveHostingFeature();
        hostingFeature
                .setEntitlestateChangeReasonId(EntitlementStateChangeReasonEnum.CHARGEBACK.getId());
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, never()).submitIrpHostingRequest(any());
        verify(qbesHostingRequestService, never()).markSubmitted(any());
        verify(qbesHostingRequestService, never()).markAborted(any(), any());
        verify(qbesHostingRequestService, times(1))
                .initiateRequestEntity(reqCaptor.capture(), featureIdCaptor.capture());
        assertEquals(HostingRequestType.DELINQUENT.name(), reqCaptor.getValue().getActivityType());
        assertEquals(hostingEvent.getAccountId(), reqCaptor.getValue().getCustomerId());
        assertEquals(hostingEvent.getSubscriptionId(), reqCaptor.getValue().getSubscriptionId());
        assertEquals(hostingEvent.getLicense(), reqCaptor.getValue().getLicense());
        assertEquals(hostingEvent.getEoc(), reqCaptor.getValue().getEoc());
        assertEquals(hostingEvent.getHostingFeatureId(), featureIdCaptor.getValue());
    }

    @Test
    public void processEvent_Cancel() throws Exception {
        ArgumentCaptor<QbesHostingRequestStagingEntity> reqCaptor =
                ArgumentCaptor.forClass(QbesHostingRequestStagingEntity.class);
        ArgumentCaptor<BigInteger> featureIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateUpdateSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateInactiveHostingFeature();
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, never()).submitIrpHostingRequest(any());
        verify(qbesHostingRequestService, never()).markSubmitted(any());
        verify(qbesHostingRequestService, never()).markAborted(any(), any());
        verify(qbesHostingRequestService, times(1))
                .initiateRequestEntity(reqCaptor.capture(), featureIdCaptor.capture());
        assertEquals(HostingRequestType.CANCEL.name(), reqCaptor.getValue().getActivityType());
        assertEquals(hostingEvent.getAccountId(), reqCaptor.getValue().getCustomerId());
        assertEquals(hostingEvent.getSubscriptionId(), reqCaptor.getValue().getSubscriptionId());
        assertEquals(hostingEvent.getLicense(), reqCaptor.getValue().getLicense());
        assertEquals(hostingEvent.getEoc(), reqCaptor.getValue().getEoc());
        assertEquals(hostingEvent.getHostingFeatureId(), featureIdCaptor.getValue());
    }

    @Test
    public void processEvent_Cancel_Subsume() throws Exception {
        ArgumentCaptor<QbesHostingRequestStagingEntity> reqCaptor =
                ArgumentCaptor.forClass(QbesHostingRequestStagingEntity.class);
        ArgumentCaptor<BigInteger> featureIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateUpdateSubscriptionEvent();
        EntitledProductLicenseEntity entitledProductLicenseEntity = TestHelpers.generateEntitledProductLicense();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateInactiveHostingFeature();
        hostingFeature.setEntitlestateChangeReasonId(EntitlementStateChangeReasonEnum.SUBSUME_OPTIONAL_FEATURE.getId());
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);
        when(productLicenseService.findByEntitledProductIdAndStatusOrderByFulfillmentDateDesc(any(), any())).thenReturn(entitledProductLicenseEntity);

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, never()).submitIrpHostingRequest(any());
        verify(qbesHostingRequestService, never()).markSubmitted(any());
        verify(qbesHostingRequestService, never()).markAborted(any(), any());
        verify(qbesHostingRequestService, times(1))
                .initiateRequestEntity(reqCaptor.capture(), featureIdCaptor.capture());
        assertEquals(HostingRequestType.CANCEL.name(), reqCaptor.getValue().getActivityType());
        assertEquals(hostingEvent.getAccountId(), reqCaptor.getValue().getCustomerId());
        assertEquals(hostingEvent.getSubscriptionId(), reqCaptor.getValue().getSubscriptionId());
        assertNotEquals(hostingEvent.getLicense(), reqCaptor.getValue().getLicense());
        assertEquals(entitledProductLicenseEntity.getId().getLicense(), reqCaptor.getValue().getLicense());
        assertNotEquals(hostingEvent.getEoc(), reqCaptor.getValue().getEoc());
        assertEquals(entitledProductLicenseEntity.getId().getEoc(), reqCaptor.getValue().getEoc());
        assertEquals(hostingEvent.getHostingFeatureId(), featureIdCaptor.getValue());
    }

    @Test
    public void processEvent_Conversion() throws Exception {
        ArgumentCaptor<BigInteger> featureIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateConversionSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateHostingFeature();
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, never()).submitIrpHostingRequest(any());
        verify(qbesHostingRequestService, never()).markSubmitted(any());
        verify(qbesHostingRequestService, never()).markAborted(any(), any());
        verify(qbesHostingRequestService, never()).initiateRequestEntity(any(), any());
        verify(hostingFeatureService, times(1)).skipProcessing(featureIdCaptor.capture());
        assertEquals(hostingEvent.getHostingFeatureId(), featureIdCaptor.getValue());
    }

    @Test
    public void processEvent_PotentialRpsMatch() throws Exception {
        ArgumentCaptor<BigInteger> featureIdCaptor = ArgumentCaptor.forClass(BigInteger.class);
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateOnHoldSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateInactiveHostingFeature();
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);

        subscriptionService.processEvent(hostingEvent);

        verify(qbesPftsService, never()).submitIrpHostingRequest(any());
        verify(qbesHostingRequestService, never()).markSubmitted(any());
        verify(qbesHostingRequestService, never()).markAborted(any(), any());
        verify(qbesHostingRequestService, never()).initiateRequestEntity(any(), any());
        verify(hostingFeatureService, times(1)).skipProcessing(featureIdCaptor.capture());
        assertEquals(hostingEvent.getHostingFeatureId(), featureIdCaptor.getValue());
    }

    @Test(expected = IncompleteEventException.class)
    public void processEvent_Incomplete() throws Exception {
        HostingSubscriptionEvent hostingEvent = TestHelpers.generateIncompleteSubscriptionEvent();
        EntitledProductFeatureEntity hostingFeature = TestHelpers.generateInactiveHostingFeature();
        when(hostingFeatureService.findHostingFeatureById(any())).thenReturn(hostingFeature);

        subscriptionService.processEvent(hostingEvent);
    }
}
