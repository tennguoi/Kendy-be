package com.example.KendyDigital.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.StoredFile;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
