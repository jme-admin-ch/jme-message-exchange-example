package ch.admin.bit.jeap.jme.messageexchangeservice.client;

import ch.admin.bit.jeap.messageexchange.event.message.received.B2BMessageReceivedEvent;
import ch.admin.bit.jeap.messageexchange.event.message.sent.B2BMessageSentEvent;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContract;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@JeapMessageConsumerContract(value = B2BMessageReceivedEvent.TypeRef.class, topic = "jme-messageexchange-b2bmessagereceived")
@JeapMessageConsumerContract(value = B2BMessageSentEvent.TypeRef.class, topic = "jme-messageexchange-b2bmessagesent")
@SpringBootApplication
public class MesClientApplication {
    static void main(String[] args) {
        SpringApplication.run(MesClientApplication.class, args);
    }
}
