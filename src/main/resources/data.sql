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
-- 현실적인 테스트 계정 (유저 5명)
-- =====================================================
-- 비밀번호: password ($2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqtqzZME3xpB8vCqMRK3H.KxD6Qdi)
-- =====================================================

INSERT INTO users (username, password, email, nickname, created_at, is_public, role, deleted) 
VALUES ('jiwoo_kim', '$2a$10$kCJc0.9IFfdx.Ku7i9vNuucIkM1GAzOkQ0N42DjdgGGfHUJ0M14Z.', 'jiwoo@example.com', '지우의일상', NOW(), true, 'USER', false)
ON DUPLICATE KEY UPDATE password=VALUES(password), role=VALUES(role), deleted=VALUES(deleted);

INSERT INTO users (username, password, email, nickname, created_at, is_public, role, deleted) 
VALUES ('minjun_lee', '$2a$10$kCJc0.9IFfdx.Ku7i9vNuucIkM1GAzOkQ0N42DjdgGGfHUJ0M14Z.', 'minjun@example.com', '민준테크', NOW(), true, 'USER', false)
ON DUPLICATE KEY UPDATE password=VALUES(password), role=VALUES(role), deleted=VALUES(deleted);

INSERT INTO users (username, password, email, nickname, created_at, is_public, role, deleted) 
VALUES ('seoyun_park', '$2a$10$kCJc0.9IFfdx.Ku7i9vNuucIkM1GAzOkQ0N42DjdgGGfHUJ0M14Z.', 'seoyun@example.com', '서윤아트', NOW(), true, 'USER', false)
ON DUPLICATE KEY UPDATE password=VALUES(password), role=VALUES(role), deleted=VALUES(deleted);

INSERT INTO users (username, password, email, nickname, created_at, is_public, role, deleted) 
VALUES ('hyejin_choi', '$2a$10$kCJc0.9IFfdx.Ku7i9vNuucIkM1GAzOkQ0N42DjdgGGfHUJ0M14Z.', 'hyejin@example.com', '혜진의서재', NOW(), true, 'USER', false)
ON DUPLICATE KEY UPDATE password=VALUES(password), role=VALUES(role), deleted=VALUES(deleted);

INSERT INTO users (username, password, email, nickname, created_at, is_public, role, deleted) 
VALUES ('taeyang_lee', '$2a$10$kCJc0.9IFfdx.Ku7i9vNuucIkM1GAzOkQ0N42DjdgGGfHUJ0M14Z.', 'taeyang@example.com', '태양스포츠', NOW(), true, 'USER', false)
ON DUPLICATE KEY UPDATE password=VALUES(password), role=VALUES(role), deleted=VALUES(deleted);

-- =====================================================
-- 유저별 매거진 데이터 (각 유저당 3개)
-- =====================================================

-- 1. 김지우 (jiwoo_kim)
INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '2025 봄 패션 예고', '화사한 색감의 귀환', '올 봄 유행할 트렌드 컬러와 스타일링을 제안합니다.', 
       'https://images.unsplash.com/photo-1520635360276-79f3dbd80916?w=800', '패션,봄,트렌드', NOW()
FROM users u WHERE u.username = 'jiwoo_kim' ON DUPLICATE KEY UPDATE title=title;

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '제주 한 달 살기 기록', '느리게 걷는 법', '제주도에서 보낸 한 달간의 여유와 기록.', 
       'https://images.unsplash.com/photo-1500835556837-99ac94a94552?w=800', '여행,제주,힐링', NOW()
FROM users u WHERE u.username = 'jiwoo_kim' ON DUPLICATE KEY UPDATE title=title;

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '매거진의 시작: 패션', 'Welcome Magazine', '지우님의 첫 번째 매거진입니다.', 
       'https://images.unsplash.com/photo-1445205170230-053b83016050?w=800', '웰컴,패션', NOW()
FROM users u WHERE u.username = 'jiwoo_kim' ON DUPLICATE KEY UPDATE title=title;

-- 2. 이민준 (minjun_lee)
INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '데스크테리어의 정석', '나만의 생산성 공간', '집중력을 높여주는 데스크 셋업 가이드.', 
       'https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=800', 'IT,인테리어,데스크', NOW()
FROM users u WHERE u.username = 'minjun_lee' ON DUPLICATE KEY UPDATE title=title;

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '2024 최고의 가젯들', '테크 라이프 스타일', '올해 직접 써보고 추천하는 IT 기기 리뷰.', 
       'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800', '테크,리뷰,IT', NOW()
FROM users u WHERE u.username = 'minjun_lee' ON DUPLICATE KEY UPDATE title=title;

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '매거진의 시작: IT', 'Welcome Magazine', '민준님의 첫 번째 IT 매거진입니다.', 
       'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800', '웰컴,IT', NOW()
FROM users u WHERE u.username = 'minjun_lee' ON DUPLICATE KEY UPDATE title=title;

-- 3. 박서윤 (seoyun_park)
INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '미니멀 라이프 인테리어', '비움의 미학', '단순함이 주는 평온함을 담은 집 꾸미기.', 
       'https://images.unsplash.com/photo-1494438639946-1ebd1d20bf85?w=800', '인테리어,미니멀', NOW()
FROM users u WHERE u.username = 'seoyun_park' ON DUPLICATE KEY UPDATE title=title;

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '전시회 나들이 가이드', '예술과 함께하는 주말', '서울 주요 갤러리 전시 리뷰와 팁.', 
       'https://images.unsplash.com/photo-1547826039-bfc35e0f1ea8?w=800', '미술,전시,아트', NOW()
FROM users u WHERE u.username = 'seoyun_park' ON DUPLICATE KEY UPDATE title=title;

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '매거진의 시작: 디자인', 'Welcome Magazine', '서윤님의 디자인 매거진입니다.', 
       'https://images.unsplash.com/photo-1551033541-20948943715c?w=800', '웰컴,디자인', NOW()
FROM users u WHERE u.username = 'seoyun_park' ON DUPLICATE KEY UPDATE title=title;

-- 4. 최혜진 (hyejin_choi)
INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '주말엔 방구석 시네마', '놓치기 아까운 수작들', 'OTT에서 바로 볼 수 있는 인생 영화 추천.', 
       'https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800', '영화,OTT,추천', NOW()
FROM users u WHERE u.username = 'hyejin_choi' ON DUPLICATE KEY UPDATE title=title;

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '심리학으로 읽는 책', '마음의 온도계', '일상의 고민을 명쾌하게 풀어주는 도서 리뷰.', 
       'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800', '독서,심리,에세이', NOW()
FROM users u WHERE u.username = 'hyejin_choi' ON DUPLICATE KEY UPDATE title=title;

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '매거진의 시작: 독서', 'Welcome Magazine', '혜진님의 독서 매거진입니다.', 
       'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=800', '웰컴,독서', NOW()
FROM users u WHERE u.username = 'hyejin_choi' ON DUPLICATE KEY UPDATE title=title;

-- 5. 이태양 (taeyang_lee)
INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '초보 캠퍼를 위한 가이드', '자연 속으로!', '첫 캠핑 준비물부터 장소 선정까지 완벽 정리.', 
       'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=800', '캠핑,여행,아웃도어', NOW()
FROM users u WHERE u.username = 'taeyang_lee' ON DUPLICATE KEY UPDATE title=title;

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '오운완: 챌린지 기록', '매일 더 건강하게', '체력 증진을 위한 30일 홈트 프로젝트.', 
       'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800', '운동,헬스,건강', NOW()
FROM users u WHERE u.username = 'taeyang_lee' ON DUPLICATE KEY UPDATE title=title;

INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, created_at)
SELECT u.id, '매거진의 시작: 스포츠', 'Welcome Magazine', '태양님의 스포츠 매거진입니다.', 
       'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800', '웰컴,스포츠', NOW()
FROM users u WHERE u.username = 'taeyang_lee' ON DUPLICATE KEY UPDATE title=title;
