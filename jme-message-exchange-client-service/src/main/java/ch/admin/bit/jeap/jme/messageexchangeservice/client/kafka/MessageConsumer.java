package ch.admin.bit.jeap.jme.messageexchangeservice.client.kafka;

import ch.admin.bit.jeap.jme.messageexchangeservice.client.mes.MessageExchangeClient;
import ch.admin.bit.jeap.messageexchange.event.message.received.B2BMessageReceivedEvent;
import ch.admin.bit.jeap.messageexchange.event.message.sent.B2BMessageSentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final MessageExchangeClient messageExchangeClient;

    @KafkaListener(topics = "jme-messageexchange-b2bmessagereceived")
    public void listenToReceivedEvent(B2BMessageReceivedEvent event, Acknowledgment acknowledgment) {
        UUID messageId = UUID.fromString(
                event.getReferences().getMessageReference().getMessageId());

        messageExchangeClient.onPartnerMessage(messageId, event.getPayload().getScanStatus());
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "jme-messageexchange-b2bmessagesent")
    public void listenToSentEvent(B2BMessageSentEvent event, Acknowledgment acknowledgment) {
        UUID messageId = UUID.fromString(
                event.getReferences().getMessageReference().getMessageId());

        messageExchangeClient.onInternalMessage(messageId);
        acknowledgment.acknowledge();
    }
}
