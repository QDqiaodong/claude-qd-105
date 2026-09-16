package com.sorting.center.controller;

import com.sorting.center.entity.SortBatch;
import com.sorting.center.service.SortBatchService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SortBatchController {

    private final SortBatchService service;

    public SortBatchController(SortBatchService service) {
        this.service = service;
    }

    @GetMapping("/batches")
    public List<SortBatch> list(@RequestParam(required = false) Long chuteId,
                                @RequestParam(required = false) String status) {
        return service.list(chuteId, status);
    }

    @PostMapping("/batches")
    public SortBatch create(@RequestBody SortBatch input) {
        return service.create(input);
    }

    @PostMapping("/batches/{id}/advance")
    public SortBatch advance(@PathVariable Long id, @RequestParam String action) {
        return service.advance(id, action);
    }
}
