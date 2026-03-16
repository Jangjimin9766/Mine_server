package com.mine.api.listener;

import com.mine.api.domain.User;
import com.mine.api.event.UserSignupEvent;
import com.mine.api.service.MagazineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MagazineGenerationListener {

    private final MagazineService magazineService;

    /**
     * 회원가입 완료 트랜잭션 커밋 직후 발생하는 이벤트를 수신하여 비동기 메서드 호출
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserSignupEvent(UserSignupEvent event) {
        log.info("Received UserSignupEvent for user: {}. Triggering async magazine generation via proxy.", 
                event.getUser().getUsername());
        // 외부 빈(Listener)에서 호출하므로 @Async가 정상 작동함
        generateInitialMagazinesAsync(event.getUser(), event.getInterests());
    }

    /**
     * 회원가입 직후 관심사 기반 매거진 자동 생성 (비동기)
     */
    @Async
    public void generateInitialMagazinesAsync(User user, List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            log.warn("No interests found for user: {}. Skipping initial magazine generation.", user.getUsername());
            return;
        }

        // 1. 관심사 목록 중복 방지 및 복사본 생성
        List<String> modifiableInterests = new ArrayList<>(interests);
        
        // 2. 관심사 목록을 무작위로 섞음
        Collections.shuffle(modifiableInterests);
        
        // 3. 최대 2개까지만 리스트를 자름
        List<String> targetInterests = modifiableInterests.subList(0, Math.min(2, modifiableInterests.size()));

        log.info("Starting initial magazine generation for user: {} with interests: {}", user.getUsername(), targetInterests);

        for (String interestCode : targetInterests) {
            try {
                // 각 관심사를 주제로 매거진 생성 요청
                com.mine.api.dto.MagazineGenerationRequest genRequest = new com.mine.api.dto.MagazineGenerationRequest();
                genRequest.setTopic(interestCode);
                genRequest.setUserMood("vibrant");

                log.info("Generating welcome magazine for interest: {} (User: {})", interestCode, user.getUsername());
                // MagazineService의 트랜잭션 메서드 호출 (새로운 트랜잭션으로 실행됨)
                magazineService.generateAndSaveMagazine(genRequest, user.getUsername());
                
                // AI 서버 부하 분산을 위해 짧은 지연
                Thread.sleep(5000); 
                
            } catch (Exception e) {
                log.error("Failed to generate initial magazine for interest: {} (User: {})", interestCode, user.getUsername(), e);
            }
        }
        log.info("Completed initial magazine generation for user: {}", user.getUsername());
    }
}
