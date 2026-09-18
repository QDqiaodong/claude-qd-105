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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransitBagService {

    /** 会占住批次余量的袋：还没拆掉的。发车后冻结，仍占余量。 */
    private static final List<String> ACTIVE_STATUS = List.of("待发车", "已发车");

    private final TransitBagRepository bags;
    private final LoadPlanRepository plans;
    private final SortBatchRepository batches;
    private final ChuteRepository chutes;
    private final ExceptionItemRepository exceptions;

    public TransitBagService(TransitBagRepository bags, LoadPlanRepository plans,
                             SortBatchRepository batches, ChuteRepository chutes,
                             ExceptionItemRepository exceptions) {
        this.bags = bags;
        this.plans = plans;
        this.batches = batches;
        this.chutes = chutes;
        this.exceptions = exceptions;
    }

    public List<TransitBag> list(Long batchId, Long loadPlanId, String status) {
        return bags.findAllByOrderByIdDesc().stream()
                .filter(t -> batchId == null || batchId.equals(t.batchId))
                .filter(t -> loadPlanId == null || loadPlanId.equals(t.loadPlanId))
                .filter(t -> status == null || status.isEmpty() || status.equals(t.status))
                .toList();
    }

    /**
     * 开袋：格口、已完成的分拣批次、待装车的装车单三边一次挂齐。
     * 全方法在一个事务里，先锁批次行再重算余量，并发两笔只有一笔成功。
     */
    @Transactional
    public TransitBag open(TransitBag input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("袋号不能为空");
        }
        if (bags.existsByCode(input.code.trim())) {
            throw new BizException("袋号 " + input.code.trim() + " 已经用过了");
        }
        if (input.loadPlanId == null) {
            throw new BizException("调度规矩：月台上不许有无家可归的袋，得先挂一张待装车的装车单");
        }
        if (input.quantity == null || input.quantity <= 0) {
            throw new BizException("袋里件数要大于 0");
        }

        // 加锁顺序统一为 装车单 -> 批次 -> 格口，与改件数、发车保持一致，避免并发交叉死锁。
        LoadPlan plan = plans.findByIdForUpdate(input.loadPlanId)
                .orElseThrow(() -> new BizException("装车单不存在"));
        if (!"待装车".equals(plan.status)) {
            throw new BizException("装车单 " + plan.code + " 已经「" + plan.status
                    + "」，新袋挂不上去");
        }

        SortBatch batch = batches.findByIdForUpdate(plan.batchId)
                .orElseThrow(() -> new BizException("批次不存在"));
        if (!"已完成".equals(batch.status)) {
            throw new BizException("批次 " + batch.code + " 还停在「" + batch.status
                    + "」，这批货还没分完，抽不进袋");
        }

        Chute chute = chutes.findByIdForUpdate(batch.chuteId)
                .orElseThrow(() -> new BizException("格口不存在"));
        if (!"启用".equals(chute.status)) {
            throw new BizException("格口 " + chute.code + " 现在是「" + chute.status
                    + "」，不能再开新袋");
        }

        // 开袋三边必须对齐：袋挂的格口/批次要和装车单、批次本身对得上。
        if (input.batchId != null && !input.batchId.equals(batch.id)) {
            throw new BizException("袋要装的批次和装车单 " + plan.code + " 的批次对不上");
        }
        if (input.chuteId != null && !input.chuteId.equals(chute.id)) {
            throw new BizException("袋要挂的格口和批次 " + batch.code + " 所在格口对不上");
        }

        long pendingEx = exceptions.countByBatchIdAndStatus(batch.id, "待处理");
        int used = sumActiveQuantity(batch.id);
        int available = batch.quantity - (int) pendingEx - used;
        if (input.quantity > available) {
            throw new BizException("批次 " + batch.code + " 开不成：登记 " + batch.quantity
                    + " 件，待处理异常件扣掉 " + pendingEx + " 件，其它没拆的袋已占 " + used
                    + " 件，此刻只剩 " + Math.max(available, 0) + " 件，装不进 "
                    + input.quantity + " 件");
        }

        TransitBag saved = new TransitBag();
        saved.code = input.code.trim();
        saved.chuteId = chute.id;
        saved.batchId = batch.id;
        saved.loadPlanId = plan.id;
        saved.quantity = input.quantity;
        saved.bagDate = input.bagDate;
        saved.operator = input.operator;
        saved.status = "待发车";
        return bags.save(saved);
    }

    /** 发车前拆袋重打：发车后冻结，不能拆。拆掉后余量归还批次。 */
    @Transactional
    public TransitBag tearDown(Long id) {
        // 先无锁读出口袋归属，再按统一顺序 装车单 -> 袋 加锁，和开袋/改件数/发车不交叉死锁。
        Long planId = bags.findById(id)
                .orElseThrow(() -> new BizException("中转袋不存在"))
                .loadPlanId;
        LoadPlan plan = plans.findByIdForUpdate(planId)
                .orElseThrow(() -> new BizException("装车单不存在"));
        TransitBag bag = bags.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("中转袋不存在"));
        if ("已发车".equals(plan.status)) {
            throw new BizException("装车单 " + plan.code + " 已经发车，挂上的袋冻结了，不能拆");
        }
        if ("已拆除".equals(bag.status)) {
            throw new BizException("袋 " + bag.code + " 已经拆掉了");
        }
        bag.status = "已拆除";
        return bags.save(bag);
    }

    /** 发车前可改件数；发车后冻结。改完仍要过余量与异常件扣减校验。 */
    @Transactional
    public TransitBag updateQuantity(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BizException("袋里件数要大于 0");
        }
        // 无锁读出归属，再按统一顺序 装车单 -> 批次 -> 袋 加锁。
        TransitBag ref = bags.findById(id).orElseThrow(() -> new BizException("中转袋不存在"));
        LoadPlan plan = plans.findByIdForUpdate(ref.loadPlanId)
                .orElseThrow(() -> new BizException("装车单不存在"));
        if (!"待装车".equals(plan.status)) {
            throw new BizException("袋 " + ref.code + " 挂的装车单已经发车，件数冻住了不能改");
        }
        SortBatch batch = batches.findByIdForUpdate(plan.batchId)
                .orElseThrow(() -> new BizException("批次不存在"));
        TransitBag bag = bags.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("中转袋不存在"));
        if ("已拆除".equals(bag.status)) {
            throw new BizException("袋 " + bag.code + " 已经拆掉了，不能改件数，重开一袋吧");
        }
        long pendingEx = exceptions.countByBatchIdAndStatus(batch.id, "待处理");
        int usedByOthers = sumActiveQuantity(batch.id) - bag.quantity;
        int available = batch.quantity - (int) pendingEx - usedByOthers;
        if (quantity > available) {
            throw new BizException("批次 " + batch.code + " 改不成：登记 " + batch.quantity
                    + " 件，待处理异常件扣掉 " + pendingEx + " 件，其它没拆的袋已占 "
                    + usedByOthers + " 件，此刻只剩 " + Math.max(available, 0)
                    + " 件，填不到 " + quantity + " 件");
        }
        bag.quantity = quantity;
        return bags.save(bag);
    }

    /** 装车单发车：把挂在这单上的袋一并冻结成「已发车」。 */
    @Transactional
    public void freezeByPlan(LoadPlan plan) {
        List<TransitBag> onPlan = bags.findByLoadPlanId(plan.id);
        for (TransitBag bag : onPlan) {
            if ("待发车".equals(bag.status)) {
                bag.status = "已发车";
                bags.save(bag);
            }
        }
    }

    private int sumActiveQuantity(Long batchId) {
        return bags.findByBatchIdAndStatusIn(batchId, ACTIVE_STATUS).stream()
                .mapToInt(b -> b.quantity)
                .sum();
    }
}
