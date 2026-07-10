package ch.admin.bit.jeap.jme.messageexchangeservice.client;

import ch.admin.bit.jeap.jme.messageexchangeservice.client.mes.MessageExchangeInternalService;
import ch.admin.bit.jeap.jme.messageexchangeservice.client.mes.MessageExchangePartnerService;
import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.net.URI;

@Configuration
public class ClientConfig {

    private @Value("${messageexchange.client.url}") String baseUrl;

    @Bean
    MessageExchangePartnerService messageExchangePartnerService(JeapOAuth2RestClientBuilderFactory factory) {
        return createClient(MessageExchangePartnerService.class, factory, "partner-client");
    }

    @Bean
    MessageExchangePartnerService messageExchangePartnerServicePrivileged(JeapOAuth2RestClientBuilderFactory factory) {
        return createClient(MessageExchangePartnerService.class, factory, "privileged-system-client");
    }

    @Bean
    MessageExchangeInternalService messageExchangeInternalService(JeapOAuth2RestClientBuilderFactory factory) {
        return createClient(MessageExchangeInternalService.class, factory, "internal-client");
    }

    private <T> T createClient(Class<T> type, JeapOAuth2RestClientBuilderFactory factory, String clientRegistryId) {
        RestClient restClient = factory
                .createForClientRegistryId(clientRegistryId)
                .baseUrl(baseUrl)
                .defaultStatusHandler(new ForwardStatusCodeErrorHandler())
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(type);
    }

    /**
     * Forwards HTTP status codes from client requests to the HTTP response
     */
    private static class ForwardStatusCodeErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
            try {
                super.handleError(url, method, response);
            } catch (RestClientResponseException e) {
                throw new ResponseStatusException(e.getStatusCode(), e.getStatusText());
            }
        }
    }
}
