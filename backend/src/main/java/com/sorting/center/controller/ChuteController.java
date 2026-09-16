package com.sorting.center.controller;

import com.sorting.center.entity.Chute;
import com.sorting.center.service.ChuteService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChuteController {

    private final ChuteService service;

    public ChuteController(ChuteService service) {
        this.service = service;
    }

    @GetMapping("/chutes")
    public List<Chute> list(@RequestParam(required = false) String area,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String keyword) {
        return service.list(area, status, keyword);
    }

    @PostMapping("/chutes")
    public Chute create(@RequestBody Chute input) {
        return service.create(input);
    }

    @PutMapping("/chutes/{id}")
    public Chute update(@PathVariable Long id, @RequestBody Chute input) {
        return service.update(id, input);
    }
}
