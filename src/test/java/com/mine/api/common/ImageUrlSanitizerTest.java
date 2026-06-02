package com.mine.api.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImageUrlSanitizerTest {

    @Test
    @DisplayName("기본 이미지 URL은 null로 정규화한다")
    void nullIfDefault_DefaultImage_ReturnsNull() {
        assertNull(ImageUrlSanitizer.nullIfDefault(
                "https://mine-moodboard-bucket.s3.ap-southeast-2.amazonaws.com/assets/default-placeholder.png"));
        assertNull(ImageUrlSanitizer.nullIfDefault(
                "https://mine-moodboard-bucket.s3.ap-southeast-2.amazonaws.com/assets/default-thumbnail.png"));
    }

    @Test
    @DisplayName("중복 이미지 URL은 두 번째 사용부터 null로 정규화한다")
    void nullIfDefaultOrDuplicate_DuplicateImage_ReturnsNull() {
        Set<String> usedImageUrls = new HashSet<>();

        assertEquals(
                "https://images.pexels.com/photos/1/photo.jpg?auto=compress&cs=tinysrgb&w=1200",
                ImageUrlSanitizer.nullIfDefaultOrDuplicate(
                        "https://images.pexels.com/photos/1/photo.jpg",
                        usedImageUrls));
        assertNull(ImageUrlSanitizer.nullIfDefaultOrDuplicate(
                "https://images.pexels.com/photos/1/photo.jpg",
                usedImageUrls));
    }

    @Test
    @DisplayName("Pexels의 같은 사진은 크기 쿼리가 달라도 중복으로 처리한다")
    void nullIfDefaultOrDuplicate_PexelsSamePathWithDifferentQuery_ReturnsNull() {
        Set<String> usedImageUrls = new HashSet<>();

        assertEquals(
                "https://images.pexels.com/photos/123/pexels-photo-123.jpeg?auto=compress&cs=tinysrgb&w=1200",
                ImageUrlSanitizer.nullIfDefaultOrDuplicate(
                        "https://images.pexels.com/photos/123/pexels-photo-123.jpeg?auto=compress&cs=tinysrgb&w=150",
                        usedImageUrls));
        assertNull(ImageUrlSanitizer.nullIfDefaultOrDuplicate(
                "https://images.pexels.com/photos/123/pexels-photo-123.jpeg?auto=compress&cs=tinysrgb&w=900",
                usedImageUrls));
    }
}
