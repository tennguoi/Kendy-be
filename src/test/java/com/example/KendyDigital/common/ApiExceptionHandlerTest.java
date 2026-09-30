package com.example.KendyDigital.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.KendyDigital.common.error.ApiError;
import com.example.KendyDigital.common.error.ErrorCode;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTest {
    private MessageSource messageSource;
    private ApiExceptionHandler handler;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        messageSource = mock(MessageSource.class);
        handler = new ApiExceptionHandler(messageSource);
        request = mock(WebRequest.class);
        when(request.getLocale()).thenReturn(Locale.forLanguageTag("vi"));
    }

    @Test
    void testWrongOldPasswordResolvesToAuthOldPasswordIncorrect() {
        when(messageSource.getMessage(eq(ErrorCode.AUTH_OLD_PASSWORD_INCORRECT.getMessageKey()), any(), eq(Locale.forLanguageTag("vi"))))
                .thenReturn("Mật khẩu hiện tại không đúng.");

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        ResponseEntity<ApiError> response = handler.handleResponseStatus(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("AUTH_OLD_PASSWORD_INCORRECT", response.getBody().getCode());
        assertEquals("Mật khẩu hiện tại không đúng.", response.getBody().getMessage());
    }

    @Test
    void testSamePasswordResolvesToAuthNewPasswordSame() {
        when(messageSource.getMessage(eq(ErrorCode.AUTH_NEW_PASSWORD_SAME.getMessageKey()), any(), eq(Locale.forLanguageTag("vi"))))
                .thenReturn("Mật khẩu mới phải khác mật khẩu hiện tại.");

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "New password must be different");
        ResponseEntity<ApiError> response = handler.handleResponseStatus(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("AUTH_NEW_PASSWORD_SAME", response.getBody().getCode());
        assertEquals("Mật khẩu mới phải khác mật khẩu hiện tại.", response.getBody().getMessage());
    }

    @Test
    void testSpecificCustomReasonPreservedForGenericBadRequest() {
        when(messageSource.getMessage(eq(ErrorCode.BAD_REQUEST.getMessageKey()), any(), eq(Locale.forLanguageTag("vi"))))
                .thenReturn("Yêu cầu không hợp lệ.");

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tài khoản không hợp lệ");
        ResponseEntity<ApiError> response = handler.handleResponseStatus(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().getCode());
        assertEquals("Tên tài khoản không hợp lệ", response.getBody().getMessage());
    }
}
