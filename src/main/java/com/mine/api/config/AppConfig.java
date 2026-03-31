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

        // 읽기 타임아웃: 180초 — RunPod 콜드스타트(~30초) + SDXL 이미지 생성(~60초) 커버
        factory.setReadTimeout(180000);

        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}
