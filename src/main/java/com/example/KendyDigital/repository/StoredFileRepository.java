package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.file.StoredFile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByPublicId(UUID publicId);
}
