package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.config.SecurityMonitorProperties;
import com.example.KendyDigital.model.security.SecurityEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecuritySignalServiceImpl implements SecuritySignalService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecuritySignalServiceImpl.class);

    private final SecurityMonitorProperties properties;
    private final SecurityEventWriter eventWriter;
    private final RiskScoringService riskScoringService;
    private final DetectionRuleEngine ruleEngine;
    private final GeoIpService geoIpService;
    private final MeterRegistry meterRegistry;

    public SecuritySignalServiceImpl(SecurityMonitorProperties properties, SecurityEventWriter eventWriter,
            RiskScoringService riskScoringService, DetectionRuleEngine ruleEngine, GeoIpService geoIpService,
            MeterRegistry meterRegistry) {
        this.properties = properties;
        this.eventWriter = eventWriter;
        this.riskScoringService = riskScoringService;
        this.ruleEngine = ruleEngine;
        this.geoIpService = geoIpService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public void record(SecuritySignal signal) {
        if (!properties.isEnabled() || signal == null || signal.type() == null) {
            return;
        }
        try {
            counter("kendy_security_events_total", signal);
            GeoIpService.GeoInfo geo = geoIpService.lookup(signal.ip());
            SecurityEvent event = new SecurityEvent(
                    signal.type(),
                    signal.severity(),
                    signal.ip(),
                    signal.userId(),
                    signal.sessionId(),
                    signal.method(),
                    signal.path(),
                    truncate(signal.userAgent(), 512),
                    null,
                    signal.metadata());
            event.setCountry(geo.country());
            event.setAsn(geo.asn());
            eventWriter.write(event);

            if (signal.riskPoints() > 0) {
                double ipRisk = riskScoringService.addIpRisk(signal.ip(), signal.riskPoints());
                double userRisk = riskScoringService.addUserRisk(signal.userId(), signal.riskPoints());
                ruleEngine.onRiskChanged(signal, ipRisk, userRisk);
            }
            ruleEngine.evaluate(signal);
        } catch (RuntimeException exception) {
            LOGGER.debug("Failed to process security signal type={}", signal.type(), exception);
        }
    }

    private void counter(String name, SecuritySignal signal) {
        try {
            meterRegistry.counter(name,
                    "type", signal.type().name(),
                    "severity", signal.severity() == null ? "UNKNOWN" : signal.severity().name())
                    .increment();
        } catch (RuntimeException ignored) {
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
