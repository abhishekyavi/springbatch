package com.batch.springbatch.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.batch.springbatch.scheduler.BatchJobScheduler;

@RestController
@RequestMapping("/job-management")
public class JobManagementController {

    @Autowired
    private BatchJobScheduler batchJobScheduler;

    @PostMapping("/trigger-import")
    public String triggerImportJob() {
        try {
            batchJobScheduler.scheduleImportJob();
            return "Scheduled Import job triggered successfully";
        } catch (Exception e) {
            return "Failed to trigger import job: " + e.getMessage();
        }
    }

    @PostMapping("/trigger-export")
    public String triggerExportJob() {
        try {
            batchJobScheduler.scheduleExportJob();
            return "Scheduled Export job triggered successfully";
        } catch (Exception e) {
            return "Failed to trigger export job: " + e.getMessage();
        }
    }
}