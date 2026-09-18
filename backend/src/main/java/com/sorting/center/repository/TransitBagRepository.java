package com.sorting.center.repository;

import com.sorting.center.entity.TransitBag;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransitBagRepository extends JpaRepository<TransitBag, Long> {

    boolean existsByCode(String code);

    List<TransitBag> findAllByOrderByIdDesc();

    /** 该批次所有还没拆掉的袋（挂在别的装车单上的也算）。 */
    @Query("select coalesce(sum(b.quantity), 0) from TransitBag b "
            + "where b.batchId = :batchId and b.status = '在袋'")
    Long sumActiveQuantityByBatch(@Param("batchId") Long batchId);

    /** 拆袋时行锁，同一袋不能被两个人同时拆掉。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from TransitBag b where b.id = :id")
    Optional<TransitBag> findLockById(@Param("id") Long id);
}
