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
public class ChuteService {

    private final ChuteRepository chutes;
    private final SortBatchRepository batches;

    public ChuteService(ChuteRepository chutes, SortBatchRepository batches) {
        this.chutes = chutes;
        this.batches = batches;
    }

    public List<Chute> list(String area, String status, String keyword) {
        return chutes.findAllByOrderByIdAsc().stream()
                .filter(c -> area == null || area.isEmpty() || area.equals(c.area))
                .filter(c -> status == null || status.isEmpty() || status.equals(c.status))
                .filter(c -> keyword == null || keyword.isEmpty()
                        || c.code.contains(keyword) || c.area.contains(keyword))
                .toList();
    }

    @Transactional
    public Chute create(Chute input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("格口编号不能为空");
        }
        if (chutes.existsByCode(input.code)) {
            throw new BizException("格口编号 " + input.code + " 已经用过了");
        }
        if (input.area == null || input.area.isBlank()) {
            throw new BizException("要填这个格口对应哪个片区");
        }
        if (input.capacity == null || input.capacity <= 0) {
            throw new BizException("格口容量要大于 0 件");
        }
        Chute saved = new Chute();
        saved.code = input.code.trim();
        saved.area = input.area.trim();
        saved.capacity = input.capacity;
        saved.status = (input.status == null || input.status.isBlank()) ? "启用" : input.status;
        return chutes.save(saved);
    }

    @Transactional
    public Chute update(Long id, Chute input) {
        Chute c = chutes.findLockById(id).orElseThrow(() -> new BizException("格口不存在"));
        List<SortBatch> running = batches.findByChuteIdAndStatusNot(c.id, "已完成");
        if (input.area != null && !input.area.isBlank()) {
            c.area = input.area.trim();
        }
        if (input.capacity != null && !input.capacity.equals(c.capacity)) {
            if (input.capacity <= 0) {
                throw new BizException("格口容量要大于 0 件");
            }
            c.capacity = input.capacity;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(c.status)) {
            if (!"启用".equals(input.status) && !running.isEmpty()) {
                throw new BizException("这个格口还有 " + running.size()
                        + " 个批次没分完，先分完才能停用或者报修");
            }
            c.status = input.status;
        }
        return chutes.save(c);
    }
}
