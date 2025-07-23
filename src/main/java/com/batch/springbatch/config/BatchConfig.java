package com.batch.springbatch.config;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
// ... imports...for export functionality
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

import com.batch.springbatch.entity.Person;
import com.batch.springbatch.listener.JobExecutionMDCListener;
import com.batch.springbatch.processor.PersonItemProcessor;

@Configuration
public class BatchConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    public DataSource dataSource;
    
    // Force Spring Batch schema initialization
    @Bean
    public DataSourceInitializer dataSourceInitializer() {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(databasePopulator());
        return initializer;
    }
    
    private DatabasePopulator databasePopulator() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath*:org/springframework/batch/core/schema-h2.sql");
            populator.addScripts(resources);
        } catch (Exception e) {
            // Fallback to manual SQL if resource not found
            populator.addScript(resolver.getResource("classpath:batch-schema.sql"));
        }
        return populator;
    }

    @Bean
    public FlatFileItemReader<Person> reader() {
        return new FlatFileItemReaderBuilder<Person>()
                .name("personItemReader")
                .resource(new ClassPathResource("templates/person.csv"))
                .delimited()
                .names("id", "first_name", "last_name", "email", "age")
                .linesToSkip(1) // Skip header line
                .fieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{
                    setTargetType(Person.class);
                }})
                .build();
    }

    @Bean
    public ItemProcessor<Person, Person> processor() {
        return person -> new PersonItemProcessor().process(person); // No processing, just pass through
    }

    @Bean
    public JdbcBatchItemWriter<Person> writer() {
        return new JdbcBatchItemWriterBuilder<Person>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO person (first_name, last_name, email, age) VALUES (:firstName, :lastName, :email, :age)")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Step step1() {
        return new StepBuilder("step1", jobRepository)
                .<Person, Person>chunk(10, transactionManager)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    public Job importPersonJob(JobExecutionMDCListener jobExecutionMDCListener) {
        return new JobBuilder("importPersonJob", jobRepository)
                .flow(step1())
                .end()
                .listener(jobExecutionMDCListener)
                .build();
    }

    //code for export functionality

@Bean
public JdbcCursorItemReader<Person> dbReader() {
    return new JdbcCursorItemReaderBuilder<Person>()
            .name("personDbReader")
            .dataSource(dataSource)
            .sql("SELECT id, first_name, last_name, email, age FROM person ORDER BY id")
            .rowMapper(new BeanPropertyRowMapper<>(Person.class))
            .build();
}

@Bean
public FlatFileItemWriter<Person> csvWriter() {
    return new FlatFileItemWriterBuilder<Person>()
            .name("personCsvWriter")
            .resource(new FileSystemResource("output/exported_persons.csv"))
            .delimited()
            .delimiter(",")
            .names("id", "firstName", "lastName", "email", "age")
            .headerCallback(writer -> writer.write("id,first_name,last_name,email,age"))
            .build();
}

@Bean
public ItemProcessor<Person, Person> exportProcessor() {
    return person -> {
        // You can add any processing logic here if needed
        // For now, just return the person as-is
        return person;
    };
}

@Bean
public Step exportStep() {
    return new StepBuilder("exportStep", jobRepository)
            .<Person, Person>chunk(10, transactionManager)
            .reader(dbReader())
            .processor(exportProcessor())
           // .writer(csvWriter())
            .writer(scheduledCsvWriter(null, null))
            .build();
}

@Bean
public Job exportPersonJob(JobExecutionMDCListener jobExecutionMDCListener) {
    return new JobBuilder("exportPersonJob", jobRepository)
            .flow(exportStep())
            .end()
            .listener(jobExecutionMDCListener)
            .build();
}

@Bean
@StepScope
public FlatFileItemWriter<Person> scheduledCsvWriter(
        @Value("#{jobParameters['startAt']}") String startAt,
        @Value("#{jobParameters['trigger']?:'manual'}") String trigger

) {

 String filename;
        if ("scheduled".equals(trigger)) {
            // Create timestamped filename for scheduled jobs
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            filename = "output/scheduled_export_" + timestamp + ".csv";
        } else {
            // Use default filename for manual triggers
            filename = "output/exported_persons.csv";
        }

        return new FlatFileItemWriterBuilder<Person>()
                .name("personCsvWriter")
                .resource(new FileSystemResource(filename))
                .delimited()
                .delimiter(",")
                .names("id", "firstName", "lastName", "email", "age")
                .headerCallback(writer -> writer.write("id,first_name,last_name,email,age"))
                .build();

   }
  



















}