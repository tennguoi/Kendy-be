package com.example.KendyDigital.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

@Configuration
public class OAuth2ClientConfig {
    @Bean
    ClientRegistrationRepository clientRegistrationRepository(AppOAuth2Properties properties) {
        List<ClientRegistration> registrations = new ArrayList<>();
        if (configured(properties.getGoogle())) {
            registrations.add(ClientRegistration.withRegistrationId("google")
                    .clientId(properties.getGoogle().getClientId())
                    .clientSecret(properties.getGoogle().getClientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email")
                    .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth?prompt=select_account")
                    .tokenUri("https://oauth2.googleapis.com/token")
                    .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .userNameAttributeName(IdTokenClaimNames.SUB)
                    .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                    .clientName("Google")
                    .build());
        }
        if (configured(properties.getGithub())) {
            registrations.add(ClientRegistration.withRegistrationId("github")
                    .clientId(properties.getGithub().getClientId())
                    .clientSecret(properties.getGithub().getClientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
                    .scope("read:user", "user:email")
                    .authorizationUri("https://github.com/login/oauth/authorize")
                    .tokenUri("https://github.com/login/oauth/access_token")
                    .userInfoUri("https://api.github.com/user")
                    .userNameAttributeName("id")
                    .clientName("GitHub")
                    .build());
        }
        return new OptionalClientRegistrationRepository(registrations);
    }

    @Bean
    @ConditionalOnMissingBean
    OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
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
