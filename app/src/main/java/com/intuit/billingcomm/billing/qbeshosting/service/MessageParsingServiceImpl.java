package com.intuit.billingcomm.billing.qbeshosting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.billingcomm.billing.qbescommon.config.QbesCommonConfig;
import com.intuit.billingcomm.billing.qbescommon.service.HostingFeatureService;
import com.intuit.billingcomm.billing.qbescommon.service.ProductLicenseService;
import com.intuit.billingcomm.billing.qbeshosting.enums.SubscriptionEventTypeEnum;
import com.intuit.billingcomm.billing.qbeshosting.exception.UnsupportedEventException;
import com.intuit.billingcomm.billing.qbeshosting.model.HostingSubscriptionEvent;
import com.intuit.platform.common.exception.v2.NotFoundException;
import com.intuit.platform.webs.subscription.internal.model.entity.EntitledProductFeatureEntity;
import com.intuit.platform.webs.subscription.internal.model.entity.EntitledProductLicenseEntity;
import com.intuit.schema.platform.webs.subscription.internal.transactions.v3.EntitledFeaturesType;
import com.intuit.schema.platform.webs.subscription.internal.transactions.v3.EntitledProductInfoType;
import com.intuit.schema.platform.webs.subscription.internal.transactions.v3.Transaction;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Service
public class MessageParsingServiceImpl implements MessageParsingService {
    private static final String TRANSACTION_COMPLETED = "COMPLETED";
    private static final String TRANSACTION_HOLD = "HOLD";
    private static final String CONVERSION_SOURCE = "ICIS";

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageParsingServiceImpl.class);

    @Autowired
    private QbesCommonConfig qbesCommonConfig;

    @Autowired
    private ProductLicenseService productLicenseService;

    @Autowired
    private HostingFeatureService hostingFeatureService;

    @Override
    public HostingSubscriptionEvent parseMessage(Message<String> message) {
        String messagePayload = message.getPayload();
        try {
            Transaction transaction = xmlStringToTransactionObject(messagePayload);
            return transactionToSubscriptionEvent(transaction);
        } catch (JAXBException jaxbe) {
            LOGGER.error("Issue parsing XML.", jaxbe);
            throw new RuntimeException("Failed to parse message: " + messagePayload);
        }
    }

    private HostingSubscriptionEvent transactionToSubscriptionEvent(Transaction transaction) {
        if (transaction.getSubscriptionInfo() != null) {
            for (Transaction.SubscriptionInfo subInfo : transaction.getSubscriptionInfo()) {
                if (subInfo.getNextBillDate() != null) {
                    throw new UnsupportedEventException();
                }
                if (subInfo.getEntitledProductInfo() != null) {
                    for (EntitledProductInfoType prod : subInfo.getEntitledProductInfo()) {
                        if (prod.getEntitledFeatures() != null) {
                            for (EntitledFeaturesType feature : prod.getEntitledFeatures()) {
                                populateEmptyFeatureCode(feature);
                                if (qbesCommonConfig.getQbesHostingFeatureCodes().contains(feature.getFeatureCode())) {
                                    HostingSubscriptionEvent hostingEvent = new HostingSubscriptionEvent();
                                    hostingEvent.setAccountId(transaction.getCustomerAccountID());
                                    hostingEvent.setSubscriptionId(subInfo.getSubscriptionID());
                                    hostingEvent.setProductId(subInfo.getEntitledProductInfo().get(0).getEntitledProductID());
                                    if (prod.getErsEntitlements() != null && !prod.getErsEntitlements().isEmpty()) {
                                        hostingEvent.setLicense(prod.getErsEntitlements().get(0).getLicense());
                                        hostingEvent.setEoc(prod.getErsEntitlements().get(0).getEoc());
                                    } else {
                                        EntitledProductLicenseEntity license = productLicenseService
                                                .findCurrentBaseLicenseByProductId(prod.getEntitledProductID());
                                        hostingEvent.setLicense(license.getId().getLicense());
                                        hostingEvent.setEoc(license.getId().getEoc());
                                    }
                                    hostingEvent.setHostingFeatureId(feature.getEntitledFeatureID());
                                    hostingEvent.setChangeType(SubscriptionEventTypeEnum.valueOf(feature.getAction()));
                                    hostingEvent.setCompleted(transaction.getTransactionStatus()
                                            .equals(TRANSACTION_COMPLETED));
                                    hostingEvent.setOnHold(transaction.getTransactionStatus().equals(TRANSACTION_HOLD));
                                    hostingEvent.setConversion(CONVERSION_SOURCE
                                            .equals(transaction.getConversionSource()));
                                    hostingEvent.setNumberOfUsers(getNumberOfUsers(feature.getFeatureCode()));
                                    return hostingEvent;
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new UnsupportedEventException();
    }

    private void populateEmptyFeatureCode(EntitledFeaturesType feature) {
        if (StringUtils.isEmpty(feature.getFeatureCode())) {
            try {
                EntitledProductFeatureEntity featureEntity =
                        hostingFeatureService.findHostingFeatureById(feature.getEntitledFeatureID());
                feature.setFeatureCode(featureEntity.getFeatureCode());
            } catch (NotFoundException ignored) {}
        }
    }

    private Transaction xmlStringToTransactionObject(String xml) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Transaction.class);
        ObjectMapper mapper = new ObjectMapper();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        mapper.setDateFormat(dateFormat);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        return (Transaction) unmarshaller.unmarshal(reader);
    }

    private int getNumberOfUsers(String featureCode) {
        int start = featureCode.indexOf("_") + 1;
        int end = featureCode.lastIndexOf("_");
        String numberOfUsers = featureCode.substring(start, end);
        return Integer.parseInt(numberOfUsers);
    }
}
