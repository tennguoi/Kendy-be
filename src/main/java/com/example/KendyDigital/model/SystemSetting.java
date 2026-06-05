package com.example.KendyDigital.model;

import com.example.KendyDigital.common.TimestampedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "system_settings")
public class SystemSetting extends TimestampedEntity {
    @Id
    @Column(name = "setting_key", columnDefinition = "TEXT", nullable = false)
    private String key;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "is_public", nullable = false)
    private boolean publicSetting = false;

    @Column(name = "updated_by")
    private Long updatedBy;

    public SystemSetting(String key, String value, boolean publicSetting, Long updatedBy) {
        this.key = key;
        this.value = value;
        this.publicSetting = publicSetting;
        this.updatedBy = updatedBy;
    }

    public void update(String value, boolean publicSetting, Long updatedBy) {
        this.value = value;
        this.publicSetting = publicSetting;
        this.updatedBy = updatedBy;
    }
}
