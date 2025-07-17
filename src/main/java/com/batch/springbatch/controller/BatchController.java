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




@RestController
@RequestMapping("/batch")
public class BatchController {

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


    
    }

    @PostMapping("/import")
    public String importPersons() {
        importJobStartCounter.increment();
        activeJobs.incrementAndGet();
             Timer.Sample sample = Timer.start(meterRegistry);

         try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis())
                    .toJobParameters();
            
            JobExecution jobExecution = jobLauncher.run(importJob, jobParameters);
            sample.stop(importJobTimer);
            
            // Check job execution status and increment appropriate counter
            if (jobExecution.getExitStatus().getExitCode().equals("COMPLETED")) {
                importJobSuccessCounter.increment();
                return "Import job completed successfully>>>>>>";
            } else {
                importJobFailureCounter.increment();
                return "Import job failed with status: " + jobExecution.getExitStatus().getExitCode();
            }
            
        } catch (Exception e) {
            sample.stop(importJobTimer);
            importJobFailureCounter.increment();
            return "Import job failed:>>>>> " + e.getMessage();
        } finally {
            activeJobs.decrementAndGet();
        }
    }

    @PostMapping("/export")
    public String exportPersons() {
        // Increment start counter
        exportJobStartCounter.increment();
        activeJobs.incrementAndGet();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis())
                    .toJobParameters();
            
            JobExecution jobExecution = jobLauncher.run(exportJob, jobParameters);
            sample.stop(exportJobTimer);
            
            // Check job execution status and increment appropriate counter
            if (jobExecution.getExitStatus().getExitCode().equals("COMPLETED")) {
                exportJobSuccessCounter.increment();
                return "Export job completed successfully";
            } else {
                exportJobFailureCounter.increment();
                return "Export job failed with status: " + jobExecution.getExitStatus().getExitCode();
            }
            
        } catch (Exception e) {
            sample.stop(exportJobTimer);
            exportJobFailureCounter.increment();
            return "Export job failed: " + e.getMessage();
        } finally {
            activeJobs.decrementAndGet();
        }
    }
}