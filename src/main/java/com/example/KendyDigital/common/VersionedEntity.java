package com.example.KendyDigital.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class VersionedEntity extends TimestampedEntity {
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public Long getVersion() {
        return version != null ? version : 0L;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
