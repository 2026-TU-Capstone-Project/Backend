-- 1. 데이터베이스가 없다면 생성 (보통 환경변수로 자동생성되지만 명시하는 게 안전함)
CREATE DATABASE IF NOT EXISTS capstone_db;

-- 2. 사용자 권한 부여 (root 권한으로 실행되므로 성공해야 함)
-- 주의: 'user'@'%'가 이미 존재하지 않을 수 있으므로 IDENTIFIED BY를 함께 써주는 것이 좋습니다.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP 
ON capstone_db.* TO 'capstone_user'@'%';

-- 3. 권한 적용
FLUSH PRIVILEGES;