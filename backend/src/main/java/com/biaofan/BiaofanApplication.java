package com.biaofan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.biaofan.mapper")
@EnableScheduling
public class BiaofanApplication {
    public static void main(String[] args) {
        SpringApplication.run(BiaofanApplication.class, args);
    }
}
