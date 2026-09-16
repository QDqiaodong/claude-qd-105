package com.sorting.center.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 分拣批次：一车到港的包裹按片区分拣。 */
@Entity
@Table(name = "sort_batch")
public class SortBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32, unique = true)
    public String code;

    @Column(name = "chute_id", nullable = false)
    public Long chuteId;

    /** 本批次件数 */
    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "arrive_date", nullable = false)
    public LocalDate arriveDate;

    /** 待分拣 / 分拣中 / 已完成 */
    @Column(nullable = false, length = 16)
    public String status;

    @Column(nullable = false, length = 32)
    public String operator;
}
