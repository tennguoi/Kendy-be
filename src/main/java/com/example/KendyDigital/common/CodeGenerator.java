package com.example.KendyDigital.common;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class CodeGenerator {
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(String prefix, int length) {
        StringBuilder builder = new StringBuilder(prefix);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }
}
