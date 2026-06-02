package com.mine.api.listener;

import com.mine.api.event.UserSignupEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class MagazineGenerationListener {

    /**
     * 회원가입 완료 트랜잭션 커밋 직후 발생하는 이벤트를 수신하여 서비스의 비동기 메서드 호출
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserSignupEvent(UserSignupEvent event) {
        log.info("Received UserSignupEvent for user: {}. Initial magazine generation is disabled.",
                event.getUser().getUsername());
    }
}
