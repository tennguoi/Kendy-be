package com.example.KendyDigital.config;

import com.example.KendyDigital.common.MaintenanceModeFilter;
import com.example.KendyDigital.common.RateLimitFilter;
import com.example.KendyDigital.common.RequestIdFilter;
import com.example.KendyDigital.common.SecurityHeadersFilter;
import com.example.KendyDigital.common.ServerTimeFilter;
import com.example.KendyDigital.security.ApiKeyAuthenticationFilter;
import com.example.KendyDigital.security.BearerTokenAuthenticationFilter;
import com.example.KendyDigital.security.OAuth2AuthenticationFailureHandler;
import com.example.KendyDigital.security.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
        @Bean
        @Order(1)
        SecurityFilterChain oauth2Security(HttpSecurity http,
                        OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                        OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler)
                        throws Exception {
                http.securityMatcher("/oauth2/**", "/login/oauth2/**")
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                                .oauth2Login(oauth2 -> oauth2
                                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                                .failureHandler(oAuth2AuthenticationFailureHandler))
                                .httpBasic(basic -> basic.disable())
                                .formLogin(form -> form.disable());
                return http.build();
        }

        @Bean
        @Order(2)
        SecurityFilterChain apiSecurity(HttpSecurity http,
                        BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
                        ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                        RequestIdFilter requestIdFilter, RateLimitFilter rateLimitFilter,
                        MaintenanceModeFilter maintenanceModeFilter, ServerTimeFilter serverTimeFilter,
                        SecurityHeadersFilter securityHeadersFilter)
                        throws Exception {
                http.csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/ws/**").permitAll()
                                                .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                                                                "/v3/api-docs/**")
                                                .hasRole("SUPER_ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/time").permitAll()
                                                .requestMatchers("/api/auth/register", "/api/auth/login",
                                                                "/api/auth/2fa/email-code",
                                                                "/api/auth/oauth2/2fa/verify",
                                                                "/api/auth/oauth2/providers",
                                                                "/api/auth/forgot-password",
                                                                "/api/auth/verify-password-reset",
                                                                "/api/auth/reset-password",
                                                                "/api/auth/resend-verification",
                                                                "/api/auth/verify-email")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/webhooks/sepay",
                                                                "/api/webhooks/sepay/")
                                                .permitAll()
                                                .requestMatchers("/api/webhooks/sepay/**").permitAll()
                                                .requestMatchers("/api/services", "/api/services/**",
                                                                "/api/pricing", "/api/pricing/**",
                                                                "/api/service-categories", "/api/service-categories/**",
                                                                "/api/content", "/api/content/**")
                                                .permitAll()
                                                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                                                .requestMatchers("/api/**").authenticated()
                                                .anyRequest().permitAll())
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.deny())
                                                .contentSecurityPolicy(csp -> csp.policyDirectives(
                                                                "default-src 'self'; frame-ancestors 'none'"))
                                                .httpStrictTransportSecurity(
                                                                hsts -> hsts.includeSubDomains(true).preload(true))
                                                .contentTypeOptions(contentTypeOptions -> contentTypeOptions.disable()) // We'll add custom header via filter
                                                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                                .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(serverTimeFilter, RequestIdFilter.class)
                                .addFilterBefore(maintenanceModeFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(bearerTokenAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class)
                                .httpBasic(basic -> basic.disable())
                                .formLogin(form -> form.disable());

                return http.build();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource(AppSecurityProperties properties) {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(properties.getCorsAllowedOrigins());
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "X-Api-Key", "Accept-Language"));
                configuration.setAllowCredentials(true);
                configuration.addExposedHeader(RequestIdFilter.REQUEST_ID_HEADER);
                configuration.addExposedHeader(ServerTimeFilter.SERVER_TIME_HEADER);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
