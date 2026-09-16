package com.sorting.center.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 异常件：破了、分错了、没面单的，单独登记跟进。 */
@Entity
@Table(name = "exception_item")
public class ExceptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32, unique = true)
    public String code;

    @Column(name = "batch_id", nullable = false)
    public Long batchId;

    /** 破损 / 错分 / 无面单 */
    @Column(nullable = false, length = 16)
    public String kind;

    @Column(length = 255)
    public String description;

    @Column(name = "found_date", nullable = false)
    public LocalDate foundDate;

    @Column(nullable = false, length = 32)
    public String handler;

    /** 待处理 / 已处理 */
    @Column(nullable = false, length = 16)
    public String status;
}
