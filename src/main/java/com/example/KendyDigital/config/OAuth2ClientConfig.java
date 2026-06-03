package com.example.KendyDigital.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class OAuth2ClientConfig {
    @Bean
    ClientRegistrationRepository clientRegistrationRepository(AppOAuth2Properties properties) {
        List<ClientRegistration> registrations = new ArrayList<>();
        if (configured(properties.getGoogle())) {
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(properties.getGoogle().getClientId())
                    .clientSecret(properties.getGoogle().getClientSecret())
                    .scope("openid", "profile", "email")
                    .build());
        }
        if (configured(properties.getGithub())) {
            registrations.add(CommonOAuth2Provider.GITHUB.getBuilder("github")
                    .clientId(properties.getGithub().getClientId())
                    .clientSecret(properties.getGithub().getClientSecret())
                    .scope("read:user", "user:email")
                    .build());
        }
        return new OptionalClientRegistrationRepository(registrations);
    }

    private boolean configured(AppOAuth2Properties.Client client) {
        return client.getClientId() != null && !client.getClientId().isBlank()
                && client.getClientSecret() != null && !client.getClientSecret().isBlank();
    }

    private static class OptionalClientRegistrationRepository
            implements ClientRegistrationRepository, Iterable<ClientRegistration> {
        private final Map<String, ClientRegistration> registrations;

        OptionalClientRegistrationRepository(List<ClientRegistration> registrations) {
            this.registrations = registrations.stream()
                    .collect(Collectors.toUnmodifiableMap(ClientRegistration::getRegistrationId,
                            Function.identity()));
        }

        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return registrations.get(registrationId);
        }

        @Override
        public Iterator<ClientRegistration> iterator() {
            return registrations.values().iterator();
        }
    }
}
