-- Interest 초기 데이터
-- 애플리케이션 시작 시 자동으로 로드됨 (Spring Boot data.sql)

-- 라이프스타일
INSERT INTO interests (code, name, category) VALUES ('FASHION', '패션', '라이프스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('BEAUTY', '뷰티', '라이프스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('ACCESSORY', '악세사리', '라이프스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('DESIGN', '디자인', '라이프스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('INTERIOR', '인테리어', '라이프스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('DOLL', '인형', '라이프스타일') ON DUPLICATE KEY UPDATE name=name;

-- 문화/예술
INSERT INTO interests (code, name, category) VALUES ('MUSIC', '음악', '문화/예술') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('ART', '미술', '문화/예술') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('MUSICAL', '뮤지컬', '문화/예술') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('THEATER', '연극', '문화/예술') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('READING', '독서', '문화/예술') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('OTT', 'OTT', '문화/예술') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('DRAMA', '드라마', '문화/예술') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('MOVIE', '영화', '문화/예술') ON DUPLICATE KEY UPDATE name=name;

-- 학문
INSERT INTO interests (code, name, category) VALUES ('SCIENCE', '과학', '학문') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('SOCIETY', '사회', '학문') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('MATH', '수학', '학문') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('LANGUAGE', '언어', '학문') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('HISTORY', '역사', '학문') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('RELIGION', '종교', '학문') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('CULTURE', '문화', '학문') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('EDUCATION', '교육', '학문') ON DUPLICATE KEY UPDATE name=name;

-- 스타일
INSERT INTO interests (code, name, category) VALUES ('MINIMALISM', '미니멀리즘', '스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('RETRO', '레트로', '스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('VINTAGE', '빈티지', '스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('CYBERPUNK', '사이버펑크', '스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('TREND', '트렌드', '스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('WEATHER', '날씨', '스타일') ON DUPLICATE KEY UPDATE name=name;

-- 활동
INSERT INTO interests (code, name, category) VALUES ('SPORTS', '스포츠', '활동') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('FITNESS', '헬스', '활동') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('TRAVEL', '여행', '활동') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('CAMPING', '캠핑', '활동') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('HIKING', '등산', '활동') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('ENVIRONMENT', '환경', '활동') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('ARCHITECTURE', '건축', '활동') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('PHOTOGRAPHY', '사진', '활동') ON DUPLICATE KEY UPDATE name=name;

-- 테크/기타
INSERT INTO interests (code, name, category) VALUES ('IT', 'IT', '테크') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('ELECTRONICS', '전자기기', '테크') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('GAME', '게임', '테크') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('ANIMAL', '동물', '자연') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('PLANT', '식물', '자연') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('PSYCHOLOGY', '심리', '학문') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('FINANCE', '금융', '경제') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('INVESTMENT', '재테크', '경제') ON DUPLICATE KEY UPDATE name=name;

-- 기존 Enum에 있던 것들 (호환성)
INSERT INTO interests (code, name, category) VALUES ('LIFESTYLE', '생활', '라이프스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('FOOD', '푸드', '라이프스타일') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('HEALTH', '건강', '활동') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO interests (code, name, category) VALUES ('TECH', '테크', '테크') ON DUPLICATE KEY UPDATE name=name;

-- =====================================================
-- 테스트용 계정 (팀 공용 테스트 및 개발용)
-- =====================================================
-- 비밀번호: password (BCrypt 해시)
-- 공용 계정: shared_user (팀 공용)
-- 개인 계정: antigravity_user (테스트용)
-- =====================================================

INSERT INTO users (username, password, email, nickname, created_at) 
VALUES ('shared_user', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqtqzZME3xpB8vCqMRK3H.KxD6Qdi', 'shared@mine.com', '공용테스터', NOW())
ON DUPLICATE KEY UPDATE username=username;

INSERT INTO users (username, password, email, nickname, created_at) 
VALUES ('antigravity_user', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqtqzZME3xpB8vCqMRK3H.KxD6Qdi', 'antigravity@mine.com', '인재테스터', NOW())
ON DUPLICATE KEY UPDATE username=username;

-- =====================================================
-- 더미 데이터 (구버전 스키마 대응을 위해 일시 중단)
-- =====================================================
/*
INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
...
*/

