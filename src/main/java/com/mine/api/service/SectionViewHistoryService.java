package com.mine.api.service;

import com.mine.api.domain.MagazineSection;
import com.mine.api.domain.SectionViewHistory;
import com.mine.api.domain.User;
import com.mine.api.dto.SectionViewHistoryDto;
import com.mine.api.repository.SectionViewHistoryRepository;
import com.mine.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectionViewHistoryService {

    private static final int MAX_HISTORY_COUNT = 30;
    private static final int RETENTION_DAYS = 30;

    private final SectionViewHistoryRepository viewHistoryRepository;
    private final UserRepository userRepository;

    /**
     * 섹션 열람 기록 저장
     */
    @Transactional
    public void recordView(User user, MagazineSection section) {
        // 기존 기록이 있으면 시간만 업데이트
        var existingHistory = viewHistoryRepository.findByUserAndSection(user, section);
        if (existingHistory.isPresent()) {
            existingHistory.get().updateViewedAt();
            return;
        }

        // 30개 초과 시 가장 오래된 기록 삭제
        long currentCount = viewHistoryRepository.countByUser(user);
        if (currentCount >= MAX_HISTORY_COUNT) {
            int deleteCount = (int) (currentCount - MAX_HISTORY_COUNT + 1);
            viewHistoryRepository.deleteOldestByUser(user, deleteCount);
        }

        // 새 기록 저장
        SectionViewHistory history = SectionViewHistory.builder()
                .user(user)
                .section(section)
                .build();
        viewHistoryRepository.save(history);
    }

    /**
     * 최근 열람 기록 조회 (30개, 한 달 이내)
     */
    public List<SectionViewHistoryDto.Response> getRecentViews(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<SectionViewHistory> histories = viewHistoryRepository
                .findTop30ByUserAndViewedAtAfterOrderByViewedAtDesc(user, oneMonthAgo);

        return histories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 30일 이상 지난 기록 자동 삭제 (매일 새벽 3시)
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        viewHistoryRepository.deleteByViewedAtBefore(cutoff);
    }

    private SectionViewHistoryDto.Response toResponse(SectionViewHistory history) {
        MagazineSection section = history.getSection();
        return SectionViewHistoryDto.Response.builder()
                .id(section.getId())
                .heading(section.getHeading())
                .content(section.getContent())
                .imageUrl(section.getImageUrl())
                .layoutType(section.getLayoutType())
                .layoutHint(section.getLayoutHint())
                .caption(section.getCaption())
                .displayOrder(section.getDisplayOrder())
                .magazineId(section.getMagazine().getId())
                .magazineTitle(section.getMagazine().getTitle())
                .viewedAt(history.getViewedAt())
                .build();
    }
}
