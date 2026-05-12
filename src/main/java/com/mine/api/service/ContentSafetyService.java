package com.mine.api.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
public class ContentSafetyService {

    private static final String BLOCKED_MESSAGE = "허용되지 않는 표현이 포함되어 있습니다.";

    private static final List<String> BLOCKED_TERMS = List.of(
            "강간",
            "자살",
            "살인",
            "테러",
            "폭탄",
            "마약",
            "음란",
            "포르노",
            "야동",
            "섹스",
            "혐오",
            "nazi",
            "porn",
            "suicide",
            "terror",
            "bomb");

    public void validateText(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        String normalized = normalize(text);
        for (String blockedTerm : BLOCKED_TERMS) {
            if (normalized.contains(normalize(blockedTerm))) {
                throw new IllegalArgumentException(BLOCKED_MESSAGE);
            }
        }
    }

    public void validateTexts(Collection<String> texts) {
        if (texts == null) {
            return;
        }
        texts.forEach(this::validateText);
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^\\p{L}\\p{N}]+", "");
    }
}
