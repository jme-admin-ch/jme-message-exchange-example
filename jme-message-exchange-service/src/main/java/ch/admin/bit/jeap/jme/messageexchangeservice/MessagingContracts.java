package ch.admin.bit.jeap.jme.messageexchangeservice;

import ch.admin.bit.jeap.messageexchange.event.message.received.B2BMessageReceivedEvent;
import ch.admin.bit.jeap.messageexchange.event.message.sent.B2BMessageSentEvent;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;

@JeapMessageProducerContract(value = B2BMessageReceivedEvent.TypeRef.class, topic = "jme-messageexchange-b2bmessagereceived")
@JeapMessageProducerContract(value = B2BMessageSentEvent.TypeRef.class, topic = "jme-messageexchange-b2bmessagesent")
class MessagingContracts {
}
