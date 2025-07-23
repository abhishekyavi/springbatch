package com.batch.springbatch.listener;

import org.slf4j.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class JobExecutionMDCListener implements JobExecutionListener {
//MDC -mapped Diagnostic Context-
//is used to add contextual information to logs, such as job names or IDs.
    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        MDC.put("jobName", jobName);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        MDC.clear();
    }
}
