package com.example.KendyDigital.service.security.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.KendyDigital.config.SecurityMonitorProperties;
import org.junit.jupiter.api.Test;

class GeoIpServiceTest {

    private final GeoIpService service = new GeoIpService(new SecurityMonitorProperties());

    @Test
    void nonIpLiteralReturnsUnknownWithoutLookup() {
        GeoIpService.GeoInfo info = service.lookup("unknown");
        assertNull(info.country());
        assertNull(info.asn());
        org.junit.jupiter.api.Assertions.assertFalse(info.known());
    }

    @Test
    void loopbackIsClassifiedAsPrivate() {
        GeoIpService.GeoInfo info = service.lookup("127.0.0.1");
        assertEquals("LO", info.country());
        assertEquals("PRIVATE", info.asn());
    }

    @Test
    void disabledGeoIpReturnsUnknownForPublicIp() {
        GeoIpService.GeoInfo info = service.lookup("8.8.8.8");
        assertNull(info.country());
        org.junit.jupiter.api.Assertions.assertFalse(info.known());
    }
}
