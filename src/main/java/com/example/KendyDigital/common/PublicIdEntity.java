package com.example.KendyDigital.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * Lớp cơ sở cấp định danh công khai (public_id) bằng UUIDv4 cho mọi Entity.
 * Long id vẫn là khóa chính nội bộ; public_id là định danh lộ ra API.
 */
@MappedSuperclass
public abstract class PublicIdEntity {

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId = UUID.randomUUID();

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }
}
