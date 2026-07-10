package ch.admin.bit.jeap.jme.messageexchangeservice.client.mes;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.UUID;

/**
 * REST API Client for the "internal" MES API
 */
@HttpExchange(url = "/api/internal/v3/messages")
public interface MessageExchangeInternalService {

    @PutExchange(value = "/{messageId}")
    void sendMessage(@PathVariable UUID messageId,
                     @RequestHeader("bp-id") String bpId,
                     @RequestHeader("message-type") String messageType,
                     @RequestHeader("partner-topic") String partnerTopic,
                     @RequestParam("topicName") String topicName,
                     @RequestParam(value = "groupId", required = false) String groupId,
                     @RequestHeader(value = "partner-external-reference", required = false) String partnerExternalReference,
                     @RequestHeader(value = "mes-metadata", required = false) String metadata,
                     @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                     @RequestBody byte[] messageBody);

    @GetExchange(value = "/{messageId}")
    byte[] getMessage(@PathVariable("messageId") UUID messageId, @RequestHeader(HttpHeaders.ACCEPT) String accept);
}
