package com.sorting.center.service;

import com.sorting.center.dto.BizException;
import com.sorting.center.entity.ExceptionItem;
import com.sorting.center.entity.SortBatch;
import com.sorting.center.repository.ExceptionItemRepository;
import com.sorting.center.repository.SortBatchRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExceptionService {

    private final ExceptionItemRepository items;
    private final SortBatchRepository batches;

    public ExceptionService(ExceptionItemRepository items, SortBatchRepository batches) {
        this.items = items;
        this.batches = batches;
    }

    public List<ExceptionItem> list(Long batchId, String kind, String status) {
        return items.findAllByOrderByIdDesc().stream()
                .filter(i -> batchId == null || batchId.equals(i.batchId))
                .filter(i -> kind == null || kind.isEmpty() || kind.equals(i.kind))
                .filter(i -> status == null || status.isEmpty() || status.equals(i.status))
                .toList();
    }

    @Transactional
    public ExceptionItem create(ExceptionItem input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("异常件单号不能为空");
        }
        if (items.existsByCode(input.code)) {
            throw new BizException("异常件单号 " + input.code + " 已经用过了");
        }
        if (input.batchId == null) {
            throw new BizException("请选择这个异常件属于哪个批次");
        }
        if (input.kind == null || input.kind.isBlank()) {
            throw new BizException("要选异常类型");
        }
        if (input.description == null || input.description.isBlank()) {
            throw new BizException("要说清是什么异常");
        }
        if (input.foundDate == null) {
            throw new BizException("要填发现日期");
        }
        SortBatch batch = batches.findById(input.batchId)
                .orElseThrow(() -> new BizException("批次不存在"));
        ExceptionItem saved = new ExceptionItem();
        saved.code = input.code.trim();
        saved.batchId = batch.id;
        saved.kind = input.kind;
        saved.description = input.description;
        saved.foundDate = input.foundDate;
        saved.handler = input.handler;
        saved.status = "待处理";
        return items.save(saved);
    }

    @Transactional
    public ExceptionItem resolve(Long id) {
        ExceptionItem item = items.findById(id).orElseThrow(() -> new BizException("异常件记录不存在"));
        if (!"待处理".equals(item.status)) {
            throw new BizException("这件已经处理过了");
        }
        item.status = "已处理";
        return items.save(item);
    }
}
