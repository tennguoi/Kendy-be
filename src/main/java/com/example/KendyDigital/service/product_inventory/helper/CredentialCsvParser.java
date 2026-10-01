package com.example.KendyDigital.service.product_inventory.helper;

import com.example.KendyDigital.dto.inventory.request.CreateAccountCredentialRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CredentialCsvParser {

    public List<CreateAccountCredentialRequest> parseCsv(String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            return List.of();
        }
        List<CreateAccountCredentialRequest> rows = new ArrayList<>();
        String[] lines = csvContent.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        int start = 0;
        if (lines.length > 0 && lines[0].toLowerCase(Locale.ROOT).contains("login")) {
            start = 1;
        }
        for (int i = start; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            List<String> columns = parseCsvLine(lines[i]);
            rows.add(new CreateAccountCredentialRequest(
                    column(columns, 0),
                    column(columns, 1),
                    column(columns, 2),
                    column(columns, 3),
                    column(columns, 4),
                    column(columns, 5),
                    null,
                    null));
        }
        return rows;
    }

    private List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                columns.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        columns.add(current.toString().trim());
        return columns;
    }

    private String column(List<String> columns, int index) {
        if (index < columns.size()) {
            String value = columns.get(index);
            return value == null || value.isBlank() ? null : value.trim();
        }
        return null;
    }
}
