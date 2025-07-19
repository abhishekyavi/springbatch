
package com.batch.springbatch.config;
import com.batch.springbatch.entity.Person;
import com.batch.springbatch.processor.PersonItemProcessor;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.item.database.JdbcBatchItemWriter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;
// ... imports...for export functionality
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;


@Configuration
public class BatchConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    public DataSource dataSource;

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
    public Job importPersonJob() {
        return new JobBuilder("importPersonJob", jobRepository)
                .flow(step1())
                .end()
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
            .writer(csvWriter())
            .build();
}

@Bean
public Job exportPersonJob() {
    return new JobBuilder("exportPersonJob", jobRepository)
            .flow(exportStep())
            .end()
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