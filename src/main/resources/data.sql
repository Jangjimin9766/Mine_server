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
-- 공용 계정: shared_user (팀 공용)
-- 개인 계정: antigravity_user (테스트용)
-- =====================================================

INSERT INTO users (username, password, email, nickname, created_at, is_public) 
VALUES ('shared_user', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqtqzZME3xpB8vCqMRK3H.KxD6Qdi', 'shared@mine.com', '공용테스터', NOW(), true)
ON DUPLICATE KEY UPDATE username=username;

INSERT INTO users (username, password, email, nickname, created_at, is_public) 
VALUES ('antigravity_user', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqtqzZME3xpB8vCqMRK3H.KxD6Qdi', 'antigravity@mine.com', '인재테스터', NOW(), true)
ON DUPLICATE KEY UPDATE username=username;

-- =====================================================
-- 더미 매거진 데이터 (shared_user 소유)
-- =====================================================

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '겨울 패션 트렌드 2025', '따뜻함과 스타일을 동시에', '올 겨울 꼭 알아야 할 패션 트렌드를 소개합니다.',
       'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=800', '패션,겨울,트렌드', NOW()
FROM users u WHERE u.username = 'shared_user'
ON DUPLICATE KEY UPDATE title=title;

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '서울 카페 투어', '숨겨진 명소를 찾아서', '서울의 감성 카페들을 모아봤습니다.',
       'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800', '카페,서울,여행', NOW()
FROM users u WHERE u.username = 'shared_user'
ON DUPLICATE KEY UPDATE title=title;

-- =====================================================
-- 더미 섹션 데이터 (첫 번째 매거진용)
-- =====================================================

-- NOTE: 기존 스키마에서 사용하던 더미 섹션 INSERT 문.
-- 현재 테이블에는 content / image_url 컬럼이 없으므로 실행 시 에러가 발생하여 주석 처리함.
-- 애플리케이션 동작에는 필수 데이터가 아니므로, 필요 시 스키마에 맞게 수정 후 다시 활성화할 것.
-- INSERT INTO magazine_sections (magazine_id, heading, content, image_url, layout_type, display_order)
-- SELECT m.id, '니트의 귀환', '<p>올 겨울, 니트가 다시 돌아왔습니다. 오버사이즈 니트부터 크롭 니트까지 다양한 스타일링을 만나보세요.</p>',
--        'https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=800', 'split_left', 0
-- FROM magazines m JOIN users u ON m.user_id = u.id 
-- WHERE u.username = 'shared_user' AND m.title LIKE '%겨울 패션%'
-- LIMIT 1
-- ON DUPLICATE KEY UPDATE heading=heading;

-- INSERT INTO magazine_sections (magazine_id, heading, content, image_url, layout_type, display_order)
-- SELECT m.id, '레이어드의 정석', '<p>추운 겨울, 레이어드는 필수입니다. 얇은 옷을 여러 겹 겹쳐 입어 스타일과 보온성을 모두 잡으세요.</p>',
--        'https://images.unsplash.com/photo-1539533018447-63fcce2678e3?w=800', 'split_right', 1
-- FROM magazines m JOIN users u ON m.user_id = u.id 
-- WHERE u.username = 'shared_user' AND m.title LIKE '%겨울 패션%'
-- LIMIT 1
-- ON DUPLICATE KEY UPDATE heading=heading;

-- INSERT INTO magazine_sections (magazine_id, heading, content, image_url, layout_type, display_order)
-- SELECT m.id, '컬러 매치 팁', '<p>겨울이라고 무채색만? 아니요! 버건디, 머스타드, 포레스트 그린 등 깊은 컬러로 포인트를 주세요.</p>',
--        'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=800', 'basic', 2
-- FROM magazines m JOIN users u ON m.user_id = u.id 
-- WHERE u.username = 'shared_user' AND m.title LIKE '%겨울 패션%'
-- LIMIT 1
-- ON DUPLICATE KEY UPDATE heading=heading;

-- 마이그레이션: 기존 유저 public 설정 보장

