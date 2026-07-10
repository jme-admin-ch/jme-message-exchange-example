package ch.admin.bit.jeap.jme.messageexchangeservice.client.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "JME Message Exchange Service Client Example",
                description = "An example showing how to use the API of the message exchange service",
                contact = @Contact(
                        email = "jEAP-Community@bit.admin.ch",
                        name = "jEAP",
                        url = "https://confluence.eap.bit.admin.ch/display/JEAP"
                )
        )
)
@Configuration
public class SwaggerConfig {

    @Bean
    GroupedOpenApi messageApi() {
        return GroupedOpenApi.builder()
                .group("Message Exchange Service Client API")
                .pathsToMatch("/api/**")
                .packagesToScan(this.getClass().getPackageName())
                .build();
    }
}
