package com.example.KendyDigital.service.order;

import com.example.KendyDigital.dto.order.request.ManualOrderTaskRequest;
import com.example.KendyDigital.dto.order.request.ManualOrderTaskStatusRequest;
import com.example.KendyDigital.dto.order.response.ManualOrderTaskResponse;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.user.UserAccount;
import java.util.List;
import java.util.UUID;

public interface ManualOrderTaskService {
    List<ManualOrderTaskResponse> findTasksForOrder(OrderRecord order);
    void createDefaultTasks(OrderRecord order);
    void syncTasks(OrderRecord order, UserAccount admin, List<ManualOrderTaskRequest> taskRequests);
    void updateTaskStatus(OrderRecord order, Long adminUserId, UUID taskId, ManualOrderTaskStatusRequest request);
}
