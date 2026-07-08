package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.order.ManualOrderTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManualOrderTaskRepository extends JpaRepository<ManualOrderTask, Long> {
    List<ManualOrderTask> findAllByOrder_IdOrderBySortOrderAscIdAsc(Long orderId);
    void deleteAllByOrder_IdAndIdNotIn(Long orderId, List<Long> ids);
    void deleteAllByOrder_Id(Long orderId);
}
