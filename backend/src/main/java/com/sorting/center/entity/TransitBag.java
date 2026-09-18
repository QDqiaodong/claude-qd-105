package com.sorting.center.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 中转袋：分完的包裹打成一袋，挂到一张还没发车的装车单上带走。 */
@Entity
@Table(name = "transit_bag")
public class TransitBag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 袋号 TB-xxxx，全库唯一 */
    @Column(nullable = false, length = 32, unique = true)
    public String code;

    /** 开袋时挂齐的格口 */
    @Column(name = "chute_id", nullable = false)
    public Long chuteId;

    /** 开袋时挂齐的分拣批次 */
    @Column(name = "batch_id", nullable = false)
    public Long batchId;

    /** 开袋时挂齐的待装车装车单（调度定的规矩：先有单才能开袋） */
    @Column(name = "load_plan_id", nullable = false)
    public Long loadPlanId;

    /** 袋内件数 */
    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "bag_date", nullable = false)
    public LocalDate bagDate;

    /** 待发车 / 已发车 / 已拆除 */
    @Column(nullable = false, length = 16)
    public String status;

    @Column(nullable = false, length = 32)
    public String operator;
}
