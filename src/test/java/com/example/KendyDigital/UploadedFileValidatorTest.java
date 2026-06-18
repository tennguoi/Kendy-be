package com.example.KendyDigital;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.KendyDigital.service.file.UploadedFileValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class UploadedFileValidatorTest {
    private final UploadedFileValidator validator = new UploadedFileValidator();

    @Test
    void acceptsPngWithMatchingSignature() {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1};
        var upload = validator.validate(new MockMultipartFile(
                "file", "image.png", "image/png", png));

        assertEquals("image.png", upload.fileName());
    }

    @Test
    void rejectsExecutableDisguisedAsText() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.txt", "text/plain", "MZ executable".getBytes());

        assertThrows(ResponseStatusException.class, () -> validator.validate(file));
    }

    @Test
    void rejectsPathTraversalFileName() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../image.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

        assertThrows(ResponseStatusException.class, () -> validator.validate(file));
    }
}
