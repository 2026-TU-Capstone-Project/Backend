-- MySQL 초기 권한 설정
-- MYSQL_USER로 생성된 사용자에게 DB 전체 권한 부여

GRANT ALL PRIVILEGES ON capstone_db.* TO 'user'@'%';

FLUSH PRIVILEGES;

-- 테이블 / 초기 데이터가 필요하면 아래에 추가
