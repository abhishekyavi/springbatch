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
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.prometheus.enabled=true
management.endpoint.metrics.enabled=true
```

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
