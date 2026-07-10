package ch.admin.bit.jeap.jme.messageexchangeservice;

import ch.admin.bit.jeap.messageexchange.test.Pacticipants;
import ch.admin.bit.jeap.messageexchange.web.rest.model.PactProviderTestBase;
import ch.admin.bit.jeap.messaging.mockkafka.KafkaMockTestConfig;
import ch.admin.bit.jeap.security.test.resource.configuration.JeapOAuth2IntegrationTestResourceConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

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
