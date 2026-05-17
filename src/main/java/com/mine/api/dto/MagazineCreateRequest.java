package com.mine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "Magazine create/save request")
@Getter
@Setter
@NoArgsConstructor
public class MagazineCreateRequest {

    @Schema(description = "Magazine title", example = "Winter fashion trend", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "title is required")
    @Size(max = 150, message = "title must be 150 characters or fewer")
    private String title;

    @Schema(description = "Magazine subtitle", example = "Warm and stylish at once")
    @Size(max = 150, message = "subtitle must be 150 characters or fewer")
    private String subtitle;

    @Schema(description = "Magazine introduction", example = "A styling guide for staying warm and stylish this winter.")
    @NotBlank(message = "introduction is required")
    @Size(max = 3000, message = "introduction must be 3000 characters or fewer")
    private String introduction;

    @Schema(description = "Cover image URL", example = "https://example.com/cover.jpg")
    @JsonProperty("cover_image_url")
    @Size(max = 2048, message = "cover_image_url must be 2048 characters or fewer")
    private String coverImageUrl;

    @Schema(description = "User email for internal API", example = "user@example.com")
    @JsonProperty("user_email")
    private String userEmail;

    @Schema(description = "Tag list")
    private List<String> tags;

    @Schema(description = "Moodboard data for the magazine")
    private MoodboardResponseDto moodboard;

    @Schema(description = "Section list")
    private List<SectionDto> sections;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ParagraphDto {
        @Schema(description = "Paragraph subtitle", example = "Looks for the commute")
        private String subtitle;

        @Schema(description = "Paragraph text", example = "This season, commuting outfits focus on layering...")
        private String text;

        @Schema(description = "Paragraph image URL", example = "https://example.com/look.jpg")
        @JsonProperty("image_url")
        @Size(max = 2048, message = "paragraph image_url must be 2048 characters or fewer")
        private String imageUrl;

        @Schema(description = "Source URL for the paragraph content", example = "https://example.com/source-article")
        @JsonProperty("source_url")
        private String sourceUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SectionDto {
        @Schema(description = "Section heading", example = "Coat styling")
        private String heading;

        @Schema(description = "Section thumbnail image URL", example = "https://example.com/thumbnail.jpg")
        @JsonProperty("thumbnail_url")
        @Size(max = 2048, message = "section thumbnail_url must be 2048 characters or fewer")
        private String thumbnailUrl;

        @Schema(description = "Paragraph list")
        @NotEmpty(message = "section paragraphs must not be empty")
        @Size(max = 20, message = "section paragraphs must contain 20 items or fewer")
        @Valid
        private List<ParagraphDto> paragraphs;

        @Schema(description = "Source URL used to generate the section", example = "https://example.com/source-article")
        @JsonProperty("source_url")
        private String sourceUrl;

        @Schema(description = "Layout hint", example = "image_left")
        @JsonProperty("layout_hint")
        @Size(max = 50, message = "layout_hint must be 50 characters or fewer")
        private String layoutHint;

        @Schema(description = "Layout type", example = "hero", allowableValues = { "hero", "quote", "split_left",
                "split_right", "basic" })
        @JsonProperty("layout_type")
        @Pattern(regexp = "^(hero|quote|split_left|split_right|basic)?$", message = "layout_type is invalid")
        private String layoutType;
    }
}
