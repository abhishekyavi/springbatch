<<<<<<< HEAD
# Spring Batch Project Documentation

## 📋 Project Overview

This is a **Spring Boot Batch Application** that demonstrates ETL (Extract, Transform, Load) operations for processing person data. The application provides both **import** and **export** functionality with REST API endpoints and includes comprehensive monitoring through Prometheus metrics.

## 🏗️ Project Structure

```
springbatch/
├── src/
│   ├── main/
│   │   ├── java/com/batch/springbatch/
│   │   │   ├── SpringbatchApplication.java     # Main application entry point
│   │   │   ├── config/
│   │   │   │   └── BatchConfig.java           # Batch job configurations
│   │   │   ├── controller/
│   │   │   │   └── BatchController.java       # REST API endpoints
│   │   │   ├── entity/
│   │   │   │   └── Person.java               # JPA entity
│   │   │   ├── processor/
│   │   │   │   └── PersonItemProcessor.java   # Data transformation logic
│   │   │   └── repo/
│   │   │       └── PersonRepository.java      # JPA repository
│   │   └── resources/
│   │       ├── application.properties         # App configuration
│   │       ├── data.sql                      # Database schema
│   │       └── templates/
│   │           └── person.csv                # Input CSV file
│   └── test/
│       └── java/com/batch/springbatch/
│           └── SpringbatchApplicationTests.java
├── output/
│   └── exported_persons.csv                  # Generated export file
├── pom.xml                                   # Maven dependencies
└── PROJECT_DOCUMENTATION.md                 # This documentation
```

## 🔧 Technology Stack

### Core Framework
- **Spring Boot 3.5.3** - Main application framework
- **Spring Batch** - Batch processing framework
- **Spring Data JPA** - Data access layer
- **Spring Web** - REST API support
- **Spring Boot Actuator** - Monitoring and metrics

### Database
- **H2 Database** - In-memory database for development
- **JDBC** - Database connectivity

### Monitoring & Metrics
- **Micrometer** - Application metrics
- **Prometheus** - Metrics collection and monitoring

### Build Tool
- **Maven** - Dependency management and build automation
- **Java 17** - Programming language version

## 📊 Data Model

### Person Entity
```java
@Entity
@Table(name = "person")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "age")
    private Integer age;
}
```

### Database Schema
```sql
CREATE TABLE person (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255),
    age INT
);
```

## 🔄 Batch Processing Flow

### 1. Import Job Flow (`importPersonJob`)

```
CSV File → Reader → Processor → Writer → Database
```

**Steps:**
1. **Reader**: `FlatFileItemReader` reads from `templates/person.csv`
2. **Processor**: `PersonItemProcessor` transforms data (converts first name to uppercase)
3. **Writer**: `JdbcBatchItemWriter` inserts data into H2 database
4. **Chunk Size**: Processes 10 records at a time

### 2. Export Job Flow (`exportPersonJob`)

```
Database → Reader → Processor → Writer → CSV File
```

**Steps:**
1. **Reader**: `JdbcCursorItemReader` reads from person table
2. **Processor**: `exportProcessor` (pass-through, can add custom logic)
3. **Writer**: `FlatFileItemWriter` writes to `output/exported_persons.csv`
4. **Chunk Size**: Processes 10 records at a time

## 🌐 REST API Endpoints

### Import Endpoint
```http
POST /batch/import
```
- **Purpose**: Triggers the import job to read CSV and load data into database
- **Response**: Job execution status message
- **Metrics**: Tracks job start, success/failure counters, and execution time

### Export Endpoint
```http
POST /batch/export
```
- **Purpose**: Triggers the export job to extract data from database to CSV
- **Response**: Job execution status message
- **Metrics**: Tracks job start, success/failure counters, and execution time

## 📈 Monitoring & Metrics

### Actuator Endpoints
- `/actuator/health` - Application health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus-formatted metrics

### Custom Metrics Tracked
1. **Job Start Counters**
   - `batch_job_started_total{job_name="importPersonJob"}`
   - `batch_job_started_total{job_name="exportPersonJob"}`

2. **Job Completion Counters**
   - `batch_job_completed_total{job_name="importPersonJob", status="success|failure"}`
   - `batch_job_completed_total{job_name="exportPersonJob", status="success|failure"}`

3. **Job Duration Timers**
   - `batch_job_duration_seconds{job_name="importPersonJob"}`
   - `batch_job_duration_seconds{job_name="exportPersonJob"}`

4. **Active Jobs Counter**
   - Tracks currently running batch jobs

## ⚙️ Configuration

### Application Properties
```properties
# Application name
spring.application.name=springbatch

# H2 Database configuration
spring.h2.console.enabled=true
spring.datasource.url=jdbc:h2:mem:springbatch
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update

# Spring Batch configuration
spring.batch.job.enabled=false
spring.batch.initialize-schema=always
spring.batch.table-prefix=BATCH_
spring.batch.jdbc.initialize-schema=always
spring.batch.job.repository.type=jdbc

# Actuator endpoints
=======
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
>>>>>>> 4193821cc48ad362160f4ea42d6e7cd2e53ff3c1
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.prometheus.enabled=true
management.endpoint.metrics.enabled=true
```

<<<<<<< HEAD
## 🚀 How to Run

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Steps
1. **Clone/Download the project**
2. **Navigate to project directory**
   ```bash
   cd springbatch
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```
   Or
   ```bash
   java -jar target/springbatch-0.0.1-SNAPSHOT.jar
   ```

5. **Access the application**
   - Application: `http://localhost:8080`
   - H2 Console: `http://localhost:8080/h2-console`
   - Health Check: `http://localhost:8080/actuator/health`
   - Metrics: `http://localhost:8080/actuator/metrics`
   - Prometheus: `http://localhost:8080/actuator/prometheus`

## 🧪 Testing the Batch Jobs

### Test Import Job
```bash
curl -X POST http://localhost:8080/batch/import
```
**Expected Response**: "Import job completed successfully>>>>>>"

### Test Export Job
```bash
curl -X POST http://localhost:8080/batch/export
```
**Expected Response**: "Export job completed successfully"

### Verify Results
1. **Check H2 Database**: Navigate to `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:springbatch`
   - Username: `sa`
   - Password: (leave empty)
   - Query: `SELECT * FROM person;`

2. **Check Export File**: Look for `output/exported_persons.csv`

## 📝 Sample Data

### Input CSV (`templates/person.csv`)
```csv
id,first_name,last_name,email,age
1,John,Doe,john.doe@example.com,30
2,Jane,Smith,jane.smith@example.com,25
3,Bob,Johnson,bob.johnson@example.com,40
...
```

### Output CSV (`output/exported_persons.csv`)
```csv
id,first_name,last_name,email,age
1,JOHN,Doe,john.doe@example.com,30
2,JANE,Smith,jane.smith@example.com,25
3,BOB,Johnson,bob.johnson@example.com,40
...
```
*Note: First names are converted to uppercase during processing*

## 🔍 Key Features

1. **Dual Processing**: Both import (CSV → DB) and export (DB → CSV) capabilities
2. **Data Transformation**: Converts first names to uppercase during import
3. **Chunk Processing**: Efficient processing in chunks of 10 records
4. **Error Handling**: Comprehensive error handling with appropriate HTTP responses
5. **Monitoring**: Built-in metrics and monitoring through Actuator and Prometheus
6. **Database Management**: Automatic schema creation and data persistence
7. **RESTful API**: Easy-to-use REST endpoints for triggering batch jobs
8. **In-Memory Database**: Quick setup with H2 for development and testing

## 🛠️ Customization Options

### Adding New Processing Logic
Modify `PersonItemProcessor.java` to add custom transformation logic:
```java
public Person process(Person person) {
    // Add your custom processing logic here
    person.setFirstName(person.getFirstName().toUpperCase());
    // Example: Validate email format, clean data, etc.
    return person;
}
```

### Changing Input/Output Files
Modify paths in `BatchConfig.java`:
- Input: `.resource(new ClassPathResource("templates/person.csv"))`
- Output: `.resource(new FileSystemResource("output/exported_persons.csv"))`

### Adding Database Persistence
Replace H2 with PostgreSQL/MySQL by updating:
1. Dependencies in `pom.xml`
2. Database configuration in `application.properties`
3. Update JPA dialect accordingly

## 📊 Performance Characteristics

- **Chunk Size**: 10 records per transaction
- **Memory Usage**: Optimized for large datasets through streaming
- **Database**: In-memory H2 for fast development cycles
- **Monitoring**: Real-time metrics collection
- **Concurrency**: Thread-safe batch processing

## 🎯 Use Cases

This project is ideal for:
1. **Learning Spring Batch concepts**
2. **ETL pipeline development**
3. **Data migration projects**
4. **Batch processing proof-of-concepts**
5. **Microservices with batch capabilities**
6. **Monitoring and metrics integration examples**

---

**Project Type**: Spring Boot Batch Application  
**Created**: 2025  
**Java Version**: 17  
**Spring Boot Version**: 3.5.3
=======
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
>>>>>>> 4193821cc48ad362160f4ea42d6e7cd2e53ff3c1
