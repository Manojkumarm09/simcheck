package com.manoj.simcheck.controller;

import com.manoj.simcheck.model.CompareRequest;
import com.manoj.simcheck.model.CompareResponse;
import com.manoj.simcheck.service.SimilarityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // fine for local dev; tighten before any real deployment
public class CompareController {

    @Autowired private SimilarityService similarityService;

    @PostMapping("/compare")
    public CompareResponse compare(@RequestBody CompareRequest request) {
        return similarityService.compare(request);
    }

    @GetMapping("/health")
    public String health() {
        return "SimCheck backend is running";
    }
}
