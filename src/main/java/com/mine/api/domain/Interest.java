package com.mine.api.domain;

public enum Interest {
    LIFESTYLE("생활"),
    SPORTS("스포츠"),
    MUSIC("음악"),
    TRAVEL("여행"),
    ART("예술"),
    BEAUTY("뷰티"),
    FOOD("푸드"),
    HEALTH("건강"),
    FASHION("패션"),
    TECH("테크");

    private final String displayName;

    Interest(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
