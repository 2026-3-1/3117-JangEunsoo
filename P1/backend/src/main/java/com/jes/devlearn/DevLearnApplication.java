package com.jes.devlearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DevLearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevLearnApplication.class, args);
    }

}
