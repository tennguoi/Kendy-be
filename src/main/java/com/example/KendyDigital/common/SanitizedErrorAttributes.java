package com.example.KendyDigital.common;

import com.example.KendyDigital.common.error.ApiError;
import com.example.KendyDigital.common.error.ErrorCode;
import java.util.Map;
import java.util.Locale;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

@Component
public class SanitizedErrorAttributes extends DefaultErrorAttributes {

    private final MessageSource messageSource;

    public SanitizedErrorAttributes(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(
                webRequest,
                options.excluding(
                        ErrorAttributeOptions.Include.EXCEPTION,
                        ErrorAttributeOptions.Include.STACK_TRACE,
                        ErrorAttributeOptions.Include.BINDING_ERRORS));
        attributes.remove("trace");
        attributes.remove("exception");
        attributes.remove("errors");

        Locale locale = webRequest.getLocale();
        Object statusObj = attributes.get("status");
        int status = statusObj instanceof Integer code ? code : 500;

        if (status >= 500) {
            String message = messageSource.getMessage(ErrorCode.INTERNAL.getMessageKey(), null, locale);
            attributes.put("message", message);
            attributes.put("code", ErrorCode.INTERNAL.name());
        } else {
            ErrorCode code = resolveErrorCode(status);
            String message = messageSource.getMessage(code.getMessageKey(), null, locale);
            attributes.put("message", message);
            attributes.put("code", code.name());
        }

        return attributes;
    }

    private ErrorCode resolveErrorCode(int status) {
        if (status == 404) return ErrorCode.NOT_FOUND;
        if (status == 403) return ErrorCode.FORBIDDEN;
        if (status == 401) return ErrorCode.UNAUTHORIZED;
        if (status == 409) return ErrorCode.CONFLICT;
        if (status == 429) return ErrorCode.TOO_MANY_REQUESTS;
        return ErrorCode.BAD_REQUEST;
    }
}
