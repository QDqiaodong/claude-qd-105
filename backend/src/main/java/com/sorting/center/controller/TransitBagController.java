package com.sorting.center.controller;

import com.sorting.center.entity.TransitBag;
import com.sorting.center.service.TransitBagService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransitBagController {

    private final TransitBagService service;

    public TransitBagController(TransitBagService service) {
        this.service = service;
    }

    @GetMapping("/bags")
    public List<TransitBag> list(@RequestParam(required = false) Long batchId,
                                 @RequestParam(required = false) Long loadPlanId,
                                 @RequestParam(required = false) String status) {
        return service.list(batchId, loadPlanId, status);
    }

    @PostMapping("/bags")
    public TransitBag open(@RequestBody TransitBag input) {
        return service.open(input);
    }

    @PostMapping("/bags/{id}/teardown")
    public TransitBag tearDown(@PathVariable Long id) {
        return service.tearDown(id);
    }

    @PutMapping("/bags/{id}/quantity")
    public TransitBag updateQuantity(@PathVariable Long id,
                                     @RequestParam Integer quantity) {
        return service.updateQuantity(id, quantity);
    }
}
