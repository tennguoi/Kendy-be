package com.example.KendyDigital.model;

import com.example.KendyDigital.model.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelValidationArchitectureTest {

    @Test
    @DisplayName("Tất cả các trường String trong @Entity bắt buộc phải khai báo rõ độ dài hoặc là TEXT")
    void allEntityStringFieldsMustHaveExplicitLengthOrText() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        List<String> violations = new ArrayList<>();

        for (BeanDefinition bd : scanner.findCandidateComponents("com.example.KendyDigital.model")) {
            Class<?> clazz = Class.forName(bd.getBeanClassName());
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.getType().equals(String.class)) {
                    Column col = field.getAnnotation(Column.class);
                    Lob lob = field.getAnnotation(Lob.class);

                    boolean isText = false;
                    boolean hasExplicitLength = false;

                    if (lob != null) {
                        isText = true;
                    }

                    if (col != null) {
                        if (col.columnDefinition() != null && col.columnDefinition().toUpperCase().contains("TEXT")) {
                            isText = true;
                        }
                        // Default length in @Column is 255
                        // If length was explicitly set (including 255 if intended, or other numbers)
                        // but if col is null or no column annotation, it is a violation
                        if (col.length() > 0) {
                            hasExplicitLength = true;
                        }
                    }

                    if (col == null && lob == null) {
                        violations.add(String.format("Entity [%s] field [%s] thiếu @Column(length = ...)",
                                clazz.getSimpleName(), field.getName()));
                    } else if (!isText && !hasExplicitLength) {
                        violations.add(String.format("Entity [%s] field [%s] thiếu độ dài hoặc TEXT",
                                clazz.getSimpleName(), field.getName()));
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Phát hiện các trường String trong @Entity chưa được khai báo độ dài:\n"
                        + String.join("\n", violations));
    }

    @Test
    @DisplayName("Trường SĐT trong UserAccount và các Request DTO bắt buộc giới hạn tối đa 11 ký tự")
    void phoneFieldMustBeRestrictedTo11Chars() throws NoSuchFieldException {
        Field userPhoneField = UserAccount.class.getDeclaredField("phone");
        Column phoneCol = userPhoneField.getAnnotation(Column.class);
        org.junit.jupiter.api.Assertions.assertNotNull(phoneCol, "Trường phone trong UserAccount phải có @Column");
        org.junit.jupiter.api.Assertions.assertEquals(11, phoneCol.length(), "Độ dài cột phone trong UserAccount phải là 11");

        // Kiểm tra DTO AuthRegisterRequest
        Field authPhoneField = com.example.KendyDigital.dto.auth.request.AuthRegisterRequest.class.getDeclaredField("phone");
        jakarta.validation.constraints.Size authPhoneSize = authPhoneField.getAnnotation(jakarta.validation.constraints.Size.class);
        org.junit.jupiter.api.Assertions.assertNotNull(authPhoneSize, "AuthRegisterRequest.phone phải có @Size");
        org.junit.jupiter.api.Assertions.assertEquals(11, authPhoneSize.max(), "AuthRegisterRequest.phone max size phải là 11");

        // Kiểm tra DTO UpdateProfileRequest
        Field updateProfilePhoneField = com.example.KendyDigital.dto.user.request.UpdateProfileRequest.class.getDeclaredField("phone");
        jakarta.validation.constraints.Size profilePhoneSize = updateProfilePhoneField.getAnnotation(jakarta.validation.constraints.Size.class);
        org.junit.jupiter.api.Assertions.assertNotNull(profilePhoneSize, "UpdateProfileRequest.phone phải có @Size");
        org.junit.jupiter.api.Assertions.assertEquals(11, profilePhoneSize.max(), "UpdateProfileRequest.phone max size phải là 11");
    }
}
