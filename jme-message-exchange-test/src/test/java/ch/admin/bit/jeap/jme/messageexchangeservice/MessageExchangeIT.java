package ch.admin.bit.jeap.jme.messageexchangeservice;

import ch.admin.bit.jeap.jme.test.BootServiceSpringIntegrationTestBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
class MessageExchangeIT extends BootServiceSpringIntegrationTestBase {

    private static final String AUTH_BASE_URL = "http://localhost:8081/jme-message-exchange-auth-scs";
    private static final String SCS_BASE_URL = "http://localhost:8080/message-exchange";
    private static final String APP_BASE_URL = "http://localhost:8082/message-exchange-client";

    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String HEADER_BP_ID = "bp-id";
    private static final String HEADER_PARTNER_EXTERNAL_REFERENCE = "partner-external-reference";
    private static final String HEADER_PARTNER_TOPIC = "partner-topic";
    private static final String HEADER_MES_METADATA = "mes-metadata";

    @BeforeAll
    static void startServices() throws Exception {
        startService("jme-message-exchange-auth-scs", AUTH_BASE_URL);
        startService("jme-message-exchange-service", SCS_BASE_URL);
        startService("jme-message-exchange-client-service", APP_BASE_URL);
    }

    @Test
    void putMessageFromPartner_json_thenReadFromInternalApp() {
        Response putMessageFromPartnerResponse = given().baseUri(APP_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/api/messages/partner?contentType=application/json");

        String messageId = putMessageFromPartnerResponse.getBody().asString();
        log.info("putMessageFromPartner_thenReadFromInternalApp sent message with id: {}", messageId);

        Response getMessageFromPartnerResponse = await()
                .atMost(TEST_TIMEOUT)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    Response response = given().baseUri(APP_BASE_URL)
                            .accept(MediaType.APPLICATION_JSON_VALUE)
                            .when()
                            .get("/api/messages/internal/" + messageId + "?contentType=application/json");
                    return response.statusCode() >= 200 && response.statusCode() < 300 ? response : null;
                }, Objects::nonNull);

        assertThat(getMessageFromPartnerResponse).isNotNull();
        String messageFromPartner = getMessageFromPartnerResponse.getBody().asString();
        assertThat(messageFromPartner).isEqualTo("{\"partnermessage-id\"=\"%s\"}".formatted(messageId));
    }

    @Test
    void putMessageFromInternalApp_json_thenReadFromPartner() {
        Response putMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/api/messages/internal?contentType=application/json");

        String messageId = putMessageFromInternalAppResponse.getBody().asString();

        Response getMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .header(HEADER_BP_ID, "123")
                .when()
                .get("/api/messages/partner/" + messageId + "?contentType=application/json");

        String messageFromPartner = getMessageFromInternalAppResponse.getBody().asString();

        assertThat(messageFromPartner)
                .isEqualTo("{\"internalmessage-id\"=\"%s\"}".formatted(messageId));
    }

    @Test
    void putMessageFromInternalAppWithHeaders_json_thenReadFromPartner() throws JsonProcessingException {

        Map<String, String> metadata = Map.of(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString()
        );

        String partnerExternalReference1 = UUID.randomUUID().toString();
        Response putMessageFromInternalAppResponse1 = given().baseUri(APP_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .queryParam("partnerExternalReference", partnerExternalReference1)
                .queryParam("contentType", MediaType.APPLICATION_JSON_VALUE)
                .body(metadata)
                .when()
                .post("/api/messages/internal");

        String messageId1 = putMessageFromInternalAppResponse1.getBody().asString();

        String partnerExternalReference2 = UUID.randomUUID().toString();
        Response putMessageFromInternalAppResponse2 = given().baseUri(APP_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .queryParam("partnerExternalReference", partnerExternalReference2)
                .queryParam("contentType", MediaType.APPLICATION_JSON_VALUE)
                .body(metadata)
                .when()
                .post("/api/messages/internal");

        String messageId2 = putMessageFromInternalAppResponse2.getBody().asString();

        Response getMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .header(HEADER_BP_ID, "123")
                .queryParam("contentType", MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/api/messages/partner/" + messageId1);

        String messageFromPartner = getMessageFromInternalAppResponse.getBody().asString();

        assertThat(messageFromPartner).isEqualTo("{\"internalmessage-id\"=\"%s\"}".formatted(messageId1));

        assertThat(getMessageFromInternalAppResponse.getHeader(HEADER_PARTNER_EXTERNAL_REFERENCE)).isEqualTo(partnerExternalReference1);
        assertThat(getMessageFromInternalAppResponse.getHeader(HEADER_PARTNER_TOPIC)).isEqualTo(HEADER_PARTNER_TOPIC);
        assertThat(getMessageFromInternalAppResponse.getHeader(HEADER_MES_METADATA)).isNotBlank();

        String decode = new String(Base64.getDecoder().decode(getMessageFromInternalAppResponse.getHeader(HEADER_MES_METADATA).getBytes()));

        Map<String, String> metadataMap = new ObjectMapper().readValue(decode, new TypeReference<>() {
        });

        assertThat(metadataMap).isEqualTo(metadata);

        Response getNextMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .header(HEADER_BP_ID, "123")
                .queryParam("partnerExternalReference", partnerExternalReference2)
                .when()
                .get("/api/messages/partner/messages/" + messageId1 + "/next");

        messageFromPartner = getNextMessageFromInternalAppResponse.getBody().asString();

        assertThat(messageFromPartner).isEqualTo("{\"internalmessage-id\"=\"%s\"}".formatted(messageId2));
        assertThat(getNextMessageFromInternalAppResponse.getHeader(HEADER_PARTNER_EXTERNAL_REFERENCE)).isEqualTo(partnerExternalReference2);
    }

    @Test
    void putMessageFromInternalApp_json_thenReadFromNotifications() {
        Response putMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/api/messages/internal?contentType=application/json");

        String messageId = putMessageFromInternalAppResponse.getBody().asString();

        await()
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    Response getMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                            .when()
                            .get("/api/messages/notifications/messages/internal");
                    String messagesString = getMessageFromInternalAppResponse.getBody().asString();
                    String statusCode = String.valueOf(getMessageFromInternalAppResponse.getStatusCode());
                    log.info("Checking /api/messages/notifications/messages/internal -> status: {}, message: {}",
                            statusCode, messagesString);
                    assertThat(messagesString).contains(messageId);
                });
    }

    @Test
    void putMessageFromPartner_xml_thenReadFromInternalApp() {
        Response putMessageFromPartnerResponse = given().baseUri(APP_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/api/messages/partner");

        String messageId = putMessageFromPartnerResponse.getBody().asString();
        log.info("putMessageFromPartner_thenReadFromInternalApp sent message with id: {}", messageId);

        Response getMessageFromPartnerResponse = await()
                .atMost(TEST_TIMEOUT)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    Response response = given().baseUri(APP_BASE_URL)
                            .accept(ContentType.XML)
                            .when()
                            .get("/api/messages/internal/" + messageId);
                    return response.statusCode() >= 200 && response.statusCode() < 300 ? response : null;
                }, Objects::nonNull);

        assertThat(getMessageFromPartnerResponse).isNotNull();
        String xmlMessageFromPartner = getMessageFromPartnerResponse.getBody().asString();
        assertThat(xmlMessageFromPartner).isEqualTo("<partnermessage id=\"%s\"/>".formatted(messageId));
    }

    @Test
    void putMessageFromInternalApp_xml_thenReadFromPartner() {
        Response putMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/api/messages/internal");

        String messageId = putMessageFromInternalAppResponse.getBody().asString();

        Response getMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                .accept(ContentType.XML)
                .header(HEADER_BP_ID, "123")
                .when()
                .get("/api/messages/partner/" + messageId);

        String xmlMessageFromPartner = getMessageFromInternalAppResponse.getBody().asString();

        assertThat(xmlMessageFromPartner)
                .isEqualTo("<internalmessage id=\"%s\"/>".formatted(messageId));
    }

    @Test
    void putMessageFromInternalAppWithHeaders_xml_thenReadFromPartner() throws JsonProcessingException {

        Map<String, String> metadata = Map.of(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString()
        );

        String partnerExternalReference1 = UUID.randomUUID().toString();
        Response putMessageFromInternalAppResponse1 = given().baseUri(APP_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .queryParam("partnerExternalReference", partnerExternalReference1)
                .body(metadata)
                .when()
                .post("/api/messages/internal");

        String messageId1 = putMessageFromInternalAppResponse1.getBody().asString();

        String partnerExternalReference2 = UUID.randomUUID().toString();
        Response putMessageFromInternalAppResponse2 = given().baseUri(APP_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .queryParam("partnerExternalReference", partnerExternalReference2)
                .body(metadata)
                .when()
                .post("/api/messages/internal");

        String messageId2 = putMessageFromInternalAppResponse2.getBody().asString();

        Response getMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .header(HEADER_BP_ID, "123")
                .when()
                .get("/api/messages/partner/" + messageId1);

        String messageFromPartner = getMessageFromInternalAppResponse.getBody().asString();

        assertThat(messageFromPartner).isEqualTo("<internalmessage id=\"%s\"/>".formatted(messageId1));

        assertThat(getMessageFromInternalAppResponse.getHeader(HEADER_PARTNER_EXTERNAL_REFERENCE)).isEqualTo(partnerExternalReference1);
        assertThat(getMessageFromInternalAppResponse.getHeader(HEADER_PARTNER_TOPIC)).isEqualTo(HEADER_PARTNER_TOPIC);
        assertThat(getMessageFromInternalAppResponse.getHeader(HEADER_MES_METADATA)).isNotBlank();

        String decode = new String(Base64.getDecoder().decode(getMessageFromInternalAppResponse.getHeader(HEADER_MES_METADATA).getBytes()));

        Map<String, String> metadataMap = new ObjectMapper().readValue(decode, new TypeReference<>() {
        });

        assertThat(metadataMap).isEqualTo(metadata);


        Response getNextMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .header(HEADER_BP_ID, "123")
                .queryParam("partnerExternalReference", partnerExternalReference2)
                .when()
                .get("/api/messages/partner/messages/" + messageId1 + "/next");

        messageFromPartner = getNextMessageFromInternalAppResponse.getBody().asString();

        assertThat(messageFromPartner).isEqualTo("<internalmessage id=\"%s\"/>".formatted(messageId2));
        assertThat(getNextMessageFromInternalAppResponse.getHeader(HEADER_PARTNER_EXTERNAL_REFERENCE)).isEqualTo(partnerExternalReference2);
    }

    @Test
    void putMessageFromInternalApp_xml_thenReadFromNotifications() {
        Response putMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/api/messages/internal");

        String messageId = putMessageFromInternalAppResponse.getBody().asString();

        await()
                .atMost(TEST_TIMEOUT)
                .untilAsserted(() -> {
                    Response getMessageFromInternalAppResponse = given().baseUri(APP_BASE_URL)
                            .when()
                            .get("/api/messages/notifications/messages/internal");
                    String messagesString = getMessageFromInternalAppResponse.getBody().asString();
                    String statusCode = String.valueOf(getMessageFromInternalAppResponse.getStatusCode());
                    log.info("Checking /api/messages/notifications/messages/internal -> status: {}, message: {}",
                            statusCode, messagesString);
                    assertThat(messagesString).contains(messageId);
                });
    }

}
