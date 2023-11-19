package com.example.middleware.schedule;

import com.example.middleware.schedule.annotation.EnableMyScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMyScheduling
public class MyScheduleSpringStarterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyScheduleSpringStarterApplication.class, args);
        while (true);
    }

}
