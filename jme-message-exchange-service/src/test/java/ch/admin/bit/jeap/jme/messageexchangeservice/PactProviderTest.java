package ch.admin.bit.jeap.jme.messageexchangeservice;

import ch.admin.bit.jeap.messageexchange.test.Pacticipants;
import ch.admin.bit.jeap.messageexchange.web.MessageExchangeApplication;
import ch.admin.bit.jeap.messageexchange.web.rest.model.PactProviderTestBase;
import ch.admin.bit.jeap.messaging.mockkafka.KafkaMockTestConfig;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import ch.admin.bit.jeap.security.test.resource.configuration.JeapOAuth2IntegrationTestResourceConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = MessageExchangeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "logging.level.au.com.dius.pact=DEBUG",
                "jeap.security.oauth2.resourceserver.authorization-server.issuer=" + JwsBuilder.DEFAULT_ISSUER,
                "jeap.security.oauth2.resourceserver.authorization-server.jwk-set-uri=http://localhost:1235/message-exchange/.well-known/jwks.json",
                "spring.cloud.aws.sqs.enabled=false"
        }
)
@Import({
        JeapOAuth2IntegrationTestResourceConfiguration.class,
        KafkaMockTestConfig.class})
public class PactProviderTest extends PactProviderTestBase {

    @MockitoBean
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @MockitoBean
    private TransactionTemplate transactionTemplate;

    @BeforeAll
    static void beforeAll() {
        Pacticipants.setMessageExchangePacticipant("bit-jme-message-exchange-service");
    }
}
