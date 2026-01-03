# Capstone Project - Backend

스프링부트 기반 백엔드 프로젝트입니다. Docker 환경에서 MySQL 데이터베이스와 함께 실행됩니다.

## 📋 목차

- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [사전 요구사항](#사전-요구사항)
- [환경 설정](#환경-설정)
- [실행 방법](#실행-방법)
- [환경변수 설명](#환경변수-설명)
- [개발 가이드](#개발-가이드)

## 🛠 기술 스택

- **Java**: 21
- **Spring Boot**: 4.0.1
- **Database**: MySQL 8.0
- **Cache**: Redis 7
- **ORM**: Spring Data JPA
- **Build Tool**: Gradle
- **Container**: Docker & Docker Compose
- **CI/CD**: GitHub Actions

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

`.env` 파일의 값들은 프로젝트에 맞게 수정할 수 있습니다. 기본값으로도 동작합니다.

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

3. **MySQL 및 Redis 컨테이너 시작**
   ```bash
   docker-compose up -d mysql redis
   ```
   - MySQL과 Redis가 완전히 시작될 때까지 약 10-20초 대기하세요.

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

# MySQL 로그만 확인
docker-compose logs -f mysql
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

#### 1. MySQL 및 Redis 컨테이너 실행

```bash
docker-compose up -d mysql redis
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

#### MySQL 접속

Docker Compose로 실행한 경우:

```bash
# MySQL 컨테이너에 접속
docker exec -it capstone-mysql mysql -u user -p capstone_db

# 또는 root로 접속
docker exec -it capstone-mysql mysql -u root -p
```

로컬 MySQL 클라이언트 사용 시:

```bash
mysql -h localhost -P 3307 -u user -p capstone_db
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

`init.sql` 파일에 초기 데이터베이스 스키마나 데이터를 추가할 수 있습니다. 이 파일은 MySQL 컨테이너가 처음 시작될 때 자동으로 실행됩니다.

```bash
# 특정 서비스 재시작
docker-compose restart app

# 전체 재시작
docker-compose restart
```
