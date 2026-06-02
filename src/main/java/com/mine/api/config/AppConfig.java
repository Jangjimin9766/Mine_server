package com.mine.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
// @Async 어노테이션 활성화 — MagazineService, MoodboardService의 비동기 메서드에 필요
@org.springframework.scheduling.annotation.EnableAsync
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 연결 타임아웃: 10초 (서버 연결 시도 시간)
        factory.setConnectTimeout(10000);

        // 읽기 타임아웃: 5분 — AI 생성 및 이미지 처리 지연을 커버
        factory.setReadTimeout(300000);

        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}
