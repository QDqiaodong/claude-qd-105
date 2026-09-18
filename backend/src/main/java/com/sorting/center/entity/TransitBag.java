package com.sorting.center.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 中转袋：分完的包裹打成袋子，挂到一张还没发车的装车单上。
 * 开袋时一次把格口、已完成的分拣批次、待装车的装车单三边挂齐。
 * 发车前可以拆袋重打（状态置为已拆袋，台账保留）；发车后冻结。
 */
@Entity
@Table(name = "transit_bag")
public class TransitBag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32, unique = true)
    public String code;

    @Column(name = "chute_id", nullable = false)
    public Long chuteId;

    @Column(name = "batch_id", nullable = false)
    public Long batchId;

    @Column(name = "plan_id", nullable = false)
    public Long planId;

    /** 袋里写的件数 */
    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "bag_date", nullable = false)
    public LocalDate bagDate;

    @Column(nullable = false, length = 32)
    public String operator;

    /** 在袋 / 已拆袋 */
    @Column(nullable = false, length = 16)
    public String status;
}
