package com.biaofan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Value("${ai.model.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${ai.model.read-timeout-ms:60000}")
    private int readTimeoutMs;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
                    factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
                    return factory;
                })
                .build();
    }
}
