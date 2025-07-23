package com.batch.springbatch.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.batch.springbatch.scheduler.BatchJobScheduler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@RestController
@RequestMapping("/job-management")
@Tag(name = "Job Management", description = "APIs for managing batch jobs by triggers")
public class JobManagementController {

    private static final Logger logger = LoggerFactory.getLogger(JobManagementController.class);

    @Autowired
    private BatchJobScheduler batchJobScheduler;

    @PostMapping("/trigger-import")
    @Operation(summary = "Trigger Import Job", description = "Triggers the scheduled import job for processing Person data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Import job executed successfully", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Import job completed successfully>>>>>>"))),
            @ApiResponse(responseCode = "500", description = "Import job execution failed", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Import job failed:>>>>> Error message")))
    })

    public String triggerImportJob() {
        try {
            MDC.put("jobName", "importPersonJob");
            logger.info("Import job trigger requested via REST API");
            batchJobScheduler.scheduleImportJob();
            logger.info("Import job triggered successfully via REST API");
            return "Scheduled Import job triggered successfully";
        } catch (Exception e) {
            logger.error("Failed to trigger import job via REST API: {}", e.getMessage(), e);
            return "Failed to trigger import job: " + e.getMessage();
        } finally {
            MDC.clear();
        }

    }

    @PostMapping("/trigger-export")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export job executed successfully", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Export job completed successfully>>>>>>"))),
            @ApiResponse(responseCode = "500", description = "Export job execution failed", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Export job failed:>>>>> Error message")))
    })
    @Operation(summary = "Trigger Export Job", description = "Triggers the scheduled export job for exporting Person data.")
    public String triggerExportJob() {
        try {
            MDC.put("jobName", "exportPersonJob");
            logger.info("Export job trigger requested via REST API");
            batchJobScheduler.scheduleExportJob();
            logger.info("Export job triggered successfully via REST API");
            return "Scheduled Export job triggered successfully";
        } catch (Exception e) {
            logger.error("Failed to trigger export job via REST API: {}", e.getMessage(), e);
            return "Failed to trigger export job: " + e.getMessage();
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/test-logs")
    @Operation(summary = "Test Log Generation", description = "Generates test logs to verify logging configuration.")
    public String testLogs() {
        try {
            MDC.put("jobName", "testJob");
            logger.info("Test log message - INFO level");
            logger.warn("Test log message - WARN level");
            logger.error("Test log message - ERROR level");

            // Test with different job names
            MDC.put("jobName", "importPersonJob");
            logger.info("Test import job log message");

            MDC.put("jobName", "exportPersonJob");
            logger.info("Test export job log message");

            return "Test logs generated successfully. Check Grafana for logs with labels: application=springbatch, job=testJob/importPersonJob/exportPersonJob";
        } catch (Exception e) {
            logger.error("Failed to generate test logs: {}", e.getMessage(), e);
            return "Failed to generate test logs: " + e.getMessage();
        } finally {
            MDC.clear();
        }
    }
}
