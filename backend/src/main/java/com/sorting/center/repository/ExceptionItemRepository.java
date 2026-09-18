package com.sorting.center.repository;

import com.sorting.center.entity.ExceptionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExceptionItemRepository extends JpaRepository<ExceptionItem, Long> {

    boolean existsByCode(String code);

    List<ExceptionItem> findByBatchId(Long batchId);

    List<ExceptionItem> findAllByOrderByIdDesc();

    /** 该批次还没处理的异常件条数：破损/错分/无面单，一件都不许打进袋。 */
    long countByBatchIdAndStatus(Long batchId, String status);
}
