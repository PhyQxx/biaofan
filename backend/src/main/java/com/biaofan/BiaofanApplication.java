package com.biaofan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 标帆SOP系统主启动类
 * 提供SOP模板管理、执行跟踪、市场分享等功能
 */
@SpringBootApplication(exclude = { MailSenderAutoConfiguration.class })
@MapperScan("com.biaofan.mapper")
@EnableScheduling
@EnableAsync
public class BiaofanApplication {
    public static void main(String[] args) {
        SpringApplication.run(BiaofanApplication.class, args);
    }
}
