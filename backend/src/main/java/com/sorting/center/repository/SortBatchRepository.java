package com.sorting.center.repository;

import com.sorting.center.entity.SortBatch;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SortBatchRepository extends JpaRepository<SortBatch, Long> {

    boolean existsByCode(String code);

    List<SortBatch> findByChuteIdAndStatusNot(Long chuteId, String status);

    List<SortBatch> findAllByOrderByIdDesc();

    /** 开袋时锁住批次行，两个人对着同一批次抢余量时在此排队串行化。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from SortBatch b where b.id = :id")
    Optional<SortBatch> findLockById(@Param("id") Long id);
}
