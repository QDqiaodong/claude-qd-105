package com.sorting.center.repository;

import com.sorting.center.entity.LoadPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoadPlanRepository extends JpaRepository<LoadPlan, Long> {

    boolean existsByCode(String code);

    List<LoadPlan> findAllByOrderByIdDesc();
}
