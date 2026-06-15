package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.file.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
