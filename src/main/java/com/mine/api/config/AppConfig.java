package com.mine.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 연결 타임아웃: 10초 (서버 연결 시도 시간)
        factory.setConnectTimeout(10000);

        // 읽기 타임아웃: 90초 (AI 응답 대기 시간)
        // Python AI 서버의 GPT-3.5-turbo 처리 시간(~5초)을 충분히 커버
        factory.setReadTimeout(90000);

        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}
