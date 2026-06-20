package com.example.KendyDigital.controller;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/time")
public class ServerTimeController {
    private final Clock clock;

    public ServerTimeController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping
    Map<String, Object> getServerTime() {
        Instant now = clock.instant();
        return Map.of("epochMillis", now.toEpochMilli(), "iso", now.toString());
    }
}
