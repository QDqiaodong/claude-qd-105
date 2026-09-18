package com.sorting.center.repository;

import com.sorting.center.entity.ExceptionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExceptionItemRepository extends JpaRepository<ExceptionItem, Long> {

    boolean existsByCode(String code);

    List<ExceptionItem> findByBatchId(Long batchId);

    List<ExceptionItem> findAllByOrderByIdDesc();

    /** 该批次仍处在待处理的异常件件数（破损/错分/无面单都不许混进袋带走）。 */
    long countByBatchIdAndStatus(Long batchId, String status);
}
