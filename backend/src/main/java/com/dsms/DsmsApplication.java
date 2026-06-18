package com.dsms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DsmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DsmsApplication.class, args);
    }
}
