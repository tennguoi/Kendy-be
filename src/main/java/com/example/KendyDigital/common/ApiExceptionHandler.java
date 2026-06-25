package com.example.KendyDigital.common;

import com.example.KendyDigital.common.error.ApiError;
import com.example.KendyDigital.common.error.ApiError.FieldError;
import com.example.KendyDigital.common.error.ErrorCode;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final MessageSource messageSource;

    public ApiExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception, WebRequest request) {
        Locale locale = request.getLocale();
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String reason = exception.getReason();
        ErrorCode code = resolveErrorCode(status, reason);
        String message = resolveMessage(code, locale, reason);
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(), status.getReasonPhrase(), code.name(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, WebRequest request) {
        Locale locale = request.getLocale();
        BindingResult bindingResult = exception.getBindingResult();
        String defaultMessage = messageSource.getMessage(ErrorCode.VALIDATION.getMessageKey(), null, locale);

        List<FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(error -> new FieldError(
                        error.getField(),
                        String.valueOf(error.getRejectedValue()),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());

        String summary = defaultMessage + " (" + fieldErrors.size() + " trường)";
        return ResponseEntity.badRequest().body(ApiError.withDetails(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ErrorCode.VALIDATION.name(),
                summary,
                fieldErrors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception, WebRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage(ErrorCode.BAD_REQUEST.getMessageKey(), null, locale);
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ErrorCode.BAD_REQUEST.name(),
                message + ": " + exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception, WebRequest request) {
        Locale locale = request.getLocale();
        String detail = exception.getMostSpecificCause().getMessage();
        ErrorCode code;
        if (detail != null && detail.contains("unique")) {
            code = ErrorCode.DATA_UNIQUE_CONSTRAINT;
        } else if (detail != null && detail.contains("foreign key")) {
            code = ErrorCode.DATA_FOREIGN_KEY_CONSTRAINT;
        } else {
            code = ErrorCode.DATA_INVALID;
        }
        String message = messageSource.getMessage(code.getMessageKey(), null, locale);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                code.name(),
                message));
    }

    private ErrorCode resolveErrorCode(HttpStatus status, String reason) {
        if (reason == null) {
            switch (status) {
                case NOT_FOUND: return ErrorCode.NOT_FOUND;
                case FORBIDDEN: return ErrorCode.FORBIDDEN;
                case UNAUTHORIZED: return ErrorCode.UNAUTHORIZED;
                case CONFLICT: return ErrorCode.CONFLICT;
                case TOO_MANY_REQUESTS: return ErrorCode.TOO_MANY_REQUESTS;
                default: return ErrorCode.BAD_REQUEST;
            }
        }
        String upper = reason.toUpperCase();
        if (upper.contains("2FA") || upper.contains("TWO FACTOR")) return ErrorCode.AUTH_TWO_FACTOR_REQUIRED;
        if (upper.contains("INSUFFICIENT") || upper.contains("BALANCE")) return ErrorCode.WALLET_INSUFFICIENT;
        if (upper.contains("NOT FOUND")) return ErrorCode.NOT_FOUND;
        if (upper.contains("UNAUTHORIZED")) return ErrorCode.UNAUTHORIZED;
        if (upper.contains("FORBIDDEN")) return ErrorCode.FORBIDDEN;
        if (upper.contains("INVALID")) return ErrorCode.BAD_REQUEST;
        if (upper.contains("EXPIRED")) return ErrorCode.AUTH_TOKEN_EXPIRED;
        return ErrorCode.BAD_REQUEST;
    }

    private String resolveMessage(ErrorCode code, Locale locale, String fallback) {
        String localized = messageSource.getMessage(code.getMessageKey(), null, locale);
        if (localized != null && !localized.equals(code.getMessageKey())) {
            return localized;
        }
        return fallback != null ? fallback : localized;
    }
}
