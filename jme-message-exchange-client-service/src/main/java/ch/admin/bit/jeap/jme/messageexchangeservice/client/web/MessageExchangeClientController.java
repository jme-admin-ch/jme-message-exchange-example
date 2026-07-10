package ch.admin.bit.jeap.jme.messageexchangeservice.client.web;

import ch.admin.bit.jeap.jme.messageexchangeservice.client.mes.MessageExchangeClient;
import ch.admin.bit.jeap.jme.messageexchangeservice.client.mes.MessageExchangeInternalService;
import ch.admin.bit.jeap.jme.messageexchangeservice.client.mes.MessageExchangePartnerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Tag(name = "MES Client", description = "A client API to demonstrate the message exchange service")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageExchangeClientController {

    private static final String HEADER_MES_METADATA = "mes-metadata";
    private static final String HEADER_PARTNER_TOPIC = "partner-topic";
    private static final String HEADER_PARTNER_EXTERNAL_REFERENCE = "partner-external-reference";
    private static final String HEADER_MESSAGE_ID = "message-id";
    private static final String HEADER_BP_ID = "bp-id";

    private final MessageExchangePartnerService messageExchangePartnerService;
    private final MessageExchangePartnerService messageExchangePartnerServicePrivileged;
    private final MessageExchangeInternalService messageExchangeInternalService;
    private final MessageExchangeClient messageExchangeClient;

    @PostMapping("/partner")
    @Operation(summary = "Submit a new message using the MES partner API", responses = {
            @ApiResponse(responseCode = "200", description = "Submitted new message")
    })
    public String putMessageFromPartner(@RequestParam(value = "contentType", defaultValue = "application/xml") String contentType,
                                        @RequestParam(value = "partnerTopic", required = false) String partnerTopic,
                                        @RequestParam(value = "partnerExternalReference", required = false) String partnerExternalReference) {
        UUID messageId = UUID.randomUUID();
        String bpId = "123";
        String messageType = "test";
        byte[] messageBody = "<partnermessage id=\"%s\"/>".formatted(messageId).getBytes(UTF_8);

        if (contentType.equals(MediaType.APPLICATION_JSON_VALUE)) {
            messageBody = "{\"partnermessage-id\"=\"%s\"}".formatted(messageId).getBytes(UTF_8);
        }

        messageExchangePartnerService.sendMessage(messageId, bpId, messageType, partnerTopic, partnerExternalReference, contentType, messageBody);

        return messageId.toString();
    }

    @PostMapping("/privilegedsystem")
    @Operation(summary = "Submit a new message using the MES partner API as privileged system", responses = {
            @ApiResponse(responseCode = "200", description = "Submitted new message")
    })
    public String putMessageFromPrivilegedSystem(@RequestParam(value = "contentType", defaultValue = "application/xml") String contentType) {
        UUID messageId = UUID.randomUUID();
        String bpId = "123";
        String messageType = "test";
        byte[] messageBody = "<partnermessage id=\"%s\"/>".formatted(messageId).getBytes(UTF_8);

        if (contentType.equals(MediaType.APPLICATION_JSON_VALUE)) {
            messageBody = "{\"partnermessage-id\"=\"%s\"}".formatted(messageId).getBytes(UTF_8);
        }

        messageExchangePartnerServicePrivileged.sendMessage(messageId, bpId, messageType, contentType, null, null, messageBody);

        return messageId.toString();
    }

    @GetMapping(value = "/internal/{messageId}")
    @Operation(summary = "Get a message using the MES internal API", responses = {
            @ApiResponse(responseCode = "200", description = "Found message")
    })
    public byte[] getMessageFromPartner(@PathVariable("messageId") UUID messageId, @RequestParam(value = "contentType", defaultValue = "application/xml") String contentType) {
        return messageExchangeInternalService.getMessage(messageId, contentType);
    }

    @PostMapping("/internal")
    @Operation(summary = "Submit a new message using the MES internal API", responses = {
            @ApiResponse(responseCode = "200", description = "Submitted new message")
    })
    public String putMessageFromInternalSystem(@RequestParam(value = "contentType", defaultValue = "application/xml") String contentType,
                                               @RequestParam(value = "partnerExternalReference", required = false) String partnerExternalReference,
                                               @RequestBody(required = false) Map<String, String> metadata) throws JsonProcessingException {
        UUID messageId = UUID.randomUUID();
        String bpId = "123";
        String messageType = "test";
        String partnerTopic = "partner-topic";
        String topicName = "test-topic";
        String groupId = "test-group";
        String metadataBase64 = null;
        byte[] messageBody = "<internalmessage id=\"%s\"/>".formatted(messageId).getBytes(UTF_8);

        if (contentType.equals("application/json")) {
            messageBody = "{\"internalmessage-id\"=\"%s\"}".formatted(messageId).getBytes(UTF_8);
        }
        if (metadata != null && !metadata.isEmpty()) {
            metadataBase64 = java.util.Base64.getEncoder().encodeToString(new ObjectMapper().writeValueAsString(metadata).getBytes());
        }

        messageExchangeInternalService.sendMessage(messageId, bpId, messageType, partnerTopic, topicName, groupId, partnerExternalReference, metadataBase64, contentType, messageBody);

        return messageId.toString();
    }

    @GetMapping(value = "/partner/{messageId}")
    @Operation(summary = "Get a message using the MES partner API", responses = {
            @ApiResponse(responseCode = "200", description = "Found message")
    })
    public ResponseEntity<byte[]> getMessageFromInternalSystem(@PathVariable("messageId") UUID messageId,
                                               @RequestHeader(HEADER_BP_ID) String bpId,
                                               @RequestParam(value = "contentType", defaultValue = "application/xml") String contentType) {
        ResponseEntity<byte[]> response = messageExchangePartnerService.getMessage(messageId, bpId, contentType);
        return ResponseEntity
                .ok()
                .header(HEADER_MES_METADATA, response.getHeaders().getFirst(HEADER_MES_METADATA))
                .header(HEADER_PARTNER_TOPIC, response.getHeaders().getFirst(HEADER_PARTNER_TOPIC))
                .header(HEADER_PARTNER_EXTERNAL_REFERENCE, response.getHeaders().getFirst(HEADER_PARTNER_EXTERNAL_REFERENCE))
                .header(HEADER_MESSAGE_ID, response.getHeaders().getFirst(HEADER_MESSAGE_ID))
                .body(response.getBody());
    }

    @GetMapping(value = "/privilegedsystem/{messageId}")
    @Operation(summary = "Get a message using the MES partner API as privileged system", responses = {
            @ApiResponse(responseCode = "200", description = "Found message")
    })
    public ResponseEntity<byte[]> getMessageFromInternalSystemPrivileged(@PathVariable("messageId") UUID messageId,
                                                         @RequestHeader(HEADER_BP_ID) String bpId,
                                                         @RequestParam(value = "contentType", defaultValue = "application/xml") String contentType) {
        ResponseEntity<byte[]> response = messageExchangePartnerServicePrivileged.getMessage(messageId, bpId, contentType);
        return ResponseEntity
                .ok()
                .header(HEADER_MES_METADATA, response.getHeaders().getFirst(HEADER_MES_METADATA))
                .header(HEADER_PARTNER_TOPIC, response.getHeaders().getFirst(HEADER_PARTNER_TOPIC))
                .header(HEADER_PARTNER_EXTERNAL_REFERENCE, response.getHeaders().getFirst(HEADER_PARTNER_EXTERNAL_REFERENCE))
                .header(HEADER_MESSAGE_ID, response.getHeaders().getFirst(HEADER_MESSAGE_ID))
                .body(response.getBody());
    }

    @GetMapping(value = "/partner/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getMessagesFromInternalSystem(@RequestHeader(HEADER_BP_ID) String bpId,
                                                @RequestParam(value = "partnerExternalReference", required = false) String partnerExternalReference) {
        return messageExchangePartnerService.getMessages(bpId, null, null, null, 10, null, partnerExternalReference);
    }

    @GetMapping(value = "/partner/messages/{messageId}/next", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> getNextMessageFromInternalSystem(@PathVariable("messageId") UUID messageId,
                                                                   @RequestHeader(HEADER_BP_ID) String bpId,
                                                                   @RequestParam(value = "partnerTopic", required = false) String partnerTopic,
                                                                   @RequestParam(value = "partnerExternalReference", required = false) String partnerExternalReference) {
        ResponseEntity<byte[]> response = messageExchangePartnerService.getNextMessage(messageId, bpId, null, partnerTopic, partnerExternalReference);
        return ResponseEntity
                .ok()
                .header(HEADER_MES_METADATA, response.getHeaders().getFirst(HEADER_MES_METADATA))
                .header(HEADER_PARTNER_TOPIC, response.getHeaders().getFirst(HEADER_PARTNER_TOPIC))
                .header(HEADER_PARTNER_EXTERNAL_REFERENCE, response.getHeaders().getFirst(HEADER_PARTNER_EXTERNAL_REFERENCE))
                .header(HEADER_MESSAGE_ID, response.getHeaders().getFirst(HEADER_MESSAGE_ID))
                .body(response.getBody());
    }

    @GetMapping(value = "/notifications/messages/partner", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List the last 100 message IDs for notifications received for new messages from partners")
    public String[] getReceivedPartnerMessages() {
        return messageExchangeClient.getReceivedPartnerMessages().stream()
                .map(MessageExchangeClient.Message::toString)
                .toList()
                .toArray(String[]::new);
    }

    @GetMapping(value = "/notifications/messages/internal", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List the last 100 message IDs for notifications received for new messages from internal")
    public String[] getReceivedInternalMessageIds() {
        return messageExchangeClient.getReceivedInternalMessageIds().stream()
                .map(UUID::toString)
                .toList()
                .toArray(String[]::new);
    }
}
