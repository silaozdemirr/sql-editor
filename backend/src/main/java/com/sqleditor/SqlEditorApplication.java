package com.sqleditor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SqlEditorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SqlEditorApplication.class, args);
    }
}
