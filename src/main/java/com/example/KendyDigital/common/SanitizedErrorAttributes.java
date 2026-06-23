package com.example.KendyDigital.common;

import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

@Component
public class SanitizedErrorAttributes extends DefaultErrorAttributes {
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
        Object status = attributes.get("status");
        if (status instanceof Integer code && code >= 500) {
            attributes.put("message", "Internal server error");
        }
        return attributes;
    }
}
