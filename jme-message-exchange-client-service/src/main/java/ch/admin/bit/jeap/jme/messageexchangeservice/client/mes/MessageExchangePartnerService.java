package ch.admin.bit.jeap.jme.messageexchangeservice.client.mes;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.UUID;

/**
 * REST API Client for the MES business partner API
 */
@HttpExchange(url = "/api/partner/v4/messages")
public interface MessageExchangePartnerService {

    @PutExchange(value = "/{messageId}")
    void sendMessage(@PathVariable("messageId") UUID messageId,
                     @RequestHeader("bp-id") String bpId,
                     @RequestHeader("message-type") String messageType,
                     @RequestHeader(value = "partner-topic", required = false) String partnerTopic,
                     @RequestHeader(value = "partner-external-reference", required = false) String partnerExternalReference,
                     @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
                     @RequestBody byte[] messageBody);

    @GetExchange(value = "/")
    String getMessages(@RequestHeader("bp-id") String bpId,
                       @RequestParam(value = "topicName", required = false) String topicName,
                       @RequestParam(value = "groupId", required = false) String groupId,
                       @RequestParam(value = "lastMessageId", required = false) UUID lastMessageId,
                       @RequestParam("size") int size,
                       @RequestParam(value = "partnerTopic", required = false) String partnerTopic,
                       @RequestParam(value = "partnerExternalReference", required = false) String partnerExternalReference);

    @GetExchange(value = "/{messageId}")
    ResponseEntity<byte[]> getMessage(@PathVariable("messageId") UUID messageId,
                                      @RequestHeader("bp-id") String bpId,
                                      @RequestHeader(HttpHeaders.ACCEPT) String contentType);

    @GetExchange(value = "/{messageId}/next")
    ResponseEntity<byte[]> getNextMessage(@PathVariable("messageId") UUID messageId,
                                          @RequestHeader("bp-id") String bpId,
                                          @RequestParam(value = "topicName", required = false) String topicName,
                                          @RequestParam(value = "partnerTopic", required = false) String partnerTopic,
                                          @RequestParam(value = "partnerExternalReference", required = false) String partnerExternalReference);
}
