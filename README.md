# Capstone Project - Backend

Spring Boot 기반 백엔드 프로젝트입니다. BitStudio AI API를 활용한 Virtual Try-On 기능을 제공하며, Docker 환경에서 PostgreSQL과 Redis와 함께 실행됩니다.

## 📋 목차

- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [프로젝트 구조](#프로젝트-구조)
- [사전 요구사항](#사전-요구사항)
- [환경 설정](#환경-설정)
- [실행 방법](#실행-방법)
- [API 문서](#api-문서)
- [환경변수 설명](#환경변수-설명)
- [개발 가이드](#개발-가이드)

## 🛠 기술 스택

- **Java**: 21
- **Spring Boot**: 4.0.1
- **Database**: PostgreSQL 16
- **AI Engine**: **Google Gemini 1.5 Flash** (가상 피팅) & **Google Cloud Vision** (의류 분석)
- **Cache**: Redis 7
- **ORM**: Spring Data JPA
- **HTTP Client**: Spring WebFlux (WebClient)
- **External API**: BitStudio AI API
- **Build Tool**: Gradle
- **Container**: Docker & Docker Compose
- **CI/CD**: GitHub Actions


## 핵심 기능: AI 가상 피팅
사용자의 사진에 상/하의를 자연스럽게 합성하는 기능을 제공합니다.

- **비동기 처리**: 고해상도 생성 시에도 서버 안정성 유지
- **이미지 최적화**: 자동 리사이징 및 타임아웃 방지 로직 적용

## 📦 사전 요구사항

다음 도구들이 설치되어 있어야 합니다:

- **Docker** (20.10 이상)
- **Docker Compose** (2.0 이상)
- **Java 21** (로컬 개발 시)
- **Gradle** (로컬 개발 시)

## ⚙️ 환경 설정

### 1. 환경변수 파일 생성

프로젝트 루트 디렉토리에 `.env` 파일을 생성하세요.

```bash
# .env.example 파일을 복사하여 .env 파일 생성
cp .env.example .env

# Windows의 경우
copy .env.example .env
```
## 사용자 인증 및 권한 시스템
### 1. 소셜 로그인 연동 (OAuth 2.0)
- **카카오(Kakao) & 구글(Google) 로그인**
- **자동 데이터 동기화**

### 2. JWT(JSON Web Token) 기반 인증
- **Stateless 인증**
- **보안 입장권**

### 3. API 테스트 가이드 (중요)
프로젝트 실행 후 아래 순서대로 기능을 테스트할 수 있습니다.

1. **로그인 진입**: 브라우저에서 아래 주소 접속
   - 카카오: `http://localhost:8080/oauth2/authorization/kakao`
   - 구글: `http://localhost:8080/oauth2/authorization/google`
2. **토큰 획득**: 로그인 완료 후 리다이렉트된 Swagger 주소창의 `?token=...` 
3. **권한 승인**: [Swagger UI](http://localhost:8080/swagger-ui/index.html) 
4. **기능 확인**: 이후 잠금 표시가 풀린 모든 API를 자유롭게 호출 가능

`.env` 파일의 값들은 프로젝트에 맞게 수정할 수 있습니다. 기본값으로도 동작합니다.

## ⚙️ 환경 설정 (.env)
다음 값들을 .env 파일에 추가해야 소셜 로그인이 작동합니다.

KAKAO_CLIENT_ID=15ec7d8923c...
KAKAO_CLIENT_SECRET=y5nI1D0X6M...
JWT_SECRET_KEY=vmlmZS1zZWNyZXQta2V5LWZvci1jYXBzdG9uZS1wcm9qZWN0LWZpdHRpbmctc2VydmljZQ==
## 🚀 실행 방법

### 처음 시작하기 (초기 설정)

1. **환경변수 파일 생성**
   ```bash
   # .env.example을 복사하여 .env 파일 생성
   cp .env.example .env
   # Windows: copy .env.example .env
   ```

2. **Docker Desktop 실행 확인**
   - Docker Desktop이 실행 중인지 확인하세요.

3. **PostgreSQL 및 Redis 컨테이너 시작**
   ```bash
   docker-compose up -d postgres redis
   ```
   - PostgreSQL과 Redis가 완전히 시작될 때까지 약 10-20초 대기하세요.

4. **애플리케이션 실행**
   ```bash
   ./gradlew bootRun
   # Windows: gradlew.bat bootRun
   ```

### Docker Compose를 사용한 실행 (권장)

#### 전체 환경 실행

```bash
docker-compose up -d
```

#### 로그 확인

```bash
# 전체 로그 확인
docker-compose logs -f

# 애플리케이션 로그만 확인
docker-compose logs -f app

# PostgreSQL 로그만 확인
docker-compose logs -f postgres
```

#### 컨테이너 중지

```bash
# 컨테이너 중지 (데이터는 유지됨)
docker-compose stop

# 컨테이너 중지 및 제거 (데이터는 유지됨)
docker-compose down

# 컨테이너 중지 및 제거 + 볼륨 삭제 (데이터 삭제)
docker-compose down -v
```

#### 컨테이너 재빌드

```bash
# 이미지 재빌드 후 실행
docker-compose up -d --build
```

### 로컬 개발 환경 실행

#### 1. PostgreSQL 및 Redis 컨테이너 실행

```bash
docker-compose up -d postgres redis
```

#### 2. 애플리케이션 실행

```bash
# Gradle을 사용한 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/*.jar
```

## 💻 개발 가이드

### 데이터베이스 접속

#### PostgreSQL 접속

Docker Compose로 실행한 경우:

```bash
# PostgreSQL 컨테이너에 접속
docker exec -it capstone-postgres psql -U capstone_user -d capstone_db
```

로컬 PostgreSQL 클라이언트 사용 시:

```bash
psql -h localhost -p 5432 -U capstone_user -d capstone_db
```

#### Redis 접속

Docker Compose로 실행한 경우:

```bash
# Redis 컨테이너에 접속
docker exec -it capstone-redis redis-cli
```

로컬 Redis 클라이언트 사용 시:

```bash
redis-cli -h localhost -p 6379
```

### API 테스트

애플리케이션이 실행되면 다음 URL로 접속할 수 있습니다:

- **애플리케이션**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health (Actuator 설정 시)

**Postman 또는 다른 API 클라이언트를 사용하여 API를 테스트할 수 있습니다.**

### 빌드 및 테스트

```bash
# 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 테스트 제외하고 빌드
./gradlew build -x test
```

### 데이터베이스 초기화

`init.sql` 파일에 초기 데이터베이스 스키마나 데이터를 추가할 수 있습니다. 이 파일은 PostgreSQL 컨테이너가 처음 시작될 때 자동으로 실행됩니다.

```bash
# 특정 서비스 재시작
docker-compose restart app

# 전체 재시작
docker-compose restart
```
