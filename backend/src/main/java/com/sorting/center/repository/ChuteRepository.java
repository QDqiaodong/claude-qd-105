package com.sorting.center.repository;

import com.sorting.center.entity.Chute;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChuteRepository extends JpaRepository<Chute, Long> {

    boolean existsByCode(String code);

    List<Chute> findAllByOrderByIdAsc();

    /** 开袋与停用/报修格口互斥，避免刚开袋格口就被报修。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Chute c where c.id = :id")
    Optional<Chute> findLockById(@Param("id") Long id);
}
