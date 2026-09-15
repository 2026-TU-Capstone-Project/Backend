# 로컬 시연 셋팅 가이드 (클라우드 없이 백엔드 실행)

배포 환경은 **GCS(Google Cloud Storage)** 에 사진을 저장하지만, 시연은 노트북 한 대에서 돌려야 하므로
`storage.type=local` 프로파일로 **사진을 로컬 디스크에 저장**하고 백엔드가 직접 서빙하도록 했다.

```
storage.type = gcs   (기본값)  → GoogleCloudStorageService  (배포, 기존과 동일)
storage.type = local           → LocalImageStorageService   (시연/개발, google-key.json 불필요)
```

## 1. 준비물

| 항목 | 비고 |
|---|---|
| Java 21 | `java -version` |
| Docker Desktop | PostgreSQL(pgvector) + Redis 컨테이너용. 없으면 [§5 Docker 없이](#5-docker-없이-돌리기) |
| `src/main/resources/application.properties` | git 미추적 개인 설정 (DB 비번, `gemini.api.key`, `serper.api-key`). 팀원에게 받기 |
| `.env` | git 미추적. `docker-compose.dev.yml` 이 `POSTGRES_*`, `SPRING_DATA_REDIS_PASSWORD` 를 읽음 |
| 인터넷 | Gemini API(가상 피팅·옷 분석·임베딩), Serper(챗봇 웹검색)는 외부 API 호출 |

> `google-key.json` 은 **필요 없다.** local 모드에서는 `GoogleCloudConfig` 자체가 생성되지 않는다.

## 2. 실행

```bash
cd TUCapstone_Backend
./run-local.sh
```

스크립트가 하는 일:
1. `docker compose -f docker-compose.dev.yml up -d` → PostgreSQL(호스트 15432) + Redis(6379)
2. `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun` → 8080 포트

확인: <http://localhost:8080/swagger-ui/index.html>, <http://localhost:8080/actuator/health>

### 프론트에서 접속할 때 주소 (중요)

앱은 두 종류의 URL을 쓴다 — **API 호출 주소**와, 응답에 들어오는 **이미지 URL**.
이미지 URL 은 백엔드가 `storage.local.public-base-url` 값으로 만들어 주므로 **앱의 API base URL 과 같은 값**으로 맞춰야 사진이 보인다.

| 프론트 실행 환경 | 앱 API base URL | 백엔드 실행 명령 |
|---|---|---|
| 같은 PC 브라우저 / iOS 시뮬레이터 | `http://localhost:8080` | `./run-local.sh` |
| Android 에뮬레이터 | `http://10.0.2.2:8080` | `STORAGE_PUBLIC_BASE_URL=http://10.0.2.2:8080 ./run-local.sh` |
| 실기기 (같은 Wi-Fi) | `http://<맥 IP>:8080` | `STORAGE_PUBLIC_BASE_URL=http://192.168.x.x:8080 ./run-local.sh` |

맥 IP 확인: `ipconfig getifaddr en0`

## 3. 어떻게 바뀌었나 (코드)

| 파일 | 변경 |
|---|---|
| `storage/ImageStorageService.java` | **신규** 저장소 인터페이스 (upload*/download/delete/isManagedUrl/extractObjectNameFromUrl) |
| `storage/LocalImageStorageService.java` | **신규** 로컬 디스크 구현 — `local-storage/<폴더>/<파일>` 저장, URL `http://<base>/files/<폴더>/<파일>` |
| `storage/LocalStorageWebConfig.java` | **신규** `/files/**` → `local-storage/` 정적 서빙 (local 모드에서만) |
| `service/GoogleCloudStorageService.java` | 인터페이스 구현, `@ConditionalOnProperty(storage.type=gcs, matchIfMissing)` |
| `config/GoogleCloudConfig.java`, `service/GoogleVisionService.java` | 같은 조건 → local 모드에서 `google-key.json` 미로딩 |
| `config/SecurityConfig.java` | `/files/**` permitAll (GCS 공개 URL 과 동일하게 인증 없이 이미지 접근) |
| `ClothesController`, `UserService`, `GeminiService`, `ClothesAnalysisService`, `FittingService`, `FittingCleanupService` | `GoogleCloudStorageService` → `ImageStorageService` 로 의존 교체, `storage.googleapis.com` 문자열 검사 → `isManagedUrl()` |
| `src/main/resources/application-local.yml` | **신규** local 프로파일 (포트 8080, 로컬 DB/Redis 주소, storage.type=local) |
| `run-local.sh`, `.gitignore`(`local-storage/`) | 실행 스크립트, 저장 폴더 제외 |

폴더 이름(`top-img`, `bottom-img`, `user-body-img`, `profile-images`, `virtual-fitting-img`)은 GCS 와 동일하게 유지해서
objectName(`폴더/파일명`) 규칙이 두 구현에서 같다.

## 4. 시연 전 체크리스트

- [ ] Docker Desktop 실행 → `./run-local.sh` 로그에 `Local image storage 초기화 완료` 와 `Started CapstoneProjectApplication` 확인
- [ ] `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
- [ ] 프론트 API base URL 을 위 표에 맞게 변경 (프론트 담당)
- [ ] 옷 1장 업로드 → `local-storage/top-img/` 에 파일 생성 + 응답 `imgUrl` 이 브라우저에서 열리는지
- [ ] 가상 피팅 1회 → `local-storage/virtual-fitting-img/` 결과 생성 확인
- [ ] 로컬 DB 는 비어 있으므로 시연용 계정/옷 데이터를 미리 넣어 둘 것 (DB 볼륨 `postgres_data` 는 컨테이너를 내려도 유지됨)

## 5. Docker 없이 돌리기

Homebrew 로 직접 설치하는 경우:

```bash
brew install postgresql@16 pgvector redis
brew services start postgresql@16
brew services start redis
createdb capstone_db && psql capstone_db -c "CREATE EXTENSION IF NOT EXISTS vector;"
# application.properties 의 spring.datasource.url / username / password, spring.data.redis.password 를 로컬 값에 맞게 수정
./run-local.sh --no-docker
```

Redis 비밀번호 없이 띄웠다면 `spring.data.redis.password=` 로 비워둔다.

## 6. 자주 나는 문제

| 증상 | 원인 / 조치 |
|---|---|
| 시작 시 `google-key.json` 관련 오류 | `SPRING_PROFILES_ACTIVE=local` 이 안 잡힌 것. `./run-local.sh` 로 실행하거나 IntelliJ Run Config 에 Active profiles=`local` |
| `Connection refused: 15432` | Docker 컨테이너 미기동. `docker compose -f docker-compose.dev.yml ps` |
| 로그인은 되는데 `Redis is not configured` | Redis 미기동 또는 비밀번호 불일치 (`.env` ↔ `application.properties`) |
| 사진 URL 이 앱에서 안 열림 | `STORAGE_PUBLIC_BASE_URL` 이 앱 API base URL 과 다름 (§2 표) |
| `type "vector" does not exist` | pgvector 확장 없음. Docker 는 `init.sql` 이 자동 생성, 직접 설치 시 `CREATE EXTENSION vector` |
| 포트 8080 사용 중 | `SERVER_PORT=8081 ./run-local.sh` (이미지 base URL 도 함께 바꿀 것) |
