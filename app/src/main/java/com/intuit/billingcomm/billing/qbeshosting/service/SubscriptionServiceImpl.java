package com.intuit.billingcomm.billing.qbeshosting.service;

import com.intuit.billingcomm.billing.qbescommon.model.CustomerContactInfo;
import com.intuit.billingcomm.billing.qbescommon.service.CustomerContactService;
import com.intuit.billingcomm.billing.qbescommon.service.HostingFeatureService;
import com.intuit.billingcomm.billing.qbescommon.service.ProductLicenseService;
import com.intuit.billingcomm.billing.qbescommon.service.QbesHostingRequestService;
import com.intuit.billingcomm.billing.qbeshosting.enums.SubscriptionEventTypeEnum;
import com.intuit.billingcomm.billing.qbeshosting.exception.IncompleteEventException;
import com.intuit.billingcomm.billing.qbeshosting.model.HostingSubscriptionEvent;
import com.intuit.billingcomm.billing.qbespfts.enums.HostingRequestType;
import com.intuit.billingcomm.billing.qbespfts.model.HostingRequestFile;
import com.intuit.billingcomm.billing.qbespfts.model.HostingRequestFile.*;
import com.intuit.billingcomm.billing.qbespfts.service.QbesPftsService;
import com.intuit.platform.webs.subscription.data.facade.enums.EntitlementStateChangeReasonEnum;
import com.intuit.platform.webs.subscription.data.facade.enums.EntitlementStateEnum;
import com.intuit.platform.webs.subscription.internal.model.entity.EntitledProductFeatureEntity;
import com.intuit.platform.webs.subscription.internal.model.entity.EntitledProductLicenseEntity;
import com.intuit.platform.webs.subscription.internal.model.entity.QbesHostingRequestStagingEntity;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {
	private static final String REQ_FILE_DATE_FORMAT = "yyyy-MM-dd";
	private static final String TIMEZONE_ID = "America/Edmonton";
	private static final String RESUB_ABORT_REASON = "Aborted due to re-subscription with same license/eoc";
	private static final String TRANSFER_ABORT_REASON = "Aborted in favor of a transfer";
	private static final String REINSTATE_ABORT_REASON = "Aborted due to reinstating";
	
	private final Logger LOGGER = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    @Autowired
    private QbesHostingRequestService qbesHostingRequestService;

    @Autowired
    private HostingFeatureService hostingFeatureService;
    
    @Autowired
	private CustomerContactService customerContactService;
	
	@Autowired
	private QbesPftsService qbesPftsService;
	
	@Autowired
	ProductLicenseService productLicenseService;

    @Override
    public void processEvent(HostingSubscriptionEvent event) throws Exception {
		HostingEventProcessingResult processingResult = processHostingEvent(event);
		if (processingResult == null) {
			LOGGER.info("No request to submit or abort for hosting event: {}", event);
		} else {
			if (processingResult.getRequestToSubmit() != null) {
				Row row = createHostingRequestRow(processingResult.getRequestToSubmit(), event);
				HostingRequestFile hostingRequest = new HostingRequestFile();
				List<Row> rows = new ArrayList<>();
				rows.add(row);
				hostingRequest.setRows(rows);
				qbesPftsService.submitIrpHostingRequest(hostingRequest);
				qbesHostingRequestService
						.markSubmitted(processingResult.getRequestToSubmit().getQbesHostingRequestStagingId());
			}
			if (processingResult.getRequestToAbort() != null) {
				qbesHostingRequestService.markAborted(
						processingResult.getRequestToAbort().getQbesHostingRequestStagingId(),
						processingResult.getAbortReason()
				);
			}
		}
    }

    private HostingEventProcessingResult processHostingEvent(HostingSubscriptionEvent event) {
		EntitledProductFeatureEntity hostingFeature =
				hostingFeatureService.findHostingFeatureById(event.getHostingFeatureId());
		if (event.isConversion() || isRpsPotentialMatch(event, hostingFeature)) {
			hostingFeatureService.skipProcessing(event.getHostingFeatureId());
			return null;
		}
		if (!event.isCompleted()) {
			throw new IncompleteEventException();
		}
    	if (hostingFeature.getEntitledFeatureState().equals(EntitlementStateEnum.ACTIVE.name())) {
			QbesHostingRequestStagingEntity pendingCancellation =
					qbesHostingRequestService.findUnsubmittedCancellationBySubscriptionId(event.getSubscriptionId());
    		if (pendingCancellation != null) {
				HostingEventProcessingResult result = new HostingEventProcessingResult();
				result.setRequestToAbort(pendingCancellation);
    			if (event.getLicense().equals(pendingCancellation.getLicense()) &&
						event.getEoc().equals(pendingCancellation.getEoc())) {
    				result.setAbortReason(RESUB_ABORT_REASON);
				} else {
					result.setRequestToSubmit(initiateHostingReq(event, HostingRequestType.TRANSFERRED.name(), hostingFeature));
					result.setAbortReason(TRANSFER_ABORT_REASON);
				}
				return result;
			}
			QbesHostingRequestStagingEntity pendingDelinquentReq =
					qbesHostingRequestService.findUnsubmittedDelinquentReqBySubscriptionId(event.getSubscriptionId());
			if (pendingDelinquentReq != null) {
				HostingEventProcessingResult result = new HostingEventProcessingResult();
				result.setRequestToAbort(pendingDelinquentReq);
				result.setAbortReason(REINSTATE_ABORT_REASON);
				if (!event.getEoc().equals(pendingDelinquentReq.getEoc()) ||
						!event.getLicense().equals(pendingDelinquentReq.getLicense())) {
					result.setRequestToSubmit(initiateHostingReq(event, HostingRequestType.TRANSFERRED.name(), hostingFeature));
				}
				return result;
			}
			QbesHostingRequestStagingEntity mostRecentReq =
					qbesHostingRequestService.findMostRecentSubmittedReqBySubscriptionId(event.getSubscriptionId());
			if (mostRecentReq != null && HostingRequestType.DELINQUENT.name().equals(mostRecentReq.getActivityType())) {
				HostingEventProcessingResult result = new HostingEventProcessingResult();
				result.setRequestToSubmit(initiateHostingReq(event, HostingRequestType.REINSTATED.name(), hostingFeature));
				return result;
			}
			if (mostRecentReq == null || !HostingRequestType.NEW.name().equals(mostRecentReq.getActivityType())) {
				HostingEventProcessingResult result = new HostingEventProcessingResult();
				result.setRequestToSubmit(initiateHostingReq(event, HostingRequestType.NEW.name(), hostingFeature));
				return result;
			}
			return null;
		}
    	if (event.getChangeType().equals(SubscriptionEventTypeEnum.UPDATE) &&
				hostingFeature.getEntitledFeatureState().equals(EntitlementStateEnum.INACTIVE.name()) &&
				Arrays.asList(
						EntitlementStateChangeReasonEnum.PAYMENT_FAILURE.getId(),
						EntitlementStateChangeReasonEnum.PAYMENT_FAILURE_GRACE_EXPIRED.getId(),
						EntitlementStateChangeReasonEnum.CHARGEBACK.getId()
				).contains(hostingFeature.getEntitlestateChangeReasonId())) {
			initiateHostingReq(event, HostingRequestType.DELINQUENT.name(), hostingFeature);
			return null;
		}
		if (event.getChangeType().equals(SubscriptionEventTypeEnum.UPDATE) &&
				Arrays.asList(
						EntitlementStateEnum.INACTIVE.name(),
						EntitlementStateEnum.CLOSED.name()
				).contains(hostingFeature.getEntitledFeatureState())) {
			initiateHostingReq(event, HostingRequestType.CANCEL.name(), hostingFeature);
			return null;
		}
    	return null;
	}

	private boolean isRpsPotentialMatch(HostingSubscriptionEvent event, EntitledProductFeatureEntity hostingFeature) {
    	return event.isOnHold() &&
				event.getChangeType().equals(SubscriptionEventTypeEnum.CREATE) &&
				hostingFeature.getEntitledFeatureState().equals(EntitlementStateEnum.INACTIVE.name());
	}

	private QbesHostingRequestStagingEntity initiateHostingReq(HostingSubscriptionEvent event, String activityType, EntitledProductFeatureEntity hostingFeature) {
		QbesHostingRequestStagingEntity hostingRequestStagingEntity = new QbesHostingRequestStagingEntity();
		hostingRequestStagingEntity.setActivityType(activityType);
		hostingRequestStagingEntity.setCustomerId(event.getAccountId());
		hostingRequestStagingEntity.setSubscriptionId(event.getSubscriptionId());
		if (activityType.equals(HostingRequestType.CANCEL.name()) &&
				EntitlementStateChangeReasonEnum.SUBSUME_OPTIONAL_FEATURE.getId().equals(hostingFeature.getEntitlestateChangeReasonId())) {
			EntitledProductLicenseEntity entitledProductLicenseEntity = productLicenseService.findByEntitledProductIdAndStatusOrderByFulfillmentDateDesc(event.getProductId(), EntitlementStateEnum.INACTIVE.name());
			hostingRequestStagingEntity.setLicense(entitledProductLicenseEntity.getId().getLicense());
			hostingRequestStagingEntity.setEoc(entitledProductLicenseEntity.getId().getEoc());
		} else {
			hostingRequestStagingEntity.setLicense(event.getLicense());
			hostingRequestStagingEntity.setEoc(event.getEoc());
		}
		return qbesHostingRequestService.initiateRequestEntity(hostingRequestStagingEntity, event.getHostingFeatureId());
	}
    
    private Row createHostingRequestRow(QbesHostingRequestStagingEntity requestStagingEntity,
										HostingSubscriptionEvent event) throws Exception {
		Row row = new Row();
		Transaction transaction = new Transaction();
		Subscription subscription = new Subscription();
		Product product = new Product();
		Customer customer = new Customer();
		
		Future<CustomerContactInfo> customerContactInfoFuture = customerContactService.fetchCustomerContactInfoAsync(event.getAccountId());
		transaction.setId(requestStagingEntity.getQbesHostingRequestStagingId().toString());
		transaction.setActivityType(requestStagingEntity.getActivityType());
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of(TIMEZONE_ID));
		String formattedDateStr = now.format(DateTimeFormatter.ofPattern(REQ_FILE_DATE_FORMAT));
		transaction.setEffectiveDate(formattedDateStr);
		transaction.setCaptureTime(formattedDateStr);
		row.setTransaction(transaction);
		// create subscription info
		subscription.setSubscriptionId(event.getSubscriptionId().toString());
		subscription.setNumberOfUsers(event.getNumberOfUsers());
		row.setSubscription(subscription);
		// create product info
		product.setLicense(event.getLicense());
		product.setEoc(event.getEoc());
		row.setProduct(product);
		// set customer info
		customer.setId(event.getAccountId().toString());
		CustomerContactInfo customerContactInfo = customerContactInfoFuture.get();
		setCustomerContactInfo(customer, customerContactInfo);			
		row.setCustomer(customer);
		return row;
	}
		
	private void setCustomerContactInfo(Customer customer, CustomerContactInfo customerContactInfo) {
		customer.setCompanyName(customerContactInfo.getCompanyName());
		customer.setFirstName(StringUtils.capitalize(customerContactInfo.getFirstName()));
		customer.setLastName(StringUtils.capitalize(customerContactInfo.getLastName()));
		customer.setCity(customerContactInfo.getCity());
		customer.setProvince(customerContactInfo.getProvince());
		customer.setPostalCode(customerContactInfo.getPostalCode());
		customer.setPhone(customerContactInfo.getPhone());
		customer.setEmail(customerContactInfo.getEmail());
		customer.setOldId(customerContactInfo.getLegacyId());
	}

	@Getter
	@Setter
	private static class HostingEventProcessingResult {
    	private QbesHostingRequestStagingEntity requestToSubmit;
    	private QbesHostingRequestStagingEntity requestToAbort;
    	private String abortReason;
	}
}
