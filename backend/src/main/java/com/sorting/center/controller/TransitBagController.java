package com.sorting.center.controller;

import com.sorting.center.entity.TransitBag;
import com.sorting.center.service.TransitBagService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransitBagController {

    private final TransitBagService service;

    public TransitBagController(TransitBagService service) {
        this.service = service;
    }

    @GetMapping("/bags")
    public List<TransitBag> list(@RequestParam(required = false) Long chuteId,
                                 @RequestParam(required = false) Long batchId,
                                 @RequestParam(required = false) Long planId,
                                 @RequestParam(required = false) String status) {
        return service.list(chuteId, batchId, planId, status);
    }

    @PostMapping("/bags")
    public TransitBag openBag(@RequestBody TransitBag input) {
        return service.openBag(input);
    }

    @PostMapping("/bags/{id}/unpack")
    public TransitBag unpack(@PathVariable Long id) {
        return service.unpack(id);
    }

    @GetMapping("/bags/remaining")
    public Map<String, Object> remaining(@RequestParam Long batchId) {
        return service.remaining(batchId);
    }
}
