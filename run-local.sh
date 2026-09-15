#!/usr/bin/env bash
# =====================================================================
# 로컬 시연용 원클릭 실행 스크립트
#   1) PostgreSQL(pgvector) + Redis 를 Docker 로 기동 (docker-compose.dev.yml)
#   2) Spring Boot 를 local 프로파일로 실행 (사진은 ./local-storage 에 저장, GCS 불필요)
#
# 사용법:
#   ./run-local.sh                                   # 같은 PC(브라우저/iOS 시뮬레이터)에서 접속
#   STORAGE_PUBLIC_BASE_URL=http://10.0.2.2:8080 ./run-local.sh      # Android 에뮬레이터
#   STORAGE_PUBLIC_BASE_URL=http://192.168.0.10:8080 ./run-local.sh  # 실기기 (맥 IP)
#   ./run-local.sh --no-docker                       # DB/Redis 를 직접 띄운 경우
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")"

USE_DOCKER=1
for arg in "$@"; do
  case "$arg" in
    --no-docker) USE_DOCKER=0 ;;
  esac
done

# ---- 0. 필수 파일 확인 ---------------------------------------------------
if [ ! -f src/main/resources/application.properties ]; then
  echo "❌ src/main/resources/application.properties 가 없습니다."
  echo "   (DB 비밀번호, Gemini API 키 등이 들어있는 개인 설정 파일 — 팀원에게 받아서 넣어주세요)"
  exit 1
fi
if [ "$USE_DOCKER" = "1" ] && [ ! -f .env ]; then
  echo "❌ .env 가 없습니다. docker-compose.dev.yml 이 POSTGRES_DB/USER/PASSWORD, SPRING_DATA_REDIS_PASSWORD 를 읽습니다."
  exit 1
fi

# ---- 1. DB / Redis -------------------------------------------------------
if [ "$USE_DOCKER" = "1" ]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "❌ docker 명령을 찾을 수 없습니다. Docker Desktop 을 설치/실행하거나,"
    echo "   PostgreSQL(pgvector, 포트 15432) 과 Redis(6379) 를 직접 띄운 뒤 ./run-local.sh --no-docker 로 실행하세요."
    exit 1
  fi
  echo "▶ PostgreSQL(pgvector) + Redis 컨테이너 기동 (docker-compose.dev.yml)"
  docker compose -f docker-compose.dev.yml up -d
  echo "▶ DB 준비 대기 중..."
  for i in $(seq 1 30); do
    if docker compose -f docker-compose.dev.yml exec -T postgres pg_isready -q 2>/dev/null; then
      echo "   ✔ PostgreSQL 준비 완료"; break
    fi
    sleep 1
    if [ "$i" = "30" ]; then echo "   ⚠ PostgreSQL 응답이 없습니다. 로그: docker compose -f docker-compose.dev.yml logs postgres"; fi
  done
fi

# ---- 2. Spring Boot (local 프로파일) --------------------------------------
export SPRING_PROFILES_ACTIVE=local
export STORAGE_PUBLIC_BASE_URL="${STORAGE_PUBLIC_BASE_URL:-http://localhost:8080}"
mkdir -p local-storage

echo "▶ 백엔드 실행: profile=local, port=${SERVER_PORT:-8080}"
echo "   이미지 URL base : ${STORAGE_PUBLIC_BASE_URL}  (앱의 API base URL 과 같아야 사진이 보입니다)"
echo "   사진 저장 폴더  : $(pwd)/local-storage"
echo "   Swagger        : http://localhost:${SERVER_PORT:-8080}/swagger-ui/index.html"
echo

exec ./gradlew bootRun
