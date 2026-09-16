package com.sorting.center.controller;

import com.sorting.center.entity.ExceptionItem;
import com.sorting.center.service.ExceptionService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ExceptionController {

    private final ExceptionService service;

    public ExceptionController(ExceptionService service) {
        this.service = service;
    }

    @GetMapping("/exceptions")
    public List<ExceptionItem> list(@RequestParam(required = false) Long batchId,
                                    @RequestParam(required = false) String kind,
                                    @RequestParam(required = false) String status) {
        return service.list(batchId, kind, status);
    }

    @PostMapping("/exceptions")
    public ExceptionItem create(@RequestBody ExceptionItem input) {
        return service.create(input);
    }

    @PostMapping("/exceptions/{id}/resolve")
    public ExceptionItem resolve(@PathVariable Long id) {
        return service.resolve(id);
    }
}
