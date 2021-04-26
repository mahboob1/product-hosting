package com.intuit.billingcomm.billing.qbeshosting.jms;

import com.intuit.billingcomm.billing.qbeshosting.TestHelpers;
import com.intuit.billingcomm.billing.qbeshosting.exception.IncompleteEventException;
import com.intuit.billingcomm.billing.qbeshosting.exception.UnsupportedEventException;
import com.intuit.billingcomm.billing.qbeshosting.model.HostingSubscriptionEvent;
import com.intuit.billingcomm.billing.qbeshosting.service.MessageParsingService;
import com.intuit.billingcomm.billing.qbeshosting.service.SubscriptionService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.Message;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionEventListenerTest {

    @InjectMocks
    private SubscriptionEventListener listener;

    @Mock
    private MessageParsingService messageParsingService;

    @Mock
    private SubscriptionService subscriptionService;

    @Test
    public void receive_ValidMessage_Success() throws Exception {
        Message<String> message = TestHelpers.generateValidSubscriptionMessage();
        HostingSubscriptionEvent event = TestHelpers.generateSubscriptionEvent();
        Mockito.when(messageParsingService.parseMessage(message)).thenReturn(event);
        listener.receive(message);
        verify(subscriptionService, times(1)).processEvent(event);
    }

    @Test
    public void receive_UnsupportedMessage_Ignored() throws Exception {
        Message<String> message = TestHelpers.generateUnsupportedMessage();
        Mockito.when(messageParsingService.parseMessage(message)).thenThrow(UnsupportedEventException.class);
        listener.receive(message);
        verify(subscriptionService, never()).processEvent(any(HostingSubscriptionEvent.class));
    }

    @Test
    public void receive_IncompleteEvent_Ignored() throws Exception {
        Message<String> message = TestHelpers.generateUnsupportedMessage();
        Mockito.when(messageParsingService.parseMessage(message)).thenThrow(IncompleteEventException.class);
        listener.receive(message);
    }

    @Test
    public void receive_IllegalArgument_NoExceptionRethrown() throws Exception {
        Message<String> message = TestHelpers.generateUnsupportedMessage();
        Mockito.doThrow(IllegalArgumentException.class).when(subscriptionService).processEvent(any());
        listener.receive(message);
    }

    @Test
    public void receive_IllegalState_NoExceptionRethrown() throws Exception {
        Message<String> message = TestHelpers.generateUnsupportedMessage();
        Mockito.doThrow(IllegalStateException.class).when(subscriptionService).processEvent(any());
        listener.receive(message);
    }

    @Test(expected = Exception.class)
    public void receive_RethrowException() throws Exception {
        Message<String> message = TestHelpers.generateValidSubscriptionMessage();
        Mockito.doThrow(Exception.class).when(subscriptionService).processEvent(any());
        listener.receive(message);
    }
}
