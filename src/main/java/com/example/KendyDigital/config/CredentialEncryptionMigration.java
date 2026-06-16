package com.example.KendyDigital.config;

import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.repository.AccountCredentialRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CredentialEncryptionMigration implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(CredentialEncryptionMigration.class);
    private final AccountCredentialRepository repository;

    public CredentialEncryptionMigration(AccountCredentialRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<AccountCredential> all = repository.findAll();
        long migrated = 0;
        for (AccountCredential credential : all) {
            if (credential.getPayloadHash() == null && credential.getPasswordSecret() != null) {
                credential.updatePayloadHash(payloadHash(credential));
                repository.save(credential);
                migrated++;
            }
        }
        if (migrated > 0) {
            LOG.info("CredentialEncryptionMigration: re-saved {} credentials to apply encryption", migrated);
        } else {
            LOG.info("CredentialEncryptionMigration: all credentials already encrypted");
        }
    }

    private String payloadHash(AccountCredential credential) {
        String normalized = credential.getService().getId() + "|"
                + nullToEmpty(credential.getLoginIdentifier()).toLowerCase(Locale.ROOT).trim() + "|"
                + nullToEmpty(credential.getPasswordSecret()).trim() + "|"
                + nullToEmpty(credential.getRecoveryInfo()).trim() + "|"
                + nullToEmpty(credential.getTwoFactorSecret()).trim();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash credential payload", exception);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
