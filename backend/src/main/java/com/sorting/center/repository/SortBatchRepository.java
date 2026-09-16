package com.sorting.center.repository;

import com.sorting.center.entity.SortBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SortBatchRepository extends JpaRepository<SortBatch, Long> {

    boolean existsByCode(String code);

    List<SortBatch> findByChuteIdAndStatusNot(Long chuteId, String status);

    List<SortBatch> findAllByOrderByIdDesc();
}
