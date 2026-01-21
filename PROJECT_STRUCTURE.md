# 프로젝트 구조

## 📁 패키지 구조

```
src/main/java/com/example/Capstone_project/
├── CapstoneProjectApplication.java     # 메인 애플리케이션 클래스
├── config/                             # 설정 클래스
│   ├── CorsConfig.java                 # CORS 설정 (Flutter 통신용)
│   ├── SecurityConfig.java             # Spring Security 설정
│   ├── WebClientConfig.java            # WebClient 설정 (Gemini API 통신용)
│   └── SwaggerConfig.java              # Swagger/OpenAPI 설정
├── common/                             # 공통 클래스
│   ├── dto/
│   │   └── ApiResponse.java            # 공통 API 응답 DTO
│   └── exception/
│       ├── GlobalExceptionHandler.java # 전역 예외 처리
│       ├── ValidationExceptionHandler.java # Validation 예외 처리
│       ├── ResourceNotFoundException.java # 리소스 없음 예외
│       └── BadRequestException.java    # 잘못된 요청 예외
├── domain/                             # 엔티티 (도메인 모델)
│   ├── BaseEntity.java                 # 기본 엔티티 (id, createdAt, updatedAt)
│   └── ExampleEntity.java              # 예시 엔티티
├── repository/                         # 데이터 접근 계층
│   └── ExampleRepository.java          # 예시 Repository (JPA)
├── service/                            # 비즈니스 로직 계층
│   ├── ExampleService.java             # 예시 Service
│   └── GeminiService.java              # Gemini API Service (가상 피팅)
├── controller/                         # REST API 컨트롤러
│   ├── ExampleController.java          # 예시 Controller
│   └── VirtualFittingController.java   # 가상 피팅 Controller
└── dto/                                # 데이터 전송 객체
    ├── ExampleRequest.java             # 요청 DTO
    ├── ExampleResponse.java            # 응답 DTO
    ├── VirtualFittingResponse.java     # 가상 피팅 응답 DTO
    ├── GeminiGenerateContentRequest.java # Gemini API 요청 DTO
    └── GeminiGenerateContentResponse.java # Gemini API 응답 DTO
```

## 🏗️ 아키텍처 패턴

### Layered Architecture (계층형 아키텍처)

1. **Controller Layer** (`controller/`)
   - REST API 엔드포인트 정의
   - HTTP 요청/응답 처리
   - 요청 검증 및 응답 변환

2. **Service Layer** (`service/`)
   - 비즈니스 로직 구현
   - 트랜잭션 관리
   - Repository 호출

3. **Repository Layer** (`repository/`)
   - 데이터베이스 접근
   - JPA Repository 인터페이스

4. **Domain Layer** (`domain/`)
   - 엔티티 정의
   - 도메인 모델

5. **DTO Layer** (`dto/`)
   - 요청/응답 데이터 전송 객체
   - 엔티티와 분리된 데이터 구조

## 🔧 주요 기능

### 1. CORS 설정
- Flutter 앱과의 통신을 위한 CORS 설정
- `/api/v1/**` 경로에 대해 모든 Origin 허용 (개발 환경)

### 2. Security 설정
- CSRF 비활성화
- Stateless 세션 관리
- `/api/v1/**` 경로는 인증 없이 접근 가능

### 3. Gemini API 통합 (나노바나나 프로)
- Google Gemini API를 활용한 AI 가상 피팅 기능
- 이미지 리사이징 및 최적화 (성능 향상)
- Base64 인코딩/디코딩
- 파일 저장 및 URL 제공
- 비동기 처리 지원 (WebClient)

### 4. Swagger/OpenAPI 문서화
- SpringDoc OpenAPI 3.0 통합
- Swagger UI 제공 (`/swagger-ui.html`)
- API 문서 자동 생성 (`/v3/api-docs`)

### 5. 공통 응답 형식
```json
{
  "success": true,
  "message": "Success",
  "data": { ... }
}
```

### 6. 예외 처리
- 전역 예외 처리 (`@RestControllerAdvice`)
- Validation 예외 처리
- 커스텀 예외 클래스

### 7. JPA Auditing
- `BaseEntity`를 상속받으면 자동으로 `createdAt`, `updatedAt` 관리
- `@EnableJpaAuditing` 활성화

## 📝 API 엔드포인트

### Example API

- `GET /api/v1/examples` - 전체 조회
- `GET /api/v1/examples/{id}` - 단일 조회
- `POST /api/v1/examples` - 생성
- `PUT /api/v1/examples/{id}` - 수정
- `DELETE /api/v1/examples/{id}` - 삭제

### Virtual Fitting API (가상 피팅)

- `POST /api/v1/virtual-fitting` - 가상 피팅 이미지 생성
  - **Parameters:**
    - `user_image` (required): 신체 사진 (MultipartFile)
    - `top_image` (required): 상의 사진 (MultipartFile)
    - `bottom_image` (required): 하의 사진 (MultipartFile)
    - `positive_prompt` (optional): 긍정적 프롬프트
    - `negative_prompt` (optional): 제외 프롬프트
    - `resolution` (optional): 해상도 (1K, 2K, 4K)
  - **Response:**
    ```json
    {
      "success": true,
      "message": "Virtual fitting completed successfully",
      "data": {
        "imageId": "gemini-1234567890",
        "status": "completed",
        "imageUrl": "/api/v1/virtual-fitting/images/uuid.jpg",
        "imageBase64": null,
        "creditsUsed": null
      }
    }
    ```

- `GET /api/v1/virtual-fitting/images/{filename}` - 결과 이미지 조회
  - 생성된 가상 피팅 이미지를 HTTP 응답으로 제공

### Swagger UI

- `GET /swagger-ui.html` - Swagger UI 페이지
- `GET /v3/api-docs` - OpenAPI 3.0 문서 (JSON)

## 🚀 새로운 기능 추가 방법

### 1. 엔티티 생성
```java
@Entity
@Table(name = "your_table")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YourEntity extends BaseEntity {
    // 필드 정의
}
```

### 2. Repository 생성
```java
@Repository
public interface YourRepository extends JpaRepository<YourEntity, Long> {
    // 커스텀 쿼리 메서드 추가 가능
}
```

### 3. DTO 생성
```java
// Request
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class YourRequest {
    @NotBlank
    private String field;
}

// Response
@Getter
@Builder
public class YourResponse {
    private Long id;
    private String field;
    
    public static YourResponse from(YourEntity entity) {
        return YourResponse.builder()
            .id(entity.getId())
            .field(entity.getField())
            .build();
    }
}
```

### 4. Service 생성
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YourService {
    private final YourRepository repository;
    
    // 비즈니스 로직 구현
}
```

### 5. Controller 생성
```java
@RestController
@RequestMapping("/api/v1/your-endpoint")
@RequiredArgsConstructor
public class YourController {
    private final YourService service;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<YourResponse>>> getAll() {
        // 구현
    }
}
```

## 🔒 보안 고려사항

현재는 개발 환경을 위해 Security가 비활성화되어 있습니다. 프로덕션 환경에서는:
- JWT 인증 추가
- 특정 Origin만 CORS 허용
- Role 기반 접근 제어
- API Rate Limiting
- **민감 정보 관리:**
  - `application.properties` 파일은 `.gitignore`에 추가되어 있음
  - API 키 등 민감한 정보는 환경 변수 또는 별도의 설정 파일로 관리
  - `.env` 파일도 `.gitignore`에 포함

## 📚 참고사항

- 모든 API는 `/api/v1`로 시작
- 공통 응답 형식 사용 (`ApiResponse<T>`)
- 예외는 `GlobalExceptionHandler`에서 처리
- Validation은 `@Valid` 어노테이션 사용
- **이미지 저장:**
  - 가상 피팅 결과 이미지는 `./images/virtual-fitting` 디렉토리에 저장
  - UUID를 사용한 고유 파일명 생성
- **외부 API 통신:**
  - Gemini API는 WebClient를 사용한 비동기 통신
  - 타임아웃 및 에러 처리 구현
  - Rate Limit 에러 (429) 처리

## 🛠️ 설정 파일

### application.properties
주요 설정:
- MySQL 데이터베이스 연결
- Redis 연결
- Gemini API 설정 (API 키, 모델, 해상도 등)
- 이미지 저장 경로 및 URL 설정
- JPA, Logging 설정

### .gitignore
보안을 위해 제외되는 파일:
- `*.properties` (application.properties 포함)
- `.env`
- `build/`, `.gradle/`
- IDE 관련 파일 (`.idea/`, `.vscode/`)

## 🚀 실행 방법

### 로컬 환경
```bash
# application.properties 파일 생성 (필수)
cp src/main/resources/application.properties.example src/main/resources/application.properties

# application.properties에 필요한 값 설정
# - MySQL 연결 정보
# - Redis 연결 정보
# - Gemini API 키

# 애플리케이션 실행
./gradlew bootRun
```

### Docker 환경
```bash
# Docker Compose로 실행
docker-compose up -d
```

