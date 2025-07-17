package com.batch.springbatch.processor;

import com.batch.springbatch.entity.Person;

public class PersonItemProcessor {

    public Person process(Person person) {
        // Example processing: Convert first name to uppercase
        String firstName = person.getFirstName();
        if (firstName != null) {
            person.setFirstName(firstName.toUpperCase());
        }
        
        // You can add more processing logic here as needed
        return person;
    }
    
}
