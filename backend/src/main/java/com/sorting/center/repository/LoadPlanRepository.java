package com.sorting.center.repository;

import com.sorting.center.entity.LoadPlan;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoadPlanRepository extends JpaRepository<LoadPlan, Long> {

    boolean existsByCode(String code);

    List<LoadPlan> findAllByOrderByIdDesc();

    /** 行锁：发车与开袋/拆袋/改件数抢同一行时，发车结果互斥可见。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LoadPlan p where p.id = :id")
    Optional<LoadPlan> findByIdForUpdate(@Param("id") Long id);
}
