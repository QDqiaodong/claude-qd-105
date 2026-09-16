package com.sorting.center.entity;

import jakarta.persistence.*;

/** 分拣格口：包裹按目的片区滑到不同的格口里。 */
@Entity
@Table(name = "chute")
public class Chute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32, unique = true)
    public String code;

    /** 目的片区，例如 华东 / 华中 / 同城 */
    @Column(nullable = false, length = 32)
    public String area;

    /** 这个格口一次最多堆多少件 */
    @Column(nullable = false)
    public Integer capacity;

    /** 启用 / 停用 / 维修 */
    @Column(nullable = false, length = 16)
    public String status;
}
