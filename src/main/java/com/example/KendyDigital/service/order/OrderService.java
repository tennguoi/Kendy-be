package com.example.KendyDigital.service.order;

import com.example.KendyDigital.dto.order.request.AdminOrderUpdateRequest;
import com.example.KendyDigital.dto.order.request.BulkRefundOrdersRequest;
import com.example.KendyDigital.dto.order.request.CancelOrderRequest;
import com.example.KendyDigital.dto.order.request.CreateOrderRequest;
import com.example.KendyDigital.dto.order.request.ExtendOrderRequest;
import com.example.KendyDigital.dto.order.request.ManualOrderWorkflowRequest;
import com.example.KendyDigital.dto.order.request.OrderNoteRequest;
import com.example.KendyDigital.dto.order.request.RefundOrderRequest;
import com.example.KendyDigital.dto.order.request.ReprocessOrderRequest;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.model.order.OrderStatus;
import java.util.List;

public interface OrderService {
    OrderResponse create(Long userId, CreateOrderRequest request);
    OrderResponse createForCheckout(Long userId, CreateOrderRequest request, Long checkoutId);
    List<OrderResponse> listByUser(Long userId, OrderStatus status);
    List<OrderResponse> listByUser(Long userId, OrderStatus status, int page, int size);
    List<OrderResponse> searchForUser(Long userId, String query, OrderStatus status, int page, int size);
    List<OrderResponse> listForAdmin(OrderStatus status, Long userId);
    List<OrderResponse> listForAdmin(OrderStatus status, Long userId, Integer limit);
    List<OrderResponse> searchForAdmin(String query, OrderStatus status, Long userId, Integer limit);
    OrderResponse getByCodeForUser(Long userId, String orderCode);
    OrderResponse getByCodeForAdmin(String orderCode);
    OrderResponse cancelForUser(Long userId, String orderCode, CancelOrderRequest request);
    OrderResponse reorder(Long userId, String orderCode);
    OrderResponse complete(String orderCode, Long adminUserId, AdminOrderUpdateRequest request);
    OrderResponse fail(String orderCode, Long adminUserId, AdminOrderUpdateRequest request);
    OrderResponse cancelByAdmin(String orderCode, Long adminUserId, CancelOrderRequest request);
    OrderResponse refund(String orderCode, Long adminUserId, RefundOrderRequest request);
    OrderResponse updateAdminNote(String orderCode, Long adminUserId, OrderNoteRequest request);
    OrderResponse updateUserNote(String orderCode, Long adminUserId, OrderNoteRequest request);
    OrderResponse extend(String orderCode, Long adminUserId, ExtendOrderRequest request);
    OrderResponse reprocess(String orderCode, Long adminUserId, ReprocessOrderRequest request);
    OrderResponse updateManualWorkflow(String orderCode, Long adminUserId, ManualOrderWorkflowRequest request);
    List<OrderResponse> bulkRefund(Long adminUserId, BulkRefundOrdersRequest request);
}
