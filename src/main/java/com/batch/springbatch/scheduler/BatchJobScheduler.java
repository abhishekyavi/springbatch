package com.batch.springbatch.scheduler;

import org.slf4j.Logger;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;

@Component
public class BatchJobScheduler {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(BatchJobScheduler.class);


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


// Metrics for scheduled jobs
    private Counter scheduledImportJobStartCounter;
    private Counter scheduledImportJobSuccessCounter;
    private Counter scheduledImportJobFailureCounter;
    private Counter scheduledExportJobStartCounter;
    private Counter scheduledExportJobSuccessCounter;
    private Counter scheduledExportJobFailureCounter;
    private Timer scheduledImportJobTimer;
    private Timer scheduledExportJobTimer;

    @PostConstruct
    public void initMetrics() {
        // Scheduled job execution counters
        scheduledImportJobStartCounter = Counter.builder("scheduled_batch_job_started_total")
                .description("Total number of scheduled import jobs started")
                .tag("job_name", "importPersonJob")
                .register(meterRegistry);

        scheduledImportJobSuccessCounter = Counter.builder("scheduled_batch_job_success_total")
                .description("Total number of successful scheduled import jobs")
                .tag("job_name", "importPersonJob")
                .register(meterRegistry);

        scheduledImportJobFailureCounter = Counter.builder("scheduled_batch_job_failure_total")
                .description("Total number of failed scheduled import jobs")
                .tag("job_name", "importPersonJob")
                .register(meterRegistry);

        scheduledExportJobStartCounter = Counter.builder("scheduled_batch_job_started_total")
                .description("Total number of scheduled export jobs started")
                .tag("job_name", "exportPersonJob")
                .register(meterRegistry);

        scheduledExportJobSuccessCounter = Counter.builder("scheduled_batch_job_success_total")
                .description("Total number of successful scheduled export jobs")
                .tag("job_name", "exportPersonJob")
                .register(meterRegistry);

        scheduledExportJobFailureCounter = Counter.builder("scheduled_batch_job_failure_total")
                .description("Total number of failed scheduled export jobs")
                .tag("job_name", "exportPersonJob")
                .register(meterRegistry);

        // Timers for job execution duration
        scheduledImportJobTimer = Timer.builder("scheduled_batch_job_duration_seconds")
                .description("Duration of scheduled import jobs in seconds")
                .tag("job_name", "importPersonJob")
                .register(meterRegistry);

        scheduledExportJobTimer = Timer.builder("scheduled_batch_job_duration_seconds")
                .description("Duration of scheduled export jobs in seconds")
                .tag("job_name", "exportPersonJob")
                .register(meterRegistry);
    }



    @Scheduled(cron = "${batch.import.cron}")
    public void scheduleImportJob() {
        scheduledImportJobStartCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("startAt:", System.currentTimeMillis())
                .addString("trigger", "scheduled")
                .toJobParameters();

               JobExecution execution=  jobLauncher.run(importJob, jobParameters);
                sample.stop(scheduledImportJobTimer);

                 if (execution.getExitStatus().getExitCode().equals("COMPLETED")) {
                scheduledImportJobSuccessCounter.increment();
                logger.info("Scheduled import job completed successfully. Job ID: {}", 
                    execution.getJobId());
            } else {
                scheduledImportJobFailureCounter.increment();
                logger.error("Scheduled import job failed with status: {}", 
                    execution.getExitStatus().getExitCode());
            }
        } catch (Exception e) {
            sample.stop(scheduledImportJobTimer);
            scheduledImportJobFailureCounter.increment();
            logger.error("Error during scheduled import job execution: {}", e.getMessage(), e);
           
        }

        logger.info("Scheduled import job completed");}

    @Scheduled(cron = "${batch.export.cron}")
    public void scheduleExportJob() {
        scheduledExportJobStartCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        try{

            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("startAt:", System.currentTimeMillis())
                .addString("trigger", "scheduled")
                .toJobParameters();

            JobExecution execution = jobLauncher.run(exportJob, jobParameters);
            sample.stop(scheduledExportJobTimer);

            if (execution.getExitStatus().getExitCode().equals("COMPLETED")) {
                scheduledExportJobSuccessCounter.increment();
                logger.info("Scheduled export job completed successfully. Job ID: {}", 
                    execution.getJobId());
            } else {
                scheduledExportJobFailureCounter.increment();
                logger.error("Scheduled export job failed with status: {}", 
                    execution.getExitStatus().getExitCode());
            }


        }catch (Exception e) {
            sample.stop(scheduledExportJobTimer);
            scheduledExportJobFailureCounter.increment();
            logger.error("Error during scheduled export job execution: {}", e.getMessage(), e);
        }


    }



}
