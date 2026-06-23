package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.service.audit.AuditService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditLogController {
    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/api/admin/audit-logs")
    public List<AuditLogResponse> list(@RequestParam(required = false) String action,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return auditService.list(action, page, size);
    }
}
