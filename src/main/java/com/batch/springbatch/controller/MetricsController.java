package com.batch.springbatch.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batch.springbatch.service.BatchMetricsService;

import io.micrometer.core.instrument.MeterRegistry;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    @Autowired
    private BatchMetricsService batchMetricsService;

    @Autowired
    private MeterRegistry meterRegistry;

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshMetrics() {
        try {
            batchMetricsService.updateMetrics();
            batchMetricsService.updateDetailedMetrics();
            return ResponseEntity.ok("Metrics refreshed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error refreshing metrics: " + e.getMessage());
        }
    }

    @GetMapping("/batch")
    public ResponseEntity<Map<String, Object>> getBatchMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Get all batch-related metrics
            meterRegistry.getMeters().stream()
                    .filter(meter -> meter.getId().getName().startsWith("batch_"))
                    .forEach(meter -> {
                        String name = meter.getId().getName();
                        String tags = meter.getId().getTags().stream()
                                .map(tag -> tag.getKey() + "=" + tag.getValue())
                                .collect(Collectors.joining(","));
                        
                        String key = tags.isEmpty() ? name : name + "{" + tags + "}";
                        
                        if (meter instanceof io.micrometer.core.instrument.Gauge) {
                            metrics.put(key, ((io.micrometer.core.instrument.Gauge) meter).value());
                        }
                    });
            
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to retrieve metrics: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/prometheus")
    public ResponseEntity<String> getPrometheusMetrics() {
        return ResponseEntity.ok("Visit /actuator/prometheus for Prometheus format metrics");
    }
}
