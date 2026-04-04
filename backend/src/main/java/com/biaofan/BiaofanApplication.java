package com.biaofan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.biaofan.mapper")
public class BiaofanApplication {
    public static void main(String[] args) {
        SpringApplication.run(BiaofanApplication.class, args);
    }
}
