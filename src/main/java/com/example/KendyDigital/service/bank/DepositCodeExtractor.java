package com.example.KendyDigital.service.bank;

import com.example.KendyDigital.config.BankProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DepositCodeExtractor {
    private final Pattern depositCodePattern;

    public DepositCodeExtractor(BankProperties bankProperties) {
        this.depositCodePattern = Pattern.compile("\\b" + Pattern.quote(bankProperties.getTransferPrefix())
                + "[A-Z0-9]{8,}\\b", Pattern.CASE_INSENSITIVE);
    }

    public List<String> extractDepositCodeCandidates(String content, String code) {
        String text = (nullSafe(content) + " " + nullSafe(code)).toUpperCase(Locale.ROOT);
        Matcher matcher = depositCodePattern.matcher(text);
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(matcher.group().toUpperCase(Locale.ROOT));
        }
        return list;
    }

    public Optional<String> extractDepositCode(String content, String code) {
        List<String> candidates = extractDepositCodeCandidates(content, code);
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
