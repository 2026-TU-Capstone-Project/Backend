-- PostgreSQL 초기화 스크립트
-- Docker PostgreSQL 이미지는 POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD env로 DB/유저를 자동 생성함
-- 아래는 PostgreSQL 15+ 에서 public 스키마 권한 보장용

\c capstone_db

-- pgvector 확장 활성화 (벡터 유사도 검색용)
CREATE EXTENSION IF NOT EXISTS vector;

GRANT ALL ON SCHEMA public TO capstone_user;
GRANT CREATE ON SCHEMA public TO capstone_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO capstone_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO capstone_user;
