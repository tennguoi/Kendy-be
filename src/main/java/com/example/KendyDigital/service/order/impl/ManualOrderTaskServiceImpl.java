package com.example.KendyDigital.service.order.impl;

import com.example.KendyDigital.dto.order.request.ManualOrderTaskRequest;
import com.example.KendyDigital.dto.order.request.ManualOrderTaskStatusRequest;
import com.example.KendyDigital.dto.order.response.ManualOrderTaskResponse;
import com.example.KendyDigital.model.catalog.ServiceType;
import com.example.KendyDigital.model.order.ManualOrderTask;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.ManualOrderTaskRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.order.ManualOrderTaskService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManualOrderTaskServiceImpl implements ManualOrderTaskService {
    private final ManualOrderTaskRepository manualOrderTaskRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public ManualOrderTaskServiceImpl(
            ManualOrderTaskRepository manualOrderTaskRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService) {
        this.manualOrderTaskRepository = manualOrderTaskRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualOrderTaskResponse> findTasksForOrder(OrderRecord order) {
        if (order.getService() == null || order.getService().getType() != ServiceType.MANUAL) {
            return List.of();
        }
        return manualOrderTaskRepository.findAllByOrder_IdOrderBySortOrderAscIdAsc(order.getId())
                .stream()
                .map(ManualOrderTaskResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void createDefaultTasks(OrderRecord order) {
        if (!manualOrderTaskRepository.findAllByOrder_IdOrderBySortOrderAscIdAsc(order.getId()).isEmpty()) {
            return;
        }
        List<String> titles = List.of(
                "Xác nhận brief/yêu cầu",
                "Kiểm tra điều kiện xử lý",
                "Chốt scope, giá và deadline",
                "Triển khai dịch vụ",
                "Gửi kết quả cho khách",
                "Nghiệm thu/hoàn tất");
        for (int i = 0; i < titles.size(); i++) {
            manualOrderTaskRepository.save(new ManualOrderTask(order, titles.get(i), i + 1));
        }
    }

    @Override
    @Transactional
    public void syncTasks(OrderRecord order, UserAccount admin, List<ManualOrderTaskRequest> taskRequests) {
        if (taskRequests == null) {
            return;
        }
        List<Long> keptIds = new ArrayList<>();
        int index = 1;
        for (ManualOrderTaskRequest taskRequest : taskRequests) {
            String title = blankToNull(taskRequest.title());
            if (title == null) {
                continue;
            }
            ManualOrderTask task = null;
            if (taskRequest.id() != null) {
                task = manualOrderTaskRepository.findById(taskRequest.id()).orElse(null);
                if (task != null && !task.getOrder().getId().equals(order.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manual task belongs to another order");
                }
            }
            if (task == null) {
                task = manualOrderTaskRepository.save(new ManualOrderTask(order, title,
                        taskRequest.sortOrder() == null ? index : taskRequest.sortOrder()));
            } else {
                task.update(title, taskRequest.sortOrder() == null ? index : taskRequest.sortOrder());
            }
            if (Boolean.TRUE.equals(taskRequest.completed()) && !task.isCompleted()) {
                task.markCompleted(admin);
            } else if (Boolean.FALSE.equals(taskRequest.completed()) && task.isCompleted()) {
                task.reopen();
            }
            keptIds.add(task.getId());
            index++;
        }
        if (keptIds.isEmpty()) {
            manualOrderTaskRepository.deleteAllByOrder_Id(order.getId());
        } else {
            manualOrderTaskRepository.deleteAllByOrder_IdAndIdNotIn(order.getId(), keptIds);
        }
    }

    @Override
    @Transactional
    public void updateTaskStatus(OrderRecord order, Long adminUserId, Long taskId, ManualOrderTaskStatusRequest request) {
        if (order.getService() == null || order.getService().getType() != ServiceType.MANUAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only manual service orders have tasks");
        }
        ManualOrderTask task = manualOrderTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manual task not found"));
        if (!task.getOrder().getId().equals(order.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Manual task not found");
        }
        UserAccount admin = userAccountRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (Boolean.TRUE.equals(request.completed())) {
            task.markCompleted(admin);
        } else {
            task.reopen();
        }
        auditService.recordAdmin(adminUserId, "ORDER_MANUAL_TASK_UPDATED", "ORDER", order.getId(),
                "taskId=" + taskId + ",completed=" + request.completed());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
