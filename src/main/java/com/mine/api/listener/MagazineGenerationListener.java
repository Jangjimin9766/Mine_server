package com.mine.api.listener;

import com.mine.api.event.UserSignupEvent;
import com.mine.api.service.MagazineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MagazineGenerationListener {

    private final MagazineService magazineService;

    /**
     * 회원가입 완료 트랜잭션 커밋 직후 발생하는 이벤트를 수신하여 서비스의 비동기 메서드 호출
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserSignupEvent(UserSignupEvent event) {
        log.info("Received UserSignupEvent for user: {}. Triggering async magazine generation via service.", 
                event.getUser().getUsername());
        // 외부 빈(MagazineService)의 @Async 메서드를 호출하므로 정상적으로 별도 스레드에서 작동함
        magazineService.generateInitialMagazinesAsync(event.getUser().getUsername());
    }
}
