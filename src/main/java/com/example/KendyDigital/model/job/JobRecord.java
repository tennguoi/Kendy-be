package com.example.KendyDigital.model.job;

import com.example.KendyDigital.common.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "job_records",
        indexes = {
                @Index(name = "idx_job_records_status", columnList = "status")
        })
public class JobRecord extends TimestampedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String logs;

    public JobRecord(String name, String status, String logs) {
        this.name = name;
        this.status = status;
        this.logs = logs;
    }

    public void retry() {
        this.status = "RETRY_REQUESTED";
        this.logs = appendLog("Retry requested");
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.logs = appendLog("Cancelled");
    }

    private String appendLog(String line) {
        String prefix = this.logs == null || this.logs.isBlank() ? "" : this.logs + "\n";
        return prefix + java.time.Instant.now() + " " + line;
    }
}
