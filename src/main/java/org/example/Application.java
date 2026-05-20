package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableReactiveFirestoreRepositories
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}