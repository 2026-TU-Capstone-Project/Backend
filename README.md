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
- **ORM**: Spring Data JPA
- **Build Tool**: Gradle
- **Container**: Docker & Docker Compose

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

3. **MySQL 컨테이너 시작**
   ```bash
   docker-compose up -d mysql
   ```
   - MySQL이 완전히 시작될 때까지 약 10-20초 대기하세요.

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

#### 1. MySQL 컨테이너만 실행

```bash
docker-compose up -d mysql
```

#### 2. 애플리케이션 실행

```bash
# Gradle을 사용한 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/*.jar
```

## 🔧 환경변수 설명

### JPA 설정

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | DDL 자동 생성 모드 (`none`, `validate`, `update`, `create`, `create-drop`) | `update` |
| `SPRING_JPA_SHOW_SQL` | SQL 쿼리 로그 출력 여부 | `true` |

### 로깅 설정

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `LOG_LEVEL_ROOT` | 루트 로그 레벨 | `INFO` |
| `LOG_LEVEL_SPRING_WEB` | Spring Web 로그 레벨 | `INFO` |
| `LOG_LEVEL_HIBERNATE_SQL` | Hibernate SQL 로그 레벨 | `DEBUG` |

## 💻 개발 가이드

### 데이터베이스 접속

Docker Compose로 실행한 경우:

```bash
# MySQL 컨테이너에 접속
docker exec -it capstone-mysql mysql -u capstone_user -p capstone_db

# 또는 root로 접속
docker exec -it capstone-mysql mysql -u root -p
```

로컬 MySQL 클라이언트 사용 시:

```bash
mysql -h localhost -P 3306 -u capstone_user -p capstone_db
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

## 🔒 보안 주의사항

1. **프로덕션 환경**에서는 반드시 `.env` 파일의 비밀번호를 강력한 값으로 변경하세요.
2. `.env` 파일은 절대 Git에 커밋하지 마세요.
3. Docker secrets나 환경변수 관리 도구를 사용하는 것을 권장합니다.

## 🐛 문제 해결

### 포트 충돌

포트가 이미 사용 중인 경우 `.env` 파일에서 포트를 변경하세요:

```env
SERVER_PORT=8081
MYSQL_PORT=3307
```

### 데이터베이스 연결 실패

1. MySQL 컨테이너가 정상적으로 실행 중인지 확인:
   ```bash
   docker-compose ps
   ```

2. MySQL 로그 확인:
   ```bash
   docker-compose logs mysql
   ```

3. 환경변수가 올바르게 설정되었는지 확인:
   ```bash
   docker-compose config
   ```

### 컨테이너 재시작

```bash
# 특정 서비스 재시작
docker-compose restart app

# 전체 재시작
docker-compose restart
```
