DELIMITER $$

DROP PROCEDURE IF EXISTS generate_dummy_data$$

CREATE PROCEDURE generate_dummy_data()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE batch_size INT DEFAULT 1000; -- 한 번에 1000개씩 커밋
    DECLARE max_rows INT DEFAULT 1000000; -- 총 100만 건
    DECLARE user_id BIGINT;
    
    -- 테스트용 유저 ID 가져오기 (shared_user)
    SELECT id INTO user_id FROM users WHERE username = 'shared_user' LIMIT 1;
    
    -- 없으면 생성
    IF user_id IS NULL THEN
        INSERT INTO users (username, email, password, nickname, role, created_at, updated_at, deleted) 
        VALUES ('shared_user', 'shared@mine.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqtqzZME3xpB8vCqMRK3H.KxD6Qdi', 'Tester', 'USER', NOW(), NOW(), false);
        SET user_id = LAST_INSERT_ID();
    END IF;

    -- 기존 데이터 삭제 (옵션)
    -- TRUNCATE TABLE magazine_sections;
    -- DELETE FROM magazines WHERE user_id = user_id;

    START TRANSACTION;
    
    WHILE i < max_rows DO
        INSERT INTO magazines (user_id, title, subtitle, introduction, cover_image_url, tags, is_public, created_at, version)
        VALUES (
            user_id,
            CONCAT('Performance Test Magazine ', i),
            CONCAT('Subtitle ', i),
            CONCAT('This is a test introduction for performance testing. Content number ', i, '. We need to check full-text search performance.'),
            'https://example.com/cover.jpg',
            CONCAT('test,performance,dummy,tag', i % 100),
            true,
            NOW(),
            0
        );
        
        SET i = i + 1;
        
        IF i % batch_size = 0 THEN
            COMMIT;
            START TRANSACTION;
        END IF;
    END WHILE;
    
    COMMIT;
END$$

DELIMITER ;

-- 프로시저 실행 (100만 건 생성)
CALL generate_dummy_data();

-- 인덱스 생성 (데이터 생성 후 실행 권장)
-- 이미 존재하면 에러가 날 수 있으므로 체크하거나 예외처리 필요하지만, 여기서는 단순 실행
-- MySQL 8.0의 IF NOT EXISTS 문법 활용 (인덱스에는 직접 지원 안되므로 프로시저 안쓰면 DROP 후 CREATE가 일반적)

-- Full-Text Index 추가 (Magazine)
ALTER TABLE magazines ADD FULLTEXT INDEX idx_ft_magazine_search (title, introduction, tags);

-- Full-Text Index 추가 (Section)
-- ALTER TABLE magazine_sections ADD FULLTEXT INDEX idx_ft_section_search (heading, content);
