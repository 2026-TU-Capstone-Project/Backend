-- 초기 데이터베이스 설정 스크립트
-- MySQL 컨테이너가 처음 시작될 때 자동으로 실행됩니다
-- 이 스크립트는 MYSQL_USER 환경변수로 생성된 사용자에게 추가 권한을 부여합니다

-- 사용자 생성 (이미 MYSQL_USER로 생성되었지만, localhost 접근을 위해 명시적으로 생성)
CREATE USER IF NOT EXISTS 'user'@'localhost' IDENTIFIED BY 'password';
CREATE USER IF NOT EXISTS 'user'@'%' IDENTIFIED BY 'password';

-- 사용자 권한 명시적 부여
GRANT ALL PRIVILEGES ON capstone_db.* TO 'user'@'localhost';
GRANT ALL PRIVILEGES ON capstone_db.* TO 'user'@'%';

-- 권한 새로고침
FLUSH PRIVILEGES;

-- 필요시 여기에 초기 테이블이나 데이터를 추가할 수 있습니다
-- 예시: 초기 테이블 생성
-- CREATE TABLE IF NOT EXISTS example_table (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     name VARCHAR(255) NOT NULL,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- );




