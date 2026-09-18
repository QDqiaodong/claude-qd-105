package com.sorting.center.repository;

import com.sorting.center.entity.TransitBag;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransitBagRepository extends JpaRepository<TransitBag, Long> {

    boolean existsByCode(String code);

    List<TransitBag> findAllByOrderByIdDesc();

    List<TransitBag> findByBatchId(Long batchId);

    List<TransitBag> findByBatchIdAndStatusIn(Long batchId, List<String> statuses);

    List<TransitBag> findByLoadPlanId(Long loadPlanId);

    /** 行锁：拆袋、改件数与发车抢同一行时互斥。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransitBag t where t.id = :id")
    Optional<TransitBag> findByIdForUpdate(@Param("id") Long id);
}
