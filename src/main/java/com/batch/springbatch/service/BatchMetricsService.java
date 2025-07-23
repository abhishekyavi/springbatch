package com.batch.springbatch.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;

@Service
public class BatchMetricsService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    // Atomic variables to hold current metric values
    private final AtomicInteger totalJobInstances = new AtomicInteger(0);
    private final AtomicInteger completedJobs = new AtomicInteger(0);
    private final AtomicInteger failedJobs = new AtomicInteger(0);
    private final AtomicInteger runningJobs = new AtomicInteger(0);
    private final AtomicLong averageJobDuration = new AtomicLong(0);
    private final AtomicInteger totalSteps = new AtomicInteger(0);
    private final AtomicInteger failedSteps = new AtomicInteger(0);
    private final AtomicInteger totalPersonRecords = new AtomicInteger(0);

    @PostConstruct
    public void initMetrics() {
        // Register gauges with Prometheus using the simpler API
        meterRegistry.gauge("batch_job_instances_total", totalJobInstances, AtomicInteger::doubleValue);
        meterRegistry.gauge("batch_jobs_completed_total", completedJobs, AtomicInteger::doubleValue);
        meterRegistry.gauge("batch_jobs_failed_total", failedJobs, AtomicInteger::doubleValue);
        meterRegistry.gauge("batch_jobs_running_total", runningJobs, AtomicInteger::doubleValue);
        meterRegistry.gauge("batch_job_average_duration_ms", averageJobDuration, AtomicLong::doubleValue);
        meterRegistry.gauge("batch_steps_total", totalSteps, AtomicInteger::doubleValue);
        meterRegistry.gauge("batch_steps_failed_total", failedSteps, AtomicInteger::doubleValue);
        meterRegistry.gauge("batch_person_records_total", totalPersonRecords, AtomicInteger::doubleValue);

        // Initialize metrics
        updateMetrics();
    }

    @Scheduled(fixedRate = 30000) // Update every 30 seconds
    public void updateMetrics() {
        try {
            updateJobMetrics();
            updateStepMetrics();
            updateJobsByStatus();
            updateAverageJobDuration();
            updatePersonRecordsCount();

        } catch (Exception e) {
            // Log error but don't fail the application
            System.err.println("Error updating batch metrics: " + e.getMessage());
        }
    }

    private void updateJobMetrics() {
        // Total job instances
        String sql = "SELECT COUNT(*) FROM BATCH_JOB_INSTANCE";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        totalJobInstances.set(count != null ? count : 0);
    }

    private void updateJobsByStatus() {
        // Completed jobs
        String completedSql = "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'COMPLETED'";
        Integer completed = jdbcTemplate.queryForObject(completedSql, Integer.class);
        completedJobs.set(completed != null ? completed : 0);

        // Failed jobs
        String failedSql = "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'FAILED'";
        Integer failed = jdbcTemplate.queryForObject(failedSql, Integer.class);
        failedJobs.set(failed != null ? failed : 0);

        // Running jobs (STARTED or STARTING)
        String runningSql = "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS IN ('STARTED', 'STARTING')";
        Integer running = jdbcTemplate.queryForObject(runningSql, Integer.class);
        runningJobs.set(running != null ? running : 0);
    }

    private void updateStepMetrics() {
        // Total steps
        String totalStepsSql = "SELECT COUNT(*) FROM BATCH_STEP_EXECUTION";
        Integer total = jdbcTemplate.queryForObject(totalStepsSql, Integer.class);
        totalSteps.set(total != null ? total : 0);

        // Failed steps
        String failedStepsSql = "SELECT COUNT(*) FROM BATCH_STEP_EXECUTION WHERE STATUS = 'FAILED'";
        Integer failed = jdbcTemplate.queryForObject(failedStepsSql, Integer.class);
        failedSteps.set(failed != null ? failed : 0);
    }

    private void updateAverageJobDuration() {
        String sql = """
            SELECT AVG(TIMESTAMPDIFF('MILLISECOND', START_TIME, END_TIME)) as avg_duration
            FROM BATCH_JOB_EXECUTION 
            WHERE START_TIME IS NOT NULL AND END_TIME IS NOT NULL
            """;
        
        try {
            Long avgDuration = jdbcTemplate.queryForObject(sql, Long.class);
            averageJobDuration.set(avgDuration != null ? avgDuration : 0);
        } catch (Exception e) {
            // Fallback for H2 - use DATEDIFF
            String h2Sql = """
                SELECT AVG(DATEDIFF('MILLISECOND', START_TIME, END_TIME)) as avg_duration
                FROM BATCH_JOB_EXECUTION 
                WHERE START_TIME IS NOT NULL AND END_TIME IS NOT NULL
                """;
            try {
                Long avgDuration = jdbcTemplate.queryForObject(h2Sql, Long.class);
                averageJobDuration.set(avgDuration != null ? avgDuration : 0);
            } catch (Exception e2) {
                averageJobDuration.set(0);
            }
        }
    }
    private void updatePersonRecordsCount() {
        String sql = "SELECT COUNT(*) FROM PERSON";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        totalPersonRecords.set(count != null ? count : 0);
    }

    // Method to get detailed job statistics by job name
    public void registerJobSpecificMetrics() {
        String sql = """
            SELECT ji.JOB_NAME, je.STATUS, COUNT(*) as count
            FROM BATCH_JOB_INSTANCE ji
            JOIN BATCH_JOB_EXECUTION je ON ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
            GROUP BY ji.JOB_NAME, je.STATUS
            """;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            for (Map<String, Object> row : results) {
                String jobName = (String) row.get("JOB_NAME");
                String status = (String) row.get("STATUS");
                Number count = (Number) row.get("COUNT");

                // Create dynamic gauges for each job+status combination
                meterRegistry.gauge("batch_job_executions_by_name",
                        Tags.of("job_name", jobName, "status", status),
                        count.doubleValue());
            }
        } catch (Exception e) {
            System.err.println("Error creating job-specific metrics: " + e.getMessage());
        }
    }

    // Method to get step-level metrics
    public void registerStepSpecificMetrics() {
        String sql = """
            SELECT STEP_NAME, STATUS, 
                   COUNT(*) as execution_count,
                   AVG(READ_COUNT) as avg_read_count,
                   AVG(WRITE_COUNT) as avg_write_count,
                   AVG(COMMIT_COUNT) as avg_commit_count
            FROM BATCH_STEP_EXECUTION
            GROUP BY STEP_NAME, STATUS
            """;

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            for (Map<String, Object> row : results) {
                String stepName = (String) row.get("STEP_NAME");
                String status = (String) row.get("STATUS");
                
                // Register multiple metrics per step
                Number executionCount = (Number) row.get("EXECUTION_COUNT");
                Number avgReadCount = (Number) row.get("AVG_READ_COUNT");
                Number avgWriteCount = (Number) row.get("AVG_WRITE_COUNT");
                Number avgCommitCount = (Number) row.get("AVG_COMMIT_COUNT");

                meterRegistry.gauge("batch_step_executions_by_name",
                        Tags.of("step_name", stepName, "status", status),
                        executionCount != null ? executionCount.doubleValue() : 0);

                if (avgReadCount != null) {
                    meterRegistry.gauge("batch_step_avg_read_count",
                            Tags.of("step_name", stepName),
                            avgReadCount.doubleValue());
                }

                if (avgWriteCount != null) {
                    meterRegistry.gauge("batch_step_avg_write_count",
                            Tags.of("step_name", stepName),
                            avgWriteCount.doubleValue());
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating step-specific metrics: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 60000) // Update detailed metrics every minute
    public void updateDetailedMetrics() {
        registerJobSpecificMetrics();
        registerStepSpecificMetrics();
    }

}
