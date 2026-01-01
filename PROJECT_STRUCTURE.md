# 프로젝트 구조

## 📁 패키지 구조

```
src/main/java/com/example/Capstone_project/
├── CapstoneProjectApplication.java     # 메인 애플리케이션 클래스
├── config/                             # 설정 클래스
│   ├── CorsConfig.java                 # CORS 설정 (Flutter 통신용)
│   └── SecurityConfig.java             # Spring Security 설정
├── common/                             # 공통 클래스
│   ├── dto/
│   │   └── ApiResponse.java            # 공통 API 응답 DTO
│   └── exception/
│       ├── GlobalExceptionHandler.java # 전역 예외 처리
│       ├── ValidationExceptionHandler.java # Validation 예외 처리
│       ├── ResourceNotFoundException.java # 리소스 없음 예외
│       └── BadRequestException.java   # 잘못된 요청 예외
├── domain/                             # 엔티티 (도메인 모델)
│   ├── BaseEntity.java                 # 기본 엔티티 (id, createdAt, updatedAt)
│   └── ExampleEntity.java              # 예시 엔티티
├── repository/                         # 데이터 접근 계층
│   └── ExampleRepository.java          # 예시 Repository (JPA)
├── service/                           # 비즈니스 로직 계층
│   └── ExampleService.java            # 예시 Service
├── controller/                        # REST API 컨트롤러
│   └── ExampleController.java         # 예시 Controller
└── dto/                               # 데이터 전송 객체
    ├── ExampleRequest.java            # 요청 DTO
    └── ExampleResponse.java          # 응답 DTO
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

### 3. 공통 응답 형식
```json
{
  "success": true,
  "message": "Success",
  "data": { ... }
}
```

### 4. 예외 처리
- 전역 예외 처리 (`@RestControllerAdvice`)
- Validation 예외 처리
- 커스텀 예외 클래스

### 5. JPA Auditing
- `BaseEntity`를 상속받으면 자동으로 `createdAt`, `updatedAt` 관리
- `@EnableJpaAuditing` 활성화

## 📝 API 엔드포인트 예시

### Example API

- `GET /api/v1/examples` - 전체 조회
- `GET /api/v1/examples/{id}` - 단일 조회
- `POST /api/v1/examples` - 생성
- `PUT /api/v1/examples/{id}` - 수정
- `DELETE /api/v1/examples/{id}` - 삭제

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

## 📚 참고사항

- 모든 API는 `/api/v1`로 시작
- 공통 응답 형식 사용 (`ApiResponse<T>`)
- 예외는 `GlobalExceptionHandler`에서 처리
- Validation은 `@Valid` 어노테이션 사용

