# Spring Batch Application Flow Documentation

## 🏗️ Application Architecture Overview

This is a **Spring Boot Batch Application** for ETL (Extract, Transform, Load) operations that processes person data with both import and export capabilities.

## 📊 Data Flow Diagram

```
CSV File → Import Job → H2 Database → Export Job → CSV File
    ↓           ↓            ↓           ↓         ↓
[Input]    [Transform]   [Storage]   [Extract]  [Output]
```

## 🔄 Complete Application Flow

### 1. **Application Startup Flow**

```
SpringbatchApplication.java → @EnableScheduling → Configuration Loading
                ↓
BatchConfig.java → Database Schema Initialization via databasePopulator()
                ↓
AsyncConfig.java → Thread Pool & JobLauncher Setup
                ↓
SwaggerApiConfig.java → API Documentation Setup
```

The `databasePopulator()` method in `BatchConfig.java` initializes the Spring Batch schema tables in H2 database.

### 2. **Import Job Flow (CSV → Database)**

**Trigger Options:**
- **Manual**: `POST /batch/import` via `BatchController`
- **Scheduled**: Every 2 minutes via `BatchJobScheduler`
- **On-demand**: `POST /job-management/trigger-import` via `JobManagementController`

**Processing Pipeline:**
```
1. FlatFileItemReader → reads person.csv (chunk size: 10)
2. PersonItemProcessor → transforms firstName to UPPERCASE  
3. JdbcBatchItemWriter → inserts into H2 database
4. JobExecutionMDCListener → adds logging context
```

**Files Involved:**
- **Input**: `src/main/resources/templates/person.csv`
- **Processor**: `PersonItemProcessor.java`
- **Entity**: `Person.java`
- **Repository**: `PersonRepository.java`

### 3. **Export Job Flow (Database → CSV)**

**Trigger Options:**
- **Manual**: `POST /batch/export` 
- **Scheduled**: Every 4 minutes
- **On-demand**: `POST /job-management/trigger-export`

**Processing Pipeline:**
```
1. JdbcCursorItemReader → reads from person table (chunk size: 10)
2. exportProcessor → pass-through (no transformation)
3. FlatFileItemWriter → writes to CSV file
4. scheduledCsvWriter → handles timestamped filenames for scheduled jobs
```

**Output**: `output/exported_persons.csv`

### 4. **Monitoring & Metrics Flow**

```
Job Execution → MeterRegistry → Custom Metrics → Prometheus Endpoint
                      ↓
BatchMetricsService → Database Queries → Gauge Metrics
                      ↓
MetricsController → /api/metrics endpoints
```

**Monitored Metrics:**
- Job start/success/failure counters
- Job execution duration timers
- Active jobs count
- Database record counts

### 5. **Logging Flow**

```
Application Logs → Logback Configuration → Multiple Outputs
                              ↓
    ┌─────────────────────────┼─────────────────────────┐
    ↓                         ↓                         ↓
Console Output        File Output              Loki Output
                   (logs/springbatch.log)   (Remote Grafana)
```

**Logging Context**: `JobExecutionMDCListener` adds job names to log context.

### 6. **Scheduled Execution Flow**

**Import Schedule**: `0 */2 * * * ?` (every 2 minutes)
**Export Schedule**: `0 */4 * * * ?` (every 4 minutes)

```
@Scheduled → BatchJobScheduler → JobLauncher → Job Execution
                    ↓
            Metrics Collection → Success/Failure Counters
                    ↓
            Log Generation → MDC Context → Loki/Grafana
```

## 🗄️ Database Schema Flow

**Spring Batch Tables** (auto-created by `databasePopulator()`):
- `BATCH_JOB_INSTANCE`
- `BATCH_JOB_EXECUTION` 
- `BATCH_STEP_EXECUTION`
- `BATCH_JOB_EXECUTION_PARAMS`

**Application Table** (created by data.sql):
- `person` table

## 🌐 API Endpoints Flow

### Manual Job Control:
```
POST /batch/import     → Direct job execution
POST /batch/export     → Direct job execution
```

### Scheduled Job Control:
```
POST /job-management/trigger-import  → Scheduled job trigger
POST /job-management/trigger-export  → Scheduled job trigger
GET  /job-management/test-logs       → Log testing
```

### Monitoring:
```
GET /api/metrics/batch      → Custom batch metrics
GET /api/metrics/refresh    → Force metrics update
GET /actuator/prometheus    → Prometheus format metrics
GET /swagger-ui.html        → API documentation
```

## 🔧 Configuration Flow

**Main Config**: `application.properties`
- H2 database settings
- Batch job settings
- Cron schedules
- Logging configuration

**Batch Config**: `BatchConfig.java`
- Job definitions
- Step configurations  
- Reader/Writer/Processor beans

## 📈 Complete Processing Cycle

1. **Application starts** → Schema initialization
2. **Scheduler runs** → Import job every 2 minutes
3. **Data flows**: CSV → Transform → Database
4. **Scheduler runs** → Export job every 4 minutes  
5. **Data flows**: Database → CSV output
6. **Metrics collected** → Prometheus monitoring
7. **Logs generated** → Multiple destinations
8. **APIs available** → Manual job control

This creates a continuous ETL pipeline with comprehensive monitoring and flexible execution options.

## 📋 Detailed Component Analysis

### JobManagementController Flow

The `JobManagementController` provides REST API endpoints for manual job triggering:

#### Import Job Trigger Flow:
```java
POST /job-management/trigger-import
    ↓
MDC.put("jobName", "importPersonJob")
    ↓
batchJobScheduler.scheduleImportJob()
    ↓
Response: "Scheduled Import job triggered successfully"
```

#### Export Job Trigger Flow:
```java
POST /job-management/trigger-export
    ↓
MDC.put("jobName", "exportPersonJob")
    ↓
batchJobScheduler.scheduleExportJob()
    ↓
Response: "Scheduled Export job triggered successfully"
```

#### Test Logs Flow:
```java
GET /job-management/test-logs
    ↓
Generate test logs with different levels (INFO, WARN, ERROR)
    ↓
Test different job names in MDC context
    ↓
Response: "Test logs generated successfully"
```

### BatchJobScheduler Integration

The controller delegates actual job execution to `BatchJobScheduler`, which:
1. Uses `@Async` for non-blocking execution
2. Adds job parameters (startAt, trigger type)
3. Launches jobs through `JobLauncher`
4. Handles metrics collection
5. Manages error handling and logging

### Logging Context Management

Each endpoint uses MDC (Mapped Diagnostic Context) for:
- **Job identification**: `MDC.put("jobName", "jobType")`
- **Request tracing**: Links logs to specific job executions
- **Grafana integration**: Enables log filtering by job type
- **Cleanup**: `MDC.clear()` in finally blocks

### API Documentation Integration

The controller uses OpenAPI 3.0 annotations:
- `@Operation`: Describes endpoint purpose
- `@ApiResponses`: Documents response codes and examples
- `@Tag`: Groups endpoints in Swagger UI
- `@Schema`: Defines response content types

### Error Handling Strategy

Each endpoint follows consistent error handling:
1. **Try-catch blocks**: Capture exceptions
2. **Detailed logging**: Error messages with stack traces
3. **User-friendly responses**: Simple error messages for API consumers
4. **MDC cleanup**: Ensures logging context is cleared
5. **Graceful degradation**: Application continues running on job failures

This architecture ensures reliable job execution with comprehensive monitoring and easy debugging capabilities.
