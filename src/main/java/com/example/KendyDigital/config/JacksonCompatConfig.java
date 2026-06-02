package com.example.KendyDigital.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JacksonCompatConfig {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
