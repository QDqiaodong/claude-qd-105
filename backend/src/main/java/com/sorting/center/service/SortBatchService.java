package com.sorting.center.service;

import com.sorting.center.dto.BizException;
import com.sorting.center.entity.Chute;
import com.sorting.center.entity.SortBatch;
import com.sorting.center.repository.ChuteRepository;
import com.sorting.center.repository.SortBatchRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SortBatchService {

    private final SortBatchRepository batches;
    private final ChuteRepository chutes;

    public SortBatchService(SortBatchRepository batches, ChuteRepository chutes) {
        this.batches = batches;
        this.chutes = chutes;
    }

    public List<SortBatch> list(Long chuteId, String status) {
        return batches.findAllByOrderByIdDesc().stream()
                .filter(b -> chuteId == null || chuteId.equals(b.chuteId))
                .filter(b -> status == null || status.isEmpty() || status.equals(b.status))
                .toList();
    }

    @Transactional
    public SortBatch create(SortBatch input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("批次号不能为空");
        }
        if (batches.existsByCode(input.code)) {
            throw new BizException("批次号 " + input.code + " 已经用过了");
        }
        if (input.chuteId == null) {
            throw new BizException("请选择分到哪个格口");
        }
        if (input.quantity == null || input.quantity <= 0) {
            throw new BizException("批次件数要大于 0");
        }
        Chute chute = chutes.findById(input.chuteId)
                .orElseThrow(() -> new BizException("格口不存在"));
        if (!"启用".equals(chute.status)) {
            throw new BizException("格口 " + chute.code + " 现在是「" + chute.status + "」，不能往这儿分");
        }
        if (input.quantity > chute.capacity) {
            throw new BizException("格口 " + chute.code + " 一次最多堆 " + chute.capacity
                    + " 件，这批有 " + input.quantity + " 件，堆不下");
        }
        SortBatch saved = new SortBatch();
        saved.code = input.code.trim();
        saved.chuteId = chute.id;
        saved.quantity = input.quantity;
        saved.arriveDate = input.arriveDate;
        saved.operator = input.operator;
        saved.status = "待分拣";
        return batches.save(saved);
    }

    @Transactional
    public SortBatch advance(Long id, String action) {
        SortBatch batch = batches.findById(id).orElseThrow(() -> new BizException("批次不存在"));
        if ("start".equals(action)) {
            if (!"待分拣".equals(batch.status)) {
                throw new BizException("只有待分拣的批次能开工，现在是 " + batch.status);
            }
            batch.status = "分拣中";
        } else if ("done".equals(action)) {
            if (!"分拣中".equals(batch.status)) {
                throw new BizException("只有分拣中的批次能收工，现在是 " + batch.status);
            }
            batch.status = "已完成";
        } else {
            throw new BizException("不认识的动作：" + action);
        }
        return batches.save(batch);
    }
}
