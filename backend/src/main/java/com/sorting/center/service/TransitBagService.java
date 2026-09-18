package com.sorting.center.service;

import com.sorting.center.dto.BizException;
import com.sorting.center.entity.Chute;
import com.sorting.center.entity.LoadPlan;
import com.sorting.center.entity.SortBatch;
import com.sorting.center.entity.TransitBag;
import com.sorting.center.repository.ChuteRepository;
import com.sorting.center.repository.ExceptionItemRepository;
import com.sorting.center.repository.LoadPlanRepository;
import com.sorting.center.repository.SortBatchRepository;
import com.sorting.center.repository.TransitBagRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransitBagService {

    private final TransitBagRepository bags;
    private final ChuteRepository chutes;
    private final SortBatchRepository batches;
    private final LoadPlanRepository plans;
    private final ExceptionItemRepository exceptions;

    public TransitBagService(TransitBagRepository bags,
                             ChuteRepository chutes,
                             SortBatchRepository batches,
                             LoadPlanRepository plans,
                             ExceptionItemRepository exceptions) {
        this.bags = bags;
        this.chutes = chutes;
        this.batches = batches;
        this.plans = plans;
        this.exceptions = exceptions;
    }

    public List<TransitBag> list(Long chuteId, Long batchId, Long planId, String status) {
        return bags.findAllByOrderByIdDesc().stream()
                .filter(b -> chuteId == null || chuteId.equals(b.chuteId))
                .filter(b -> batchId == null || batchId.equals(b.batchId))
                .filter(b -> planId == null || planId.equals(b.planId))
                .filter(b -> status == null || status.isEmpty() || status.equals(b.status))
                .toList();
    }

    /**
     * 开袋：格口、已完成的分拣批次、待装车的装车单三边一次挂齐。
     *
     * READ_COMMITTED 是为了让锁内的余量汇总一定读到同行已提交的占用：
     * 两个人同时对着同一批次开袋时，第二个事务在批次行锁上排队，
     * 拿到锁后汇总能看到第一笔已打的袋，不会两笔都写成功把件数打超。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransitBag openBag(TransitBag input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("袋号不能为空");
        }
        if (bags.existsByCode(input.code)) {
            throw new BizException("袋号 " + input.code + " 已经用过了");
        }
        if (input.chuteId == null) {
            throw new BizException("请选在哪个格口开袋");
        }
        if (input.batchId == null) {
            throw new BizException("请选装哪个分拣批次");
        }
        if (input.planId == null) {
            // 调度规矩：月台上不许有无家可归的袋，必须先有待装车单才允许开袋
            throw new BizException("请先挂一张待装车的装车单，没有单不能开袋");
        }
        if (input.quantity == null || input.quantity <= 0) {
            throw new BizException("袋内件数要大于 0");
        }

        // 按 格口 → 批次 → 装车单 的固定顺序加行锁，防止并发开袋/发车/停用互相打架
        Chute chute = chutes.findLockById(input.chuteId)
                .orElseThrow(() -> new BizException("格口不存在"));
        SortBatch batch = batches.findLockById(input.batchId)
                .orElseThrow(() -> new BizException("批次不存在"));
        LoadPlan plan = plans.findLockById(input.planId)
                .orElseThrow(() -> new BizException("装车单不存在"));

        if (!"启用".equals(chute.status)) {
            throw new BizException("格口 " + chute.code + " 现在是「" + chute.status
                    + "」，不能再开新袋");
        }
        if (!"已完成".equals(batch.status)) {
            throw new BizException("批次 " + batch.code + " 还在「" + batch.status
                    + "」，分拣完了才能抽件打袋");
        }
        if (!batch.chuteId.equals(chute.id)) {
            throw new BizException("批次 " + batch.code + " 是格口 " + batch.chuteId
                    + " 的货，不能挂到格口 " + chute.code + " 的袋上");
        }
        if (!plan.batchId.equals(batch.id)) {
            throw new BizException("装车单 " + plan.code + " 装的不是批次 "
                    + batch.code + " 的货，三边挂不齐");
        }
        if (!"待装车".equals(plan.status)) {
            throw new BizException("装车单 " + plan.code + " 已经发车，新袋挂不上去");
        }

        // 袋内件数 + 该批次其它没拆掉的袋 + 仍待处理的异常件，不得超过批次登记总件数
        long activeQuantity = bags.sumActiveQuantityByBatch(batch.id);
        long pendingExceptions = exceptions.countByBatchIdAndStatus(batch.id, "待处理");
        long remaining = (long) batch.quantity - activeQuantity - pendingExceptions;
        if ((long) input.quantity > remaining) {
            throw new BizException("批次 " + batch.code + " 登记 " + batch.quantity
                    + " 件，其他未拆的袋已占 " + activeQuantity + " 件，待处理异常件 "
                    + pendingExceptions + " 件，此刻只剩 " + remaining
                    + " 件，打不了 " + input.quantity
                    + " 件；把袋拆小或等异常件处理完再开");
        }

        TransitBag saved = new TransitBag();
        saved.code = input.code.trim();
        saved.chuteId = chute.id;
        saved.batchId = batch.id;
        saved.planId = plan.id;
        saved.quantity = input.quantity;
        saved.bagDate = input.bagDate;
        saved.operator = input.operator;
        saved.status = "在袋";
        return bags.save(saved);
    }

    /** 发车前拆袋重打：台账保留，状态置为已拆袋，占用的余量释放回去。 */
    @Transactional
    public TransitBag unpack(Long id) {
        TransitBag bag = bags.findLockById(id).orElseThrow(() -> new BizException("中转袋不存在"));
        LoadPlan plan = plans.findLockById(bag.planId)
                .orElseThrow(() -> new BizException("装车单不存在"));
        if (!"在袋".equals(bag.status)) {
            throw new BizException("袋 " + bag.code + " 已经拆掉了，不能再拆");
        }
        if ("已发车".equals(plan.status)) {
            throw new BizException("装车单 " + plan.code + " 已发车，袋已冻结，不能拆");
        }
        bag.status = "已拆袋";
        return bags.save(bag);
    }

    /** 开袋前看一眼这个批次还能打几件（仅展示用，真正的扣减在开袋事务里锁着算）。 */
    public Map<String, Object> remaining(Long batchId) {
        if (batchId == null) {
            throw new BizException("请选批次");
        }
        SortBatch batch = batches.findById(batchId)
                .orElseThrow(() -> new BizException("批次不存在"));
        long activeQuantity = bags.sumActiveQuantityByBatch(batch.id);
        long pendingExceptions = exceptions.countByBatchIdAndStatus(batch.id, "待处理");
        long remaining = (long) batch.quantity - activeQuantity - pendingExceptions;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("batchId", batch.id);
        body.put("batchCode", batch.code);
        body.put("batchStatus", batch.status);
        body.put("totalQuantity", batch.quantity);
        body.put("activeQuantity", activeQuantity);
        body.put("pendingExceptionQuantity", pendingExceptions);
        body.put("remaining", remaining);
        return body;
    }
}
