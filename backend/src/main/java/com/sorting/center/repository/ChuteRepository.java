package com.sorting.center.repository;

import com.sorting.center.entity.Chute;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChuteRepository extends JpaRepository<Chute, Long> {

    boolean existsByCode(String code);

    List<Chute> findAllByOrderByIdAsc();

    /** 行锁：开袋校合格口状态时锁住格口，避免与停用/报修并发。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Chute c where c.id = :id")
    Optional<Chute> findByIdForUpdate(@Param("id") Long id);
}
