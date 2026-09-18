package com.sorting.center.repository;

import com.sorting.center.entity.LoadPlan;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoadPlanRepository extends JpaRepository<LoadPlan, Long> {

    boolean existsByCode(String code);

    List<LoadPlan> findAllByOrderByIdDesc();

    /** 发车与开袋/拆袋互斥：装车单行在哪个事务里先锁住，另一边就得等。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LoadPlan p where p.id = :id")
    Optional<LoadPlan> findLockById(@Param("id") Long id);
}
