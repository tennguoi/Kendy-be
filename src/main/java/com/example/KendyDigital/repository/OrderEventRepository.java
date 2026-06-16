package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.order.OrderEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    @Query("select e from OrderEvent e join fetch e.order where e.order.id = :orderId order by e.createdAt asc")
    List<OrderEvent> findAllByOrder_IdOrderByCreatedAtAsc(@Param("orderId") Long orderId);
}
