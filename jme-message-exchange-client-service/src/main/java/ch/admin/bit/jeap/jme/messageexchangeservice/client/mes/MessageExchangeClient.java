package ch.admin.bit.jeap.jme.messageexchangeservice.client.mes;

import ch.admin.bit.jeap.messageexchange.event.message.received.S3ObjectMalwareScanStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageExchangeClient {
    @Getter
    private final Collection<Message> receivedPartnerMessages = new CircularFifoQueue<>(100);
    @Getter
    private final Collection<UUID> receivedInternalMessageIds = new CircularFifoQueue<>(100);

    private final MessageExchangeInternalService messageExchangeInternalService;

    public void onPartnerMessage(UUID messageId, S3ObjectMalwareScanStatus scanStatus) {
        byte[] messagePayload = messageExchangeInternalService.getMessage(messageId, MediaType.APPLICATION_XML_VALUE);

        log.info("Received message with id {}: {} and scanStatus {}", messageId, new String(messagePayload, UTF_8), scanStatus);

        receivedPartnerMessages.add(new Message(messageId, scanStatus));
    }

    public void onInternalMessage(UUID messageId) {
        log.info("Received message with id {}", messageId);
        receivedInternalMessageIds.add(messageId);
    }

    public record Message(UUID messageId, S3ObjectMalwareScanStatus scanStatus) {
        @Override
        public String toString() {
            return messageId.toString() + " " + scanStatus;
        }
    }
}
