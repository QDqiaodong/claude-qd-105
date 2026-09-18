package com.sorting.center.repository;

import com.sorting.center.entity.SortBatch;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SortBatchRepository extends JpaRepository<SortBatch, Long> {

    boolean existsByCode(String code);

    List<SortBatch> findByChuteIdAndStatusNot(Long chuteId, String status);

    List<SortBatch> findAllByOrderByIdDesc();

    /** 行锁：同一批次并发动袋时靠它把交易串起来，后到的一笔重算余量。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from SortBatch b where b.id = :id")
    Optional<SortBatch> findByIdForUpdate(@Param("id") Long id);
}
