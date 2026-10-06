package com.example.KendyDigital.model.security;

public enum SecuritySeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public boolean atLeast(SecuritySeverity other) {
        return ordinal() >= other.ordinal();
    }
}
