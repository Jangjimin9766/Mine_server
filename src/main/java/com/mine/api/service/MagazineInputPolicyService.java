package com.mine.api.service;

import com.mine.api.dto.MagazineCreateRequest;
import com.mine.api.dto.MagazineGenerationRequest;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class MagazineInputPolicyService {

    private static final Pattern SYMBOL_ONLY_PATTERN = Pattern.compile("^[\\p{Punct}\\p{IsPunctuation}\\s]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://).+", Pattern.CASE_INSENSITIVE);
    private static final Pattern POLITICAL_MOBILIZATION_PATTERN = Pattern.compile(
            "(민주당|국민의힘|정당|대통령|정권).*(지지|투표|선동|타도|죽어|죽여|몰아내)"
                    + "|(지지|투표|선동|타도|죽어|죽여|몰아내).*(민주당|국민의힘|정당|대통령|정권)");

    private static final Map<String, List<String>> BLOCKED_TERMS = new LinkedHashMap<>();

    static {
        BLOCKED_TERMS.put("sexual", List.of(
                "야동", "19금", "섹스", "섹파", "원나잇", "자위", "오르가즘", "오랄", "애무",
                "보지", "자지", "좆", "젖꼭지", "nudes", "porn", "seggs", "콘"));
        BLOCKED_TERMS.put("profanity", List.of(
                "씨발", "시발", "ㅅㅂ", "개새끼", "병신", "ㅂㅅ", "좆같", "씹새끼", "지랄",
                "꺼져", "죽을래", "엿먹어", "느금마", "호로새끼", "썅"));
        BLOCKED_TERMS.put("hate", List.of(
                "김치녀", "된장녀", "페미충", "한남충", "맘충", "메갈", "워마드", "홍어",
                "짱깨", "쪽발이", "흑인노예", "죽어라"));
        BLOCKED_TERMS.put("violence", List.of(
                "테러", "생화학테러", "폭탄", "칼로", "총으로", "패죽", "죽인다", "죽여버"));
        BLOCKED_TERMS.put("self_harm", List.of(
                "자살", "자해", "죽고싶", "끝내고싶", "리스트컷", "목매달", "투신", "약먹고죽"));
        BLOCKED_TERMS.put("drugs_or_scam", List.of(
                "필로폰", "빙두", "마약", "dm구매", "투자보장", "원금보장", "피싱"));
    }

    public void validateGenerationRequest(MagazineGenerationRequest request) {
        validateMeaningfulText("topic", request.getTopic());
        validateSafeText("topic", request.getTopic());
        validateOptionalSafeText("user_mood", request.getUserMood());
    }

    public void validateCreateRequest(MagazineCreateRequest request) {
        validateMeaningfulText("title", request.getTitle());
        validateSafeText("title", request.getTitle());
        validateOptionalSafeText("subtitle", request.getSubtitle());
        validateMeaningfulText("introduction", request.getIntroduction());
        validateSafeText("introduction", request.getIntroduction());

        if (request.getCoverImageUrl() != null && !request.getCoverImageUrl().isBlank()) {
            validateUrl("cover_image_url", request.getCoverImageUrl());
        }

        if (request.getTags() != null) {
            for (String tag : request.getTags()) {
                validateMeaningfulText("tag", tag);
                validateSafeText("tag", tag);
            }
        }

        if (request.getSections() != null) {
            for (MagazineCreateRequest.SectionDto section : request.getSections()) {
                validateMeaningfulText("section.heading", section.getHeading());
                validateSafeText("section.heading", section.getHeading());

                if (section.getThumbnailUrl() != null && !section.getThumbnailUrl().isBlank()) {
                    validateUrl("section.thumbnail_url", section.getThumbnailUrl());
                }

                if (section.getParagraphs() != null) {
                    for (MagazineCreateRequest.ParagraphDto paragraph : section.getParagraphs()) {
                        validateOptionalSafeText("paragraph.subtitle", paragraph.getSubtitle());
                        validateMeaningfulText("paragraph.text", paragraph.getText());
                        validateSafeText("paragraph.text", paragraph.getText());

                        if (paragraph.getImageUrl() != null && !paragraph.getImageUrl().isBlank()) {
                            validateUrl("paragraph.image_url", paragraph.getImageUrl());
                        }
                    }
                }
            }
        }
    }

    private void validateOptionalSafeText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        validateMeaningfulText(fieldName, value);
        validateSafeText(fieldName, value);
    }

    private void validateMeaningfulText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        if (SYMBOL_ONLY_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must contain meaningful text");
        }
    }

    private void validateSafeText(String fieldName, String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must contain meaningful text");
        }

        for (Map.Entry<String, List<String>> entry : BLOCKED_TERMS.entrySet()) {
            for (String term : entry.getValue()) {
                if (normalized.contains(normalize(term))) {
                    throw new IllegalArgumentException(
                            fieldName + " contains blocked " + entry.getKey() + " content");
                }
            }
        }

        if (POLITICAL_MOBILIZATION_PATTERN.matcher(normalized).find()) {
            throw new IllegalArgumentException(fieldName + " contains blocked political agitation content");
        }
    }

    private void validateUrl(String fieldName, String value) {
        if (!URL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a valid http/https URL");
        }
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        normalized = normalized.replace('1', 'i');
        normalized = normalized.replace('0', 'o');
        normalized = normalized.replace('3', 'e');
        normalized = normalized.replace('5', 's');
        normalized = normalized.replace('7', 't');
        return normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHangul}]+", "");
    }
}
