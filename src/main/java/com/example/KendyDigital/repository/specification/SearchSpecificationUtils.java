package com.example.KendyDigital.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.Locale;

public final class SearchSpecificationUtils {

    private SearchSpecificationUtils() {
    }

    public static String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String likePattern(String query) {
        String normalized = normalizeQuery(query);
        return normalized == null ? null : "%" + normalized.toLowerCase(Locale.ROOT) + "%";
    }

    public static Long parseLongOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Predicate likeLower(CriteriaBuilder cb, Expression<String> expression, String pattern) {
        return cb.like(cb.lower(cb.coalesce(expression, "")), pattern);
    }
}
