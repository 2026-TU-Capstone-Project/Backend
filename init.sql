-- 1. 데이터베이스가 없으면 생성
CREATE DATABASE IF NOT EXISTS capstone_db;

-- 2. 유저가 없으면 생성 (비밀번호 없음)
-- MySQL 5.7+ / 8.0+ 공용 문법
CREATE USER IF NOT EXISTS 'capstone_user'@'%'
CREATE USER IF NOT EXISTS 'capstone_user'@'localhost' 

-- 3. 권한 부여
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP 
ON capstone_db.* TO 'capstone_user'@'%';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP 
ON capstone_db.* TO 'capstone_user'@'localhost';

-- 4. 변경 사항 적용
FLUSH PRIVILEGES;