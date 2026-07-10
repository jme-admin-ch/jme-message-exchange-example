package ch.admin.bit.jeap.jme.messageexchangeservice.client.mes;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import ch.admin.bit.jeap.messaging.mockkafka.KafkaMockTestConfig;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.client.MockJeapOAuth2RestClientBuilderFactory;
import ch.admin.bit.jeap.security.test.client.configuration.JeapOAuth2IntegrationTestClientConfiguration;
import ch.admin.bit.jeap.security.test.jws.JwsBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(properties = "messageexchange.client.url=http://localhost:8888/message-exchange")
@ActiveProfiles("test")
@PactConsumerTest
@PactTestFor(pactVersion = PactSpecVersion.V4)
@MockServerConfig(hostInterface = "localhost", port = "8888")
@Import({JeapOAuth2IntegrationTestClientConfiguration.class, KafkaMockTestConfig.class})
class MessageExchangeClientConsumerTest {
    private static final String PROVIDER = "bit-jme-message-exchange-service";
    private static final String CONSUMER = "bit-jme-message-exchange-client-service";
    private static final String API_PATH = "/message-exchange/api/internal/v3/messages";
    private static final String IN_MESSAGE_ID = "610b64cc-4211-4625-a11e-8e8bfe616876";
    private static final String OUT_MESSAGE_ID = "54acea68-addc-463b-9109-47089b7e4b90";
    static final String MESSAGE_RECEIVED_STATE = "A message has been received from a business partner with messageId=" + IN_MESSAGE_ID;
    private static final String BAD_MESSAGE_ID = "123464cc-1111-2222-3333-8e8bfe619999";
    private static final String IN_XML = "<partnermessage/>";
    private static final String OUT_XML = "<internalmessage/>";

    @Autowired
    private MockJeapOAuth2RestClientBuilderFactory mockRestClientBuilderFactory;

    @Autowired
    private MessageExchangeInternalService client;

    private String clientToken;

    @BeforeEach
    void init(@Autowired JwsBuilderFactory jwsBuilderFactory) {
        SemanticApplicationRole readRole = SemanticApplicationRole.builder()
                .system("jme")
                .resource("b2bmessagein")
                .operation("read")
                .build();
        SemanticApplicationRole writeRole = SemanticApplicationRole.builder()
                .system("jme")
                .resource("b2bmessageout")
                .operation("write")
                .build();
        clientToken = jwsBuilderFactory.createValidForFixedLongPeriodBuilder("jme-message-exchange-client-service", JeapAuthenticationContext.SYS)
                .withUserRoles(readRole, writeRole)
                .build().serialize();
    }

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    private V4Pact getMessageFromPartner(PactBuilder builder) {
        String path = API_PATH + "/" + IN_MESSAGE_ID;
        return builder
                .given(MESSAGE_RECEIVED_STATE)
                .expectsToReceiveHttpInteraction("A GET request to " + path, httpInteractionBuilder -> httpInteractionBuilder
                        .withRequest(httpRequestBuilder -> httpRequestBuilder
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                                .header(HttpHeaders.ACCEPT, "application/xml")
                                .method("GET")
                                .path(path))
                        .willRespondWith(httpResponseBuilder -> httpResponseBuilder
                                .status(200)
                                .header(HttpHeaders.CONTENT_TYPE, "application/xml")
                                .body(IN_XML.getBytes(UTF_8)))
                ).toPact();
    }

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    private V4Pact getMessageFromPartner_notFound(PactBuilder builder) {
        String path = API_PATH + "/" + BAD_MESSAGE_ID;
        return builder
                .given(MESSAGE_RECEIVED_STATE)
                .expectsToReceiveHttpInteraction("A GET request to " + path, httpInteractionBuilder -> httpInteractionBuilder
                        .withRequest(httpRequestBuilder -> httpRequestBuilder
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                                .header(HttpHeaders.ACCEPT, "application/xml")
                                .method("GET")
                                .path(path))
                        .willRespondWith(httpResponseBuilder -> httpResponseBuilder
                                .status(404))
                ).toPact();
    }

    @Pact(provider = PROVIDER, consumer = CONSUMER)
    private V4Pact sendMessageToPartner(PactBuilder builder) {
        String path = API_PATH + "/" + OUT_MESSAGE_ID;
        return builder
                .expectsToReceiveHttpInteraction("A PUT request to " + path, httpInteractionBuilder -> httpInteractionBuilder
                        .withRequest(httpRequestBuilder -> httpRequestBuilder
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                                .header(HttpHeaders.CONTENT_TYPE, "application/xml")
                                .header("bp-id", "1234")
                                .header("message-type", "someMessageType")
                                .header("partner-topic", "somePartnerTopic")
                                .queryParameter("topicName", "someTopic")
                                .queryParameter("groupId", "someGroupId")
                                .method("PUT")
                                .path(path))
                        .willRespondWith(httpResponseBuilder -> httpResponseBuilder
                                .status(201))
                ).toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getMessageFromPartner")
    void testGetMessageFromPartner() {
        mockRestClientBuilderFactory.getAuthTokenProvider().setAuthToken(clientToken);

        byte[] result = client.getMessage(UUID.fromString(IN_MESSAGE_ID), MediaType.APPLICATION_XML_VALUE);

        assertThat(new String(result))
                .isEqualTo(IN_XML);
    }

    @Test
    @PactTestFor(pactMethod = "getMessageFromPartner_notFound")
    void testGetMessageFromPartner_notFound() {
        mockRestClientBuilderFactory.getAuthTokenProvider().setAuthToken(clientToken);

        assertThatThrownBy(() -> client.getMessage(UUID.fromString(BAD_MESSAGE_ID), MediaType.APPLICATION_XML_VALUE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    @PactTestFor(pactMethod = "sendMessageToPartner")
    void testPutMessageForPartner() {
        mockRestClientBuilderFactory.getAuthTokenProvider().setAuthToken(clientToken);

        String bpId = "1234";
        String messageType = "someMessageType";
        String partnerTopic = "somePartnerTopic";
        String topicName = "someTopic";
        String groupId = "someGroupId";

        assertDoesNotThrow(() ->
                client.sendMessage(UUID.fromString(OUT_MESSAGE_ID),
                        bpId, messageType, partnerTopic, topicName, groupId,
                        null,
                        null,
                        MediaType.APPLICATION_XML_VALUE,
                        OUT_XML.getBytes(UTF_8)));
    }

}
