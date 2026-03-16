package com.mine.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@org.springframework.scheduling.annotation.EnableAsync
public class MineApplication {

    public static void main(String[] args) {
        SpringApplication.run(MineApplication.class, args);
    }

}
