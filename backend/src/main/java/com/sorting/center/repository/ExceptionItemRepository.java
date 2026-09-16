package com.sorting.center.repository;

import com.sorting.center.entity.ExceptionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExceptionItemRepository extends JpaRepository<ExceptionItem, Long> {

    boolean existsByCode(String code);

    List<ExceptionItem> findByBatchId(Long batchId);

    List<ExceptionItem> findAllByOrderByIdDesc();
}
