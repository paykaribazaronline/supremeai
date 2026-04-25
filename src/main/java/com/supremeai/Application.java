package com.supremeai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {
    "com.supremeai",
    "com.supremeai.config",
    "com.supremeai.service",
    "com.supremeai.controller",
    "com.supremeai.selfhealing"
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}