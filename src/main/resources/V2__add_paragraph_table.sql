-- ========================================
-- MagazineSection paragraphs 구조 변경
-- 변경일: 2026-02-07
-- ========================================

-- 1. paragraph 테이블 생성
CREATE TABLE IF NOT EXISTS paragraph (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id BIGINT NOT NULL,
    subtitle VARCHAR(200) NOT NULL,
    text TEXT NOT NULL,
    image_url VARCHAR(500),
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (section_id) REFERENCES magazine_sections(id) ON DELETE CASCADE
);

-- 2. magazine_sections에 thumbnail_url 컬럼 추가 (없으면)
ALTER TABLE magazine_sections 
ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(500);

-- 3. 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_paragraph_section_id ON paragraph(section_id);
CREATE INDEX IF NOT EXISTS idx_paragraph_display_order ON paragraph(section_id, display_order);

-- 4. 기존 image_url을 thumbnail_url로 복사 (선택, 한 번만 실행)
-- UPDATE magazine_sections 
-- SET thumbnail_url = image_url 
-- WHERE image_url IS NOT NULL AND thumbnail_url IS NULL;
