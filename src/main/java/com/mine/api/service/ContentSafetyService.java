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
            // 성적/음란 표현
            "강간",
            "성폭행",
            "성추행",
            "성희롱",
            "몰카",
            "리벤지포르노",
            "불법촬영",
            "음란",
            "포르노",
            "야동",
            "섹스",
            "성매매",
            "매춘",
            "나체",
            "누드",
            "자위",
            "변태",
            "porn",
            "porno",
            "sex",
            "nude",

            // 자해/극단적 선택
            "자살",
            "자해",
            "극단적선택",
            "목매달",
            "투신",
            "suicide",
            "selfharm",

            // 폭력/범죄/위험물
            "살인",
            "살해",
            "죽여",
            "죽이는법",
            "암살",
            "납치",
            "협박",
            "고문",
            "폭행",
            "흉기",
            "칼부림",
            "테러",
            "폭탄",
            "총기",
            "총살",
            "방화",
            "도박",
            "해킹",
            "피싱",
            "마약",
            "대마초",
            "필로폰",
            "코카인",
            "헤로인",
            "마약거래",
            "terror",
            "bomb",
            "weapon",
            "murder",
            "kill",
            "drug",
            "cocaine",
            "heroin",

            // 혐오/차별/극단주의
            "혐오",
            "장애인비하",
            "인종차별",
            "성차별",
            "동성애혐오",
            "나치",
            "히틀러",
            "nazi",
            "hitler",

            // 욕설/비속어
            "씨발",
            "시발",
            "ㅅㅂ",
            "개새끼",
            "병신",
            "븅신",
            "지랄",
            "좆",
            "존나",
            "꺼져",
            "fuck",
            "shit",
            "bitch",
            "asshole");

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
