package com.batch.springbatch.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/batch")
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("importPersonJob")
    private Job importJob;

    @Autowired
    @Qualifier("exportPersonJob")
    private Job exportJob;


    @Autowired
    private MeterRegistry meterRegistry;
    
    // Custom metrics
    private Counter importJobStartCounter;
    private Counter importJobSuccessCounter;
    private Counter importJobFailureCounter;
    private Counter exportJobStartCounter;
    private Counter exportJobSuccessCounter;
    private Counter exportJobFailureCounter;
    private Timer importJobTimer;
    private Timer exportJobTimer;
    private AtomicInteger activeJobs = new AtomicInteger(0);

    @PostConstruct
   
          public void initMetrics() {
        logger.info("Initializing metrics for BatchController");
        // Job execution counters
        importJobStartCounter = Counter.builder("batch_job_started_total")
                .description("Total number of import jobs started")
                .tag("job_name", "importPersonJob")
                .register(meterRegistry);

        importJobSuccessCounter = Counter.builder("batch_job_completed_total")
                .description("Total number of import jobs completed successfully")
                .tag("job_name", "importPersonJob")
                .tag("status", "success")
                .register(meterRegistry);

        importJobFailureCounter = Counter.builder("batch_job_completed_total")
                .description("Total number of import jobs failed")
                .tag("job_name", "importPersonJob")
                .tag("status", "failure")
                .register(meterRegistry);

        exportJobStartCounter = Counter.builder("batch_job_started_total")
                .description("Total number of export jobs started")
                .tag("job_name", "exportPersonJob")
                .register(meterRegistry);

        exportJobSuccessCounter = Counter.builder("batch_job_completed_total")
                .description("Total number of export jobs completed successfully")
                .tag("job_name", "exportPersonJob")
                .tag("status", "success")
                .register(meterRegistry);

        exportJobFailureCounter = Counter.builder("batch_job_completed_total")
                .description("Total number of export jobs failed")
                .tag("job_name", "exportPersonJob")
                .tag("status", "failure")
                .register(meterRegistry);

        // Job execution timers
        importJobTimer = Timer.builder("batch_job_duration_seconds")
                .description("Time taken to execute import job")
                .tag("job_name", "importPersonJob")
                .register(meterRegistry);

        exportJobTimer = Timer.builder("batch_job_duration_seconds")
                .description("Time taken to execute export job")
                .tag("job_name", "exportPersonJob")
                .register(meterRegistry);


        logger.info("All metrics have been successfully initialized");
    
    }

    @PostMapping("/import")
    public String importPersons() {
        logger.info("Starting import job execution");
        importJobStartCounter.increment();
        activeJobs.incrementAndGet();
             Timer.Sample sample = Timer.start(meterRegistry);

         try {
            logger.debug("Creating job parameters for import job");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis())
                    .toJobParameters();
            
            logger.info("Launching import job with parameters: {}", jobParameters);
            JobExecution jobExecution = jobLauncher.run(importJob, jobParameters);
            sample.stop(importJobTimer);
            
            logger.info("Import job execution completed with status: {}", jobExecution.getExitStatus().getExitCode());
            
            // Check job execution status and increment appropriate counter
            if (jobExecution.getExitStatus().getExitCode().equals("COMPLETED")) {
                importJobSuccessCounter.increment();
                logger.info("Import job completed successfully");
                return "Import job completed successfully>>>>>>";
            } else {
                importJobFailureCounter.increment();
                logger.warn("Import job failed with status: {}", jobExecution.getExitStatus().getExitCode());
                return "Import job failed with status: " + jobExecution.getExitStatus().getExitCode();
            }
            
        } catch (Exception e) {
            sample.stop(importJobTimer);
            importJobFailureCounter.increment();
            logger.error("Import job failed with exception: {}", e.getMessage(), e);
            return "Import job failed:>>>>> " + e.getMessage();
        } finally {
            activeJobs.decrementAndGet();
            logger.debug("Active jobs count: {}", activeJobs.get());
        }
    }

    @PostMapping("/export")
    public String exportPersons() {
        logger.info("Starting export job execution");
        // Increment start counter
        exportJobStartCounter.increment();
        activeJobs.incrementAndGet();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            logger.debug("Creating job parameters for export job");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis())
                    .toJobParameters();
            
            logger.info("Launching export job with parameters: {}", jobParameters);
            JobExecution jobExecution = jobLauncher.run(exportJob, jobParameters);
            sample.stop(exportJobTimer);
            
            logger.info("Export job execution completed with status: {}", jobExecution.getExitStatus().getExitCode());
            
            // Check job execution status and increment appropriate counter
            if (jobExecution.getExitStatus().getExitCode().equals("COMPLETED")) {
                exportJobSuccessCounter.increment();
                logger.info("Export job completed successfully");
                return "Export job completed successfully";
            } else {
                exportJobFailureCounter.increment();
                logger.warn("Export job failed with status: {}", jobExecution.getExitStatus().getExitCode());
                return "Export job failed with status: " + jobExecution.getExitStatus().getExitCode();
            }
            
        } catch (Exception e) {
            sample.stop(exportJobTimer);
            exportJobFailureCounter.increment();
            logger.error("Export job failed with exception: {}", e.getMessage(), e);
            return "Export job failed: " + e.getMessage();
        } finally {
            activeJobs.decrementAndGet();
            logger.debug("Active jobs count: {}", activeJobs.get());
        }
    }
}