package com.example.KendyDigital.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.AuditLogResponse;
import com.example.KendyDigital.service.AuditService;

@RestController
public class AuditLogController {
    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/api/admin/audit-logs")
    public List<AuditLogResponse> list(@RequestParam(required = false) String action) {
        return auditService.list(action);
    }
}
