package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.order.ManualOrderTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManualOrderTaskRepository extends JpaRepository<ManualOrderTask, Long> {
    List<ManualOrderTask> findAllByOrder_IdOrderBySortOrderAscIdAsc(Long orderId);

    @Modifying
    @Query("delete from ManualOrderTask t where t.order.id = :orderId and t.id not in :ids")
    void deleteAllByOrder_IdAndIdNotIn(@Param("orderId") Long orderId, @Param("ids") List<Long> ids);

    @Modifying
    @Query("delete from ManualOrderTask t where t.order.id = :orderId")
    void deleteAllByOrder_Id(@Param("orderId") Long orderId);
}
