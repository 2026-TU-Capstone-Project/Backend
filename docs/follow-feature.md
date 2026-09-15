# 팔로우/팔로잉 기능 구현 명세

> 브랜치: `feature/follow-following`
> 작업일: 2026-05-06

---

## 개요

인스타그램 스타일의 팔로우/팔로잉 관계 시스템을 구현했습니다.

- 계정 고유 아이디(`username`) 도입
- 팔로우 신청 → 수락/거절 플로우
- 유저 검색 (아이디 + 이름)
- 피드 공개 범위 필터링 실제 적용

---

## 1. DB 변경사항

### `users` 테이블 — 컬럼 추가

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `username` | VARCHAR(30) | UNIQUE, NULLABLE | 인스타그램 아이디처럼 고유한 계정 ID |

> `nickname` 은 기존대로 **이름(표시명)** 역할을 유지합니다.

---

### `follows` 테이블 — 신규 생성

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK | |
| `follower_id` | BIGINT | FK → users, NOT NULL | 팔로우 요청자 |
| `following_id` | BIGINT | FK → users, NOT NULL | 팔로우 대상 |
| `status` | VARCHAR(10) | NOT NULL | `PENDING` / `ACCEPTED` |
| `created_at` | TIMESTAMP | NOT NULL | 신청 시각 |
| `updated_at` | TIMESTAMP | | 수락 시각 |

**유니크 제약:** `(follower_id, following_id)` — 중복 신청 불가

**인덱스:**
- `idx_follows_following_status` : `(following_id, status)` — 받은 신청/팔로워 목록 조회
- `idx_follows_follower_status` : `(follower_id, status)` — 팔로잉 목록 조회

---

## 2. API 명세

### 베이스 URL: `/api/v1`

---

### 팔로우 (`/follows`)

#### `POST /follows/{targetUserId}` — 팔로우 신청
```
Authorization: Bearer {token}
```
- 대상 유저에게 팔로우 신청 (status = PENDING)
- 자기 자신 팔로우 불가 → 400
- 이미 신청 중이거나 팔로우 중이면 → 400

**Response**
```json
{ "success": true, "message": "팔로우 신청이 완료되었습니다.", "data": null }
```

---

#### `DELETE /follows/{targetUserId}` — 팔로우 취소 / 언팔로우
```
Authorization: Bearer {token}
```
- PENDING 상태 → 신청 취소
- ACCEPTED 상태 → 언팔로우
- 관계 없음 → 404

---

#### `PATCH /follows/requests/{followId}/accept` — 팔로우 수락
```
Authorization: Bearer {token}
```
- 본인에게 온 PENDING 신청만 수락 가능
- 수락 후 status = ACCEPTED

---

#### `DELETE /follows/requests/{followId}/reject` — 팔로우 거절
```
Authorization: Bearer {token}
```
- 거절 시 Follow 레코드 삭제 → 상대방이 재신청 가능

---

#### `GET /follows/requests` — 받은 팔로우 신청 목록
```
Authorization: Bearer {token}
```

**Response**
```json
{
  "success": true,
  "data": [
    {
      "followId": 1,
      "requesterId": 5,
      "username": "kim_style",
      "nickname": "김스타일",
      "profileImageUrl": "https://...",
      "requestedAt": "2026-05-06T10:00:00"
    }
  ]
}
```

---

#### `GET /follows/followers` — 내 팔로워 목록
#### `GET /follows/followings` — 내 팔로잉 목록
#### `GET /follows/{userId}/followers` — 특정 유저 팔로워 목록
#### `GET /follows/{userId}/followings` — 특정 유저 팔로잉 목록

**Response 항목**
```json
{
  "followId": 3,
  "userId": 7,
  "username": "park_fit",
  "nickname": "박핏",
  "profileImageUrl": "https://..."
}
```

---

### 유저 (`/users`)

#### `GET /users/search?keyword={검색어}` — 유저 검색
```
Authorization: Bearer {token}  (선택)
```
- `username` 또는 `nickname` 모두 LIKE 검색 (대소문자 무시)
- 최대 20명 반환
- `followStatus`: 나와의 관계 (`null` / `PENDING` / `ACCEPTED`)

**Response**
```json
{
  "success": true,
  "data": [
    {
      "userId": 3,
      "username": "kim_style",
      "nickname": "김스타일",
      "profileImageUrl": "https://...",
      "followStatus": "ACCEPTED"
    }
  ]
}
```

---

#### `GET /users/{userId}` — 다른 유저 공개 프로필 조회

**Response**
```json
{
  "success": true,
  "data": {
    "userId": 3,
    "username": "kim_style",
    "nickname": "김스타일",
    "profileImageUrl": "https://...",
    "followerCount": 120,
    "followingCount": 85,
    "followStatus": "PENDING",
    "isMe": false
  }
}
```

---

#### `POST /users/me` & `PATCH /users/me` — 프로필 설정/수정 (변경사항)

기존 파라미터에 아래 항목 추가:

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `username` | 선택 | 영문·숫자·언더스코어, 3~30자, 중복 불가 |
| `nickname` | 선택 | 기존과 동일 (표시 이름) |

**username 유효성 오류 시 → 400**
```json
{ "success": false, "message": "username은 영문, 숫자, 언더스코어(_)만 사용 가능하며 3~30자여야 합니다." }
```

---

### 피드 공개 범위 실제 적용 (`/feeds`)

#### 기존 버그 수정
- 이전에는 `FOLLOWERS_ONLY` 설정이 DB에만 저장되고 실제 필터링이 없었음
- 이번 작업으로 **실제 팔로우 관계**를 기준으로 필터링됩니다

#### `GET /feeds` 조회 기준

| 조건 | 보이는 피드 |
|------|------------|
| 비로그인 | PUBLIC 피드만 |
| 로그인 | PUBLIC + 내 피드 + 팔로우 중인 사람의 FOLLOWERS_ONLY 피드 |

#### `GET /feeds/{feedId}` 상세 조회
- `FOLLOWERS_ONLY` 피드를 비팔로워가 조회 시 → **403 Forbidden**
```json
{ "success": false, "message": "팔로워만 볼 수 있는 피드입니다." }
```

#### `POST /feeds` 피드 작성 시 공개 범위
- 기존 `visibility` 필드를 body에 포함해서 전송
- `PUBLIC` (기본값) 또는 `FOLLOWERS_ONLY`

---

## 3. 팔로우 상태 흐름

```
[없음]
  │
  │  POST /follows/{targetUserId}
  ▼
[PENDING]  ──── DELETE /follows/requests/{id}/reject ────▶ [삭제됨 → 재신청 가능]
  │
  │  PATCH /follows/requests/{id}/accept
  ▼
[ACCEPTED]
  │
  │  DELETE /follows/{targetUserId}  (팔로워 또는 팔로잉 본인이 취소)
  ▼
[삭제됨]
```

---

## 4. 수정된 파일 목록

### 신규 생성
| 파일 | 설명 |
|------|------|
| `domain/FollowStatus.java` | PENDING / ACCEPTED enum |
| `domain/Follow.java` | 팔로우 관계 엔티티 |
| `repository/FollowRepository.java` | 팔로우 관련 쿼리 |
| `service/FollowService.java` | 팔로우 비즈니스 로직 |
| `controller/FollowController.java` | 팔로우 API |
| `dto/FollowUserDto.java` | 팔로워/팔로잉 목록 항목 DTO |
| `dto/FollowRequestItemDto.java` | 받은 신청 목록 항목 DTO |
| `dto/UserSearchResponseDto.java` | 유저 검색 결과 DTO |
| `dto/UserPublicProfileResponseDto.java` | 공개 프로필 DTO |

### 수정
| 파일 | 변경 내용 |
|------|----------|
| `domain/User.java` | `username` 필드 추가 |
| `repository/UserRepository.java` | username 조회·중복확인·검색 메서드 추가 |
| `dto/UserProfileResponseDto.java` | `username` 필드 추가 |
| `service/UserService.java` | username 처리 + 유저 검색 추가 |
| `controller/UserController.java` | username 파라미터 + 검색·공개프로필 엔드포인트 추가 |
| `repository/FeedRepository.java` | visibility 인식 쿼리 2개 추가 |
| `service/FeedService.java` | visibility 필터링 + 상세 접근 권한 검사 적용 |
