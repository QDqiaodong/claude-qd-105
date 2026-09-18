package com.sorting.center.service;

import com.sorting.center.dto.BizException;
import com.sorting.center.entity.LoadPlan;
import com.sorting.center.entity.SortBatch;
import com.sorting.center.repository.LoadPlanRepository;
import com.sorting.center.repository.SortBatchRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoadPlanService {

    private final LoadPlanRepository plans;
    private final SortBatchRepository batches;
    private final TransitBagService bagService;

    public LoadPlanService(LoadPlanRepository plans, SortBatchRepository batches,
                           TransitBagService bagService) {
        this.plans = plans;
        this.batches = batches;
        this.bagService = bagService;
    }

    public List<LoadPlan> list(Long batchId, String status) {
        return plans.findAllByOrderByIdDesc().stream()
                .filter(p -> batchId == null || batchId.equals(p.batchId))
                .filter(p -> status == null || status.isEmpty() || status.equals(p.status))
                .toList();
    }

    @Transactional
    public LoadPlan create(LoadPlan input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("装车单号不能为空");
        }
        if (plans.existsByCode(input.code)) {
            throw new BizException("装车单号 " + input.code + " 已经用过了");
        }
        if (input.batchId == null) {
            throw new BizException("请选择装哪个批次");
        }
        if (input.plateNo == null || input.plateNo.isBlank()) {
            throw new BizException("要填车牌号");
        }
        if (input.destination == null || input.destination.isBlank()) {
            throw new BizException("要填目的地");
        }
        if (input.quantity == null || input.quantity <= 0) {
            throw new BizException("装车件数要大于 0");
        }
        SortBatch batch = batches.findById(input.batchId)
                .orElseThrow(() -> new BizException("批次不存在"));
        if (!"已完成".equals(batch.status)) {
            throw new BizException("批次 " + batch.code + " 还在「" + batch.status
                    + "」，分拣完了才能装车");
        }
        if (input.quantity > batch.quantity) {
            throw new BizException("批次 " + batch.code + " 一共 " + batch.quantity
                    + " 件，装不了 " + input.quantity + " 件");
        }
        LoadPlan saved = new LoadPlan();
        saved.code = input.code.trim();
        saved.batchId = batch.id;
        saved.plateNo = input.plateNo.trim();
        saved.destination = input.destination.trim();
        saved.quantity = input.quantity;
        saved.loadDate = input.loadDate;
        saved.operator = input.operator;
        saved.status = "待装车";
        return plans.save(saved);
    }

    @Transactional
    public LoadPlan depart(Long id) {
        // 锁住装车单行：与正在开袋/拆袋/改件数的事务互斥，不会出现发车后还挂上新袋。
        LoadPlan plan = plans.findByIdForUpdate(id)
                .orElseThrow(() -> new BizException("装车单不存在"));
        if (!"待装车".equals(plan.status)) {
            throw new BizException("这单已经发过车了");
        }
        plan.status = "已发车";
        LoadPlan saved = plans.save(plan);
        // 发车即冻结：挂上的袋全部转成已发车，之后改件数、拆袋都被挡住。
        bagService.freezeByPlan(saved);
        return saved;
    }
}
