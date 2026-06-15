package com.example.KendyDigital.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonCompatConfig {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
