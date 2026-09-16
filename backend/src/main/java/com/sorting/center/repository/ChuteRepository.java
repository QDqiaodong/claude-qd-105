package com.sorting.center.repository;

import com.sorting.center.entity.Chute;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChuteRepository extends JpaRepository<Chute, Long> {

    boolean existsByCode(String code);

    List<Chute> findAllByOrderByIdAsc();
}
