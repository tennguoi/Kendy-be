package com.example.KendyDigital.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.KendyDigital.config.AppSecurityProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private ClientIpResolver resolver(boolean trustCloudflare) {
        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setTrustedProxies(List.of("10.0.0.1"));
        properties.setTrustCloudflare(trustCloudflare);
        return new ClientIpResolver(properties);
    }

    @Test
    void ignoresForwardedForFromUntrustedPeer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("8.8.8.8");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertEquals("8.8.8.8", resolver(false).resolve(request));
    }

    @Test
    void trustsForwardedForFromTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 10.0.0.1");

        assertEquals("1.2.3.4", resolver(false).resolve(request));
    }

    @Test
    void trustsCloudflareConnectingIpWhenEnabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "9.9.9.9");
        request.addHeader("CF-Connecting-IP", "5.5.5.5");

        assertEquals("5.5.5.5", resolver(true).resolve(request));
    }

    @Test
    void matchesCidrBlocks() {
        ClientIpResolver resolver = resolver(false);
        org.junit.jupiter.api.Assertions.assertTrue(resolver.matches("203.0.113.0/24", "203.0.113.55"));
        org.junit.jupiter.api.Assertions.assertFalse(resolver.matches("203.0.113.0/24", "203.0.114.1"));
    }
}
