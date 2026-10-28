package com.sft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartFinanceApplication.class, args);
    }
}
