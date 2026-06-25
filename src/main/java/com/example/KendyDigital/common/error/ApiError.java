package com.example.KendyDigital.common.error;

import java.time.Instant;
import java.util.List;

public class ApiError {
    private final String timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final List<FieldError> details;

    public ApiError(int status, String error, String code, String message, List<FieldError> details) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public String getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public List<FieldError> getDetails() { return details; }

    public static ApiError of(int status, String error, String code, String message) {
        return new ApiError(status, error, code, message, null);
    }

    public static ApiError withDetails(int status, String error, String code, String message, List<FieldError> details) {
        return new ApiError(status, error, code, message, details);
    }

    public static class FieldError {
        private final String field;
        private final String rejectedValue;
        private final String message;

        public FieldError(String field, String rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }

        public String getField() { return field; }
        public String getRejectedValue() { return rejectedValue; }
        public String getMessage() { return message; }
    }
}
