package com.openisle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpenIsleApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenIsleApplication.class, args);
    }
}
