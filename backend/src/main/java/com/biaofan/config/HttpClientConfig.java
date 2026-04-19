package com.biaofan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;

@Component
public class HttpClientConfig {
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .executor(Executors.newFixedThreadPool(20))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
}
