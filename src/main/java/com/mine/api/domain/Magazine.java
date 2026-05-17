package com.mine.api.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "magazines")
public class Magazine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "moodboard_image_url", length = 1000)
    private String moodboardImageUrl;

    @Column(name = "moodboard_description", columnDefinition = "TEXT")
    private String moodboardDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "moodboard_status", length = 20)
    private MoodboardStatus moodboardStatus = MoodboardStatus.PENDING;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @com.fasterxml.jackson.annotation.JsonManagedReference
    @OneToMany(mappedBy = "magazine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<MagazineSection> sections = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "magazine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MagazineInteraction> interactions = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "magazine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MagazineLike> likes = new ArrayList<>();

    @Version
    private Long version;

    @Builder
    public Magazine(String title, String subtitle, String introduction, String coverImageUrl,
            String tags, String moodboardImageUrl, String moodboardDescription,
            MoodboardStatus moodboardStatus, User user) {
        this.title = title;
        this.subtitle = subtitle;
        this.introduction = introduction;
        this.coverImageUrl = coverImageUrl;
        this.tags = tags;
        this.moodboardImageUrl = moodboardImageUrl;
        this.moodboardDescription = moodboardDescription;
        this.moodboardStatus = moodboardStatus != null
                ? moodboardStatus
                : (moodboardImageUrl != null ? MoodboardStatus.COMPLETED : MoodboardStatus.PENDING);
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    public void addSection(MagazineSection section) {
        sections.add(section);
        section.setMagazine(this);
    }

    public void updateInfo(String title, String introduction) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
        if (introduction != null && !introduction.trim().isEmpty()) {
            this.introduction = introduction;
        }
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void setMoodboardImageUrl(String moodboardImageUrl) {
        this.moodboardImageUrl = moodboardImageUrl;
    }

    public void setMoodboardDescription(String moodboardDescription) {
        this.moodboardDescription = moodboardDescription;
    }

    public void markMoodboardPending() {
        this.moodboardStatus = MoodboardStatus.PENDING;
    }

    public void completeMoodboard(String moodboardImageUrl, String moodboardDescription) {
        this.moodboardImageUrl = moodboardImageUrl;
        this.moodboardDescription = moodboardDescription;
        this.moodboardStatus = MoodboardStatus.COMPLETED;
    }

    public void failMoodboard() {
        this.moodboardStatus = MoodboardStatus.FAILED;
    }

    public MoodboardStatus getMoodboardStatus() {
        if (this.moodboardStatus != null) {
            return this.moodboardStatus;
        }
        return this.moodboardImageUrl != null ? MoodboardStatus.COMPLETED : MoodboardStatus.PENDING;
    }

    public String getRepresentativeImageUrl() {
        if (hasText(coverImageUrl)) {
            return coverImageUrl;
        }

        for (MagazineSection section : sections) {
            if (section == null) {
                continue;
            }

            if (hasText(section.getThumbnailUrl())) {
                return section.getThumbnailUrl();
            }

            for (Paragraph paragraph : section.getParagraphs()) {
                if (paragraph != null && hasText(paragraph.getImageUrl())) {
                    return paragraph.getImageUrl();
                }
            }
        }

        return hasText(moodboardImageUrl) ? moodboardImageUrl : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }
}
