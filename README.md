# Spring Batch Application - Complete Documentation

## 📋 Project Overview

This is a comprehensive **Spring Boot Batch Application** that demonstrates advanced ETL (Extract, Transform, Load) operations for processing person data. The application includes REST APIs, scheduled batch processing, comprehensive monitoring with Prometheus metrics, and centralized logging with Loki integration.

---

## 🏗️ Architecture & Core Components

### Key Dependencies & Add-ons

| Component | Purpose | Configuration |
|-----------|---------|---------------|
| **Spring Boot Starter Batch** | Core batch processing framework | Auto-configured with JobRepository |
| **Spring Boot Starter Actuator** | Monitoring & health checks | Exposes metrics, health endpoints |
| **Micrometer Prometheus** | Metrics collection & export | Custom counters, timers, gauges |
| **Loki Logback Appender** | Centralized logging | Real-time log aggregation |
| **H2 Database** | In-memory database | Development & testing |
| **Spring Data JPA** | Data persistence layer | Entity management |
| **Spring Boot Starter Web** | REST API endpoints | Controller layer |
| **Spring Boot Starter Validation** | Data validation | Bean validation |

---

## 🔄 Controller Flow Architecture

### 1. BatchController (`/batch`)

**Purpose**: Manual batch job execution with comprehensive monitoring

#### Import Endpoint: `POST /batch/import`
```
┌─────────────────────────────────────────────────────────────────┐
│                    IMPORT JOB FLOW                             │
├─────────────────────────────────────────────────────────────────┤
│ 1. HTTP Request → POST /batch/import                           │
│ 2. Increment import job start counter                          │
│ 3. Start execution timer                                       │
│ 4. Create JobParameters with timestamp                         │
│ 5. Launch importPersonJob via JobLauncher                      │
│ 6. Execute Step: CSV → Processor → Database                    │
│ 7. Monitor execution status                                     │
│ 8. Update success/failure counters                             │
│ 9. Stop timer & decrement active jobs                          │
│ 10. Return execution status to client                          │
└─────────────────────────────────────────────────────────────────┘
```

**Metrics Captured:**
- `batch_job_started_total{job_name="importPersonJob"}`
- `batch_job_completed_total{job_name="importPersonJob", status="success/failure"}`
- `batch_job_duration_seconds{job_name="importPersonJob"}`

#### Export Endpoint: `POST /batch/export`
```
┌─────────────────────────────────────────────────────────────────┐
│                    EXPORT JOB FLOW                             │
├─────────────────────────────────────────────────────────────────┤
│ 1. HTTP Request → POST /batch/export                           │
│ 2. Increment export job start counter                          │
│ 3. Start execution timer                                       │
│ 4. Create JobParameters with timestamp                         │
│ 5. Launch exportPersonJob via JobLauncher                      │
│ 6. Execute Step: Database → Processor → CSV                    │
│ 7. Monitor execution status                                     │
│ 8. Update success/failure counters                             │
│ 9. Stop timer & decrement active jobs                          │
│ 10. Return execution status to client                          │
└─────────────────────────────────────────────────────────────────┘
```

**Metrics Captured:**
- `batch_job_started_total{job_name="exportPersonJob"}`
- `batch_job_completed_total{job_name="exportPersonJob", status="success/failure"}`
- `batch_job_duration_seconds{job_name="exportPersonJob"}`

### 2. JobManagementController (`/job-management`)

**Purpose**: Trigger scheduled jobs manually for testing and management

#### Endpoints:
- `POST /job-management/trigger-import` → Manually trigger scheduled import
- `POST /job-management/trigger-export` → Manually trigger scheduled export

```
┌─────────────────────────────────────────────────────────────────┐
│              JOB MANAGEMENT CONTROLLER FLOW                    │
├─────────────────────────────────────────────────────────────────┤
│ 1. HTTP Request → POST /job-management/trigger-{type}          │
│ 2. Delegate to BatchJobScheduler                               │
│ 3. Execute respective scheduled job method                      │
│ 4. Return success/failure status                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## ⏰ Scheduled Job Processing

### BatchJobScheduler Component

**Cron Configurations:**
- **Import Job**: `0 */2 * * * ?` (Every 2 minutes)
- **Export Job**: `0 */4 * * * ?` (Every 4 minutes)

#### Scheduled Import Flow:
```
┌─────────────────────────────────────────────────────────────────┐
│                SCHEDULED IMPORT JOB FLOW                       │
├─────────────────────────────────────────────────────────────────┤
│ 1. Cron Trigger (Every 2 minutes)                              │
│ 2. Increment scheduled job start counter                       │
│ 3. Start execution timer                                       │
│ 4. Create JobParameters with:                                  │
│    - timestamp                                                 │
│    - trigger="scheduled"                                       │
│ 5. Launch importPersonJob                                      │
│ 6. Monitor execution & update metrics                          │
│ 7. Log execution results                                       │
└─────────────────────────────────────────────────────────────────┘
```

#### Scheduled Export Flow:
```
┌─────────────────────────────────────────────────────────────────┐
│                SCHEDULED EXPORT JOB FLOW                       │
├─────────────────────────────────────────────────────────────────┤
│ 1. Cron Trigger (Every 4 minutes)                              │
│ 2. Increment scheduled job start counter                       │
│ 3. Start execution timer                                       │
│ 4. Create JobParameters with:                                  │
│    - timestamp                                                 │
│    - trigger="scheduled"                                       │
│ 5. Launch exportPersonJob with timestamped output file         │
│ 6. Generate: scheduled_export_yyyyMMdd_HHmmss.csv              │
│ 7. Monitor execution & update metrics                          │
│ 8. Log execution results                                       │
└─────────────────────────────────────────────────────────────────┘
```

**Scheduled Job Metrics:**
- `scheduled_batch_job_started_total{job_name="importPersonJob/exportPersonJob"}`
- `scheduled_batch_job_success_total{job_name="importPersonJob/exportPersonJob"}`
- `scheduled_batch_job_failure_total{job_name="importPersonJob/exportPersonJob"}`
- `scheduled_batch_job_duration_seconds{job_name="importPersonJob/exportPersonJob"}`

---

## 🔧 Batch Configuration Details

### Import Job Configuration
```
┌─────────────────────────────────────────────────────────────────┐
│                    IMPORT JOB PIPELINE                         │
├─────────────────────────────────────────────────────────────────┤
│ Reader: FlatFileItemReader                                      │
│ ├── Source: /templates/person.csv                              │
│ ├── Delimiter: Comma                                           │
│ ├── Fields: id, first_name, last_name, email, age              │
│ └── Skip Header: Yes                                           │
│                                                                │
│ Processor: PersonItemProcessor                                  │
│ ├── Transformation: Person → Person                            │
│ └── Logic: Pass-through (can be enhanced)                      │
│                                                                │
│ Writer: JdbcBatchItemWriter                                     │
│ ├── Target: H2 Database - person table                         │
│ ├── SQL: INSERT INTO person (first_name, last_name,            │
│ │        email, age) VALUES (:firstName, :lastName,            │
│ │        :email, :age)                                         │
│ └── Chunk Size: 10 records                                     │
└─────────────────────────────────────────────────────────────────┘
```

### Export Job Configuration
```
┌─────────────────────────────────────────────────────────────────┐
│                    EXPORT JOB PIPELINE                         │
├─────────────────────────────────────────────────────────────────┤
│ Reader: JdbcCursorItemReader                                    │
│ ├── Source: H2 Database - person table                         │
│ ├── SQL: SELECT id, first_name, last_name, email, age          │
│ │        FROM person ORDER BY id                               │
│ └── Row Mapper: BeanPropertyRowMapper<Person>                  │
│                                                                │
│ Processor: exportProcessor                                      │
│ ├── Transformation: Person → Person                            │
│ └── Logic: Pass-through (extensible)                           │
│                                                                │
│ Writer: FlatFileItemWriter (@StepScope)                        │
│ ├── Target: /output/exported_persons.csv (manual)              │
│ │          /output/scheduled_export_timestamp.csv (scheduled)   │
│ ├── Format: CSV with headers                                   │
│ ├── Fields: id, first_name, last_name, email, age              │
│ └── Chunk Size: 10 records                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📊 Monitoring & Observability

### Actuator Endpoints
- **Health**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`
- **Info**: `/actuator/info`

### Custom Metrics Categories

#### 1. Job Execution Metrics
```yaml
Manual Jobs:
  - batch_job_started_total
  - batch_job_completed_total
  - batch_job_duration_seconds

Scheduled Jobs:
  - scheduled_batch_job_started_total
  - scheduled_batch_job_success_total
  - scheduled_batch_job_failure_total
  - scheduled_batch_job_duration_seconds
```

#### 2. Application Metrics
- **Active Jobs**: Real-time count of running jobs
- **Job Success Rate**: Success vs failure ratio
- **Execution Duration**: Performance monitoring

### Loki Integration

**Configuration:**
```yaml
URL: http://loki-abhishek1426-dev.apps.rm3.7wse.p1.openshiftapps.com/loki/api/v1/push
Labels:
  - application: springbatch
  - environment: dev
  - instance: localhost
Batch Settings:
  - Max Items: 1000
  - Timeout: 10 seconds
```

**Log Levels:**
- `com.batch.springbatch`: DEBUG
- `org.springframework.batch`: INFO
- `com.github.loki4j`: INFO

---

## 🗄️ Database Schema

### Person Entity
```sql
CREATE TABLE person (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255),
    age INTEGER
);
```

### Spring Batch Tables (Auto-created)
- `BATCH_JOB_INSTANCE`
- `BATCH_JOB_EXECUTION`
- `BATCH_JOB_EXECUTION_PARAMS`
- `BATCH_STEP_EXECUTION`
- `BATCH_JOB_EXECUTION_CONTEXT`
- `BATCH_STEP_EXECUTION_CONTEXT`

---

## 🚀 API Usage Examples

### Manual Job Execution
```bash
# Import CSV data to database
curl -X POST http://localhost:8080/batch/import

# Export database data to CSV
curl -X POST http://localhost:8080/batch/export
```

### Job Management
```bash
# Trigger scheduled import manually
curl -X POST http://localhost:8080/job-management/trigger-import

# Trigger scheduled export manually
curl -X POST http://localhost:8080/job-management/trigger-export
```

### Monitoring
```bash
# Check application health
curl http://localhost:8080/actuator/health

# View Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Access H2 Console
http://localhost:8080/h2-console
```

---

## 🔧 Configuration Properties

### Core Application Settings
```properties
spring.application.name=springbatch
spring.batch.job.enabled=false
spring.batch.initialize-schema=always
spring.batch.table-prefix=BATCH_
```

### Database Configuration
```properties
spring.h2.console.enabled=true
spring.datasource.url=jdbc:h2:mem:springbatch
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=update
```

### Monitoring Configuration
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.prometheus.enabled=true
management.endpoint.metrics.enabled=true
```

### Scheduling Configuration
```properties
batch.import.cron=0 */2 * * * ?
batch.export.cron=0 */4 * * * ?
```

---

## 📁 File Processing Details

### Input File Format (`person.csv`)
```csv
id,first_name,last_name,email,age
1,John,Doe,john.doe@email.com,30
2,Jane,Smith,jane.smith@email.com,25
```

### Output File Patterns
- **Manual Export**: `output/exported_persons.csv`
- **Scheduled Export**: `output/scheduled_export_yyyyMMdd_HHmmss.csv`

---

## 🔍 Error Handling & Logging

### Exception Management
- **Job Failures**: Captured in metrics and logs
- **Database Errors**: Logged with full stack traces
- **File Processing Errors**: Detailed error messages
- **Scheduling Errors**: Comprehensive error logging

### Log Pattern
```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n
```

---

## 🎯 Key Features Summary

1. **Dual Processing Modes**: Manual via REST API + Scheduled via Cron
2. **Comprehensive Monitoring**: Prometheus metrics + Loki logging
3. **Flexible File Handling**: Timestamped exports for scheduled jobs
4. **Robust Error Handling**: Detailed metrics and logging
5. **Production Ready**: Actuator endpoints, health checks, metrics
6. **Scalable Architecture**: Chunk-based processing, configurable batch sizes
7. **Development Friendly**: H2 console, debug logging, comprehensive documentation

---

## 🔗 Dependencies & Versions

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.5.3 | Core framework |
| H2 Database | Runtime | In-memory database |
| Micrometer Prometheus | Runtime | Metrics export |
| Loki Logback Appender | 1.4.2 | Log aggregation |
| Spring Batch Test | Test scope | Testing utilities |

---

*This documentation covers the complete Spring Batch application architecture, controller flows, monitoring setup, and operational procedures.*
