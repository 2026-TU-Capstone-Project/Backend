-- 1. 데이터베이스가 없다면 생성 (보통 환경변수로 자동생성되지만 명시하는 게 안전함)
CREATE DATABASE IF NOT EXISTS capstone_db;

-- 1. 외부 접속(DBeaver)용 권한 부여
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP 
ON capstone_db.* TO 'capstone_user'@'%';

-- 2. 내부 접속(서버 앱)용 권한 부여 (이 줄이 빠져서 에러가 난 것입니다!)
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP 
ON capstone_db.* TO 'capstone_user'@'localhost';

FLUSH PRIVILEGES;