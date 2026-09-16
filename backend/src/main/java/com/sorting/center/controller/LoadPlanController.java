package com.sorting.center.controller;

import com.sorting.center.entity.LoadPlan;
import com.sorting.center.service.LoadPlanService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LoadPlanController {

    private final LoadPlanService service;

    public LoadPlanController(LoadPlanService service) {
        this.service = service;
    }

    @GetMapping("/plans")
    public List<LoadPlan> list(@RequestParam(required = false) Long batchId,
                               @RequestParam(required = false) String status) {
        return service.list(batchId, status);
    }

    @PostMapping("/plans")
    public LoadPlan create(@RequestBody LoadPlan input) {
        return service.create(input);
    }

    @PostMapping("/plans/{id}/depart")
    public LoadPlan depart(@PathVariable Long id) {
        return service.depart(id);
    }
}
