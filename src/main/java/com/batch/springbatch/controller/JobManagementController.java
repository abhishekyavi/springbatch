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

@RestController
@RequestMapping("/job-management")
@Tag(name = "Job Management", description = "APIs for managing batch jobs by triggers")
public class JobManagementController {

    @Autowired
    private BatchJobScheduler batchJobScheduler;

    @PostMapping("/trigger-import")
    @Operation(summary = "Trigger Import Job", description = "Triggers the scheduled import job for processing Person data.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Import job executed successfully",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string", example = "Import job completed successfully>>>>>>")
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Import job execution failed",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string", example = "Import job failed:>>>>> Error message")
            )
        )
    })

    public String triggerImportJob() {
        try {
            batchJobScheduler.scheduleImportJob();
            return "Scheduled Import job triggered successfully";
        } catch (Exception e) {
            return "Failed to trigger import job: " + e.getMessage();
        }
    }

    @PostMapping("/trigger-export")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Export job executed successfully",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string", example = "Export job completed successfully>>>>>>")
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Export job execution failed",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string", example = "Export job failed:>>>>> Error message")
            )
        )
    })
    @Operation(summary = "Trigger Export Job", description = "Triggers the scheduled export job for exporting Person data.")
    public String triggerExportJob() {
        try {
            batchJobScheduler.scheduleExportJob();
            return "Scheduled Export job triggered successfully";
        } catch (Exception e) {
            return "Failed to trigger export job: " + e.getMessage();
        }
    }
}