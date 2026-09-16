package com.sorting.center.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 装车发运：分拣完的包裹装上干线车发走。 */
@Entity
@Table(name = "load_plan")
public class LoadPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32, unique = true)
    public String code;

    @Column(name = "batch_id", nullable = false)
    public Long batchId;

    @Column(name = "plate_no", nullable = false, length = 16)
    public String plateNo;

    /** 目的地 */
    @Column(nullable = false, length = 32)
    public String destination;

    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "load_date", nullable = false)
    public LocalDate loadDate;

    /** 待装车 / 已发车 */
    @Column(nullable = false, length = 16)
    public String status;

    @Column(nullable = false, length = 32)
    public String operator;
}
