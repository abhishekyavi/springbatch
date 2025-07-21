package com.batch.springbatch.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.*;

@Entity
@Table(name = "person")
@Tag(name = "Person", description = "Entity representing a person in the system")
public class Person {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the person", example = "1")
    private Long id;
    
    @Column(name = "first_name")
    @Schema(description = "First name of the person", example = "Alpha")
    private String firstName;
    @Schema(description = "Last name of the person", example = "Beta")
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "email")
    @Schema(description = "Email address of the person", example = "alpha.doe@example.com")
    private String email;
    
    @Column(name = "age")
    @Schema(description = "Age of the person", example = "30")
    private Integer age;
    
    // Constructors
    public Person() {}
    
    public Person(String firstName, String lastName, String email, Integer age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.age = age;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    @Override
    public String toString() {
        return "Person{id=" + id + ", firstName='" + firstName + "', lastName='" + lastName + 
               "', email='" + email + "', age=" + age + "}";
    }
}