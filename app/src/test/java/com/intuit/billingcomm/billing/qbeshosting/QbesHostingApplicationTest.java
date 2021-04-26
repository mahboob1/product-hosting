package com.intuit.billingcomm.billing.qbeshosting;

import com.intuit.billingcomm.billing.qbescommon.service.CustomerContactService;
import com.intuit.billingcomm.billing.qbeshosting.jms.SubscriptionEventListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class QbesHostingApplicationTest {

    @Autowired
    private SubscriptionEventListener subscriptionEventListener;
       
    @MockBean
	private CustomerContactService customerContactService;
	
    @Test
    public void contextLoads() {
        assertNotNull(subscriptionEventListener);
    }
}
