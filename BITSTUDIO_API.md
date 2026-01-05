# BitStudio API 통합 가이드

이 문서는 BitStudio AI API를 사용한 Virtual Try-On 기능에 대한 상세한 설명을 제공합니다.

## 📋 목차

- [개요](#개요)
- [설정](#설정)
- [API 문서](#api-문서)
- [사용 예시](#사용-예시)
- [환경변수](#환경변수)
- [주의사항](#주의사항)
- [트러블슈팅](#트러블슈팅)

## 개요

BitStudio AI API를 활용하여 신체 사진과 옷 사진을 입력받아 가상 피팅(Virtual Try-On) 결과 이미지를 생성하는 기능을 제공합니다.

### 주요 특징

- **비동기 처리**: 이미지 생성 작업을 비동기로 처리하며 Polling을 통해 완료 상태를 확인합니다.
- **다양한 해상도**: 표준 해상도와 고해상도를 지원합니다.
- **다중 이미지 생성**: 한 번의 요청으로 최대 4개의 이미지를 생성할 수 있습니다.
- **프롬프트 지원**: 선택적으로 프롬프트를 제공하여 결과 이미지의 품질을 향상시킬 수 있습니다.

## 설정

### 1. BitStudio API 키 발급

1. [BitStudio 웹사이트](https://bitstudio.ai)에 가입합니다.
2. 계정 설정에서 API 키를 발급받습니다.
3. 발급받은 API 키를 `.env` 파일에 추가합니다.

### 2. 환경변수 설정

`.env` 파일에 다음 환경변수를 추가합니다:

```env
BIT_STUDIO_KEY=your_api_key_here
```

선택적으로 다음 설정도 변경할 수 있습니다:

```env
# BitStudio API 기본 URL (기본값: https://api.bitstudio.ai)
BITSTUDIO_API_BASE_URL=https://api.bitstudio.ai

# Polling 최대 시도 횟수 (기본값: 150, 약 5분)
BITSTUDIO_POLLING_MAX_ATTEMPTS=150

# Polling 간격 (초, 기본값: 2)
BITSTUDIO_POLLING_INTERVAL=2
```

## API 문서

### Virtual Try-On API

#### POST /api/v1/virtual-try-on

신체 사진과 옷 사진을 입력받아 Virtual Try-On 결과 이미지를 생성합니다.

**Base URL**: `http://localhost:8080`

**Endpoint**: `/api/v1/virtual-try-on`

**Method**: `POST`

**Content-Type**: `multipart/form-data`

**인증**: 불필요 (현재 설정 기준)

### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| `personImage` | MultipartFile | ✅ | 신체 사진 파일 | - |
| `outfitImage` | MultipartFile | ✅ | 옷 사진 파일 | - |
| `prompt` | String | ❌ | 결과 이미지 가이드용 텍스트 | - |
| `resolution` | String | ❌ | 해상도 (`standard` 또는 `high`) | `standard` |
| `numImages` | Integer | ❌ | 생성할 이미지 수 (1-4) | `1` |

#### 파라미터 상세 설명

- **personImage**: 신체 전체가 보이는 사진 (JPEG, PNG, WebP, 최대 10MB)
  - 권장: 정면, 중립적인 포즈, 깨끗한 배경
- **outfitImage**: 옷 사진 (JPEG, PNG, WebP, 최대 10MB)
  - 권장: 단색 배경, 명확한 옷 이미지
- **prompt**: 선택적 프롬프트로 결과 이미지의 품질 향상에 도움이 됩니다.
  - 예: `"professional portrait, high quality, studio lighting"`
- **resolution**: 이미지 해상도
  - `standard`: 표준 해상도 (1 크레딧/이미지)
  - `high`: 고해상도 (2 크레딧/이미지)
- **numImages**: 한 번에 생성할 이미지 수 (1-4)
  - 여러 이미지를 생성하면 선택의 폭이 넓어집니다.

### 응답 형식

#### 성공 응답 (200 OK)

```json
{
  "success": true,
  "message": "Virtual try-on completed successfully",
  "data": {
    "imageId": "GEN_789",
    "status": "completed",
    "imageUrl": "https://media.bitstudio.ai/gen/image.jpg",
    "creditsUsed": 2,
    "sourceImageIds": ["IMG_123", "IMG_456"]
  }
}
```

#### 응답 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | Boolean | 요청 성공 여부 |
| `message` | String | 응답 메시지 |
| `data.imageId` | String | 생성된 이미지 ID |
| `data.status` | String | 이미지 상태 (`completed`) |
| `data.imageUrl` | String | 생성된 이미지 URL |
| `data.creditsUsed` | Integer | 사용된 크레딧 수 |
| `data.sourceImageIds` | Array<String> | 원본 이미지 ID 목록 |

#### 에러 응답 (400 Bad Request)

```json
{
  "success": false,
  "message": "Error message",
  "data": null
}
```

**주요 에러 상황**:
- 파일이 비어있거나 형식이 잘못된 경우
- 해상도가 `standard` 또는 `high`가 아닌 경우
- `numImages`가 1-4 범위를 벗어난 경우
- BitStudio API 에러 (크레딧 부족, API 키 오류 등)

## 사용 예시

### cURL 예시

#### 기본 요청

```bash
curl -X POST http://localhost:8080/api/v1/virtual-try-on \
  -F "personImage=@person.jpg" \
  -F "outfitImage=@outfit.jpg"
```

#### 고해상도, 프롬프트 포함

```bash
curl -X POST http://localhost:8080/api/v1/virtual-try-on \
  -F "personImage=@person.jpg" \
  -F "outfitImage=@outfit.jpg" \
  -F "prompt=professional portrait, high quality, studio lighting" \
  -F "resolution=high" \
  -F "numImages=2"
```

### JavaScript (Fetch API) 예시

#### 기본 사용법

```javascript
const formData = new FormData();
formData.append('personImage', personFile); // File 객체
formData.append('outfitImage', outfitFile); // File 객체

const response = await fetch('http://localhost:8080/api/v1/virtual-try-on', {
  method: 'POST',
  body: formData
});

if (!response.ok) {
  throw new Error(`HTTP error! status: ${response.status}`);
}

const result = await response.json();
console.log('Generated image URL:', result.data.imageUrl);
```

#### 모든 옵션 포함

```javascript
async function createVirtualTryOn(personFile, outfitFile, options = {}) {
  const formData = new FormData();
  formData.append('personImage', personFile);
  formData.append('outfitImage', outfitFile);
  
  if (options.prompt) {
    formData.append('prompt', options.prompt);
  }
  
  if (options.resolution) {
    formData.append('resolution', options.resolution); // 'standard' or 'high'
  }
  
  if (options.numImages) {
    formData.append('numImages', options.numImages.toString());
  }
  
  const response = await fetch('http://localhost:8080/api/v1/virtual-try-on', {
    method: 'POST',
    body: formData
  });
  
  const result = await response.json();
  
  if (!result.success) {
    throw new Error(result.message);
  }
  
  return result.data;
}

// 사용 예시
const result = await createVirtualTryOn(
  personFile,
  outfitFile,
  {
    prompt: 'professional portrait, high quality',
    resolution: 'high',
    numImages: 2
  }
);

console.log('Image URL:', result.imageUrl);
console.log('Credits used:', result.creditsUsed);
```

### React 예시

```jsx
import React, { useState } from 'react';

function VirtualTryOnForm() {
  const [personFile, setPersonFile] = useState(null);
  const [outfitFile, setOutfitFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!personFile || !outfitFile) {
      setError('Please select both person and outfit images');
      return;
    }
    
    setLoading(true);
    setError(null);
    
    try {
      const formData = new FormData();
      formData.append('personImage', personFile);
      formData.append('outfitImage', outfitFile);
      formData.append('resolution', 'high');
      
      const response = await fetch('http://localhost:8080/api/v1/virtual-try-on', {
        method: 'POST',
        body: formData
      });
      
      const data = await response.json();
      
      if (data.success) {
        setResult(data.data);
      } else {
        setError(data.message);
      }
    } catch (err) {
      setError('Failed to create virtual try-on: ' + err.message);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <form onSubmit={handleSubmit}>
      <div>
        <label>Person Image:</label>
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={(e) => setPersonFile(e.target.files[0])}
        />
      </div>
      
      <div>
        <label>Outfit Image:</label>
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={(e) => setOutfitFile(e.target.files[0])}
        />
      </div>
      
      <button type="submit" disabled={loading}>
        {loading ? 'Processing...' : 'Create Virtual Try-On'}
      </button>
      
      {error && <div style={{ color: 'red' }}>{error}</div>}
      
      {result && (
        <div>
          <h3>Result:</h3>
          <img src={result.imageUrl} alt="Virtual Try-On Result" />
          <p>Credits used: {result.creditsUsed}</p>
        </div>
      )}
    </form>
  );
}

export default VirtualTryOnForm;
```

### Python (requests) 예시

```python
import requests

def create_virtual_try_on(person_image_path, outfit_image_path, 
                          prompt=None, resolution='standard', num_images=1):
    url = 'http://localhost:8080/api/v1/virtual-try-on'
    
    with open(person_image_path, 'rb') as person_file, \
         open(outfit_image_path, 'rb') as outfit_file:
        
        files = {
            'personImage': person_file,
            'outfitImage': outfit_file
        }
        
        data = {
            'resolution': resolution,
            'numImages': num_images
        }
        
        if prompt:
            data['prompt'] = prompt
        
        response = requests.post(url, files=files, data=data)
        
        if response.status_code == 200:
            result = response.json()
            if result['success']:
                return result['data']
            else:
                raise Exception(result['message'])
        else:
            response.raise_for_status()

# 사용 예시
try:
    result = create_virtual_try_on(
        'person.jpg',
        'outfit.jpg',
        prompt='professional portrait, high quality',
        resolution='high',
        num_images=2
    )
    
    print(f"Image URL: {result['imageUrl']}")
    print(f"Credits used: {result['creditsUsed']}")
except Exception as e:
    print(f"Error: {e}")
```

## 환경변수

### 필수 환경변수

| 변수명 | 설명 | 예시 |
|--------|------|------|
| `BIT_STUDIO_KEY` | BitStudio API 키 | `bs_ZeFNv6yw9AoSmnrR95lAXrIpH5Y1ijl` |

### 선택적 환경변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `BITSTUDIO_API_BASE_URL` | BitStudio API 기본 URL | `https://api.bitstudio.ai` |
| `BITSTUDIO_POLLING_MAX_ATTEMPTS` | Polling 최대 시도 횟수 | `150` (약 5분) |
| `BITSTUDIO_POLLING_INTERVAL` | Polling 간격 (초) | `2` |

### 설정 파일 위치

환경변수는 프로젝트 루트 디렉토리의 `.env` 파일에 설정합니다:

```env
# BitStudio API 설정
BIT_STUDIO_KEY=your_api_key_here
BITSTUDIO_API_BASE_URL=https://api.bitstudio.ai
BITSTUDIO_POLLING_MAX_ATTEMPTS=150
BITSTUDIO_POLLING_INTERVAL=2
```

## 주의사항

### 처리 시간

- **일반 처리 시간**: 15-30초
- **최대 대기 시간**: 약 5분 (설정 가능)
- 작업이 완료될 때까지 Polling을 통해 상태를 확인합니다.

### 크레딧 소비

| 해상도 | 크레딧 (이미지당) |
|--------|------------------|
| Standard | 1 크레딧 |
| High | 2 크레딧 |

예시:
- Standard 해상도, 1개 이미지: 1 크레딧
- High 해상도, 2개 이미지: 4 크레딧 (2 × 2)

### 파일 제한

- **최대 파일 크기**: 10MB
- **지원 형식**: JPEG, PNG, WebP
- **권장 해상도**: 
  - Person 이미지: 최소 512×512 픽셀
  - Outfit 이미지: 최소 512×512 픽셀

### 이미지 품질 향상을 위한 팁

1. **Person 이미지**:
   - 정면 촬영 (전신 또는 반신)
   - 중립적인 포즈
   - 깨끗한 배경
   - 좋은 조명
   - 선명한 이미지

2. **Outfit 이미지**:
   - 단색 배경 (흰색 권장)
   - 옷이 명확하게 보이도록
   - 접힘 없이 펼쳐진 상태
   - 좋은 해상도

3. **프롬프트**:
   - 구체적이고 명확한 설명
   - 예: `"professional portrait, high quality, studio lighting, full body"`

## 트러블슈팅

### 일반적인 문제

#### 1. API 키 오류

**증상**: `401 Unauthorized` 또는 `Failed to upload image: Unauthorized`

**해결방법**:
- `.env` 파일에 `BIT_STUDIO_KEY`가 올바르게 설정되었는지 확인
- API 키에 공백이나 따옴표가 없는지 확인
- API 키가 유효한지 BitStudio 웹사이트에서 확인

#### 2. 크레딧 부족

**증상**: `402 Payment Required` 또는 `insufficient_credits`

**해결방법**:
- BitStudio 계정에 충분한 크레딧이 있는지 확인
- 크레딧을 충전하세요

#### 3. 파일 크기 초과

**증상**: `413 Payload Too Large` 또는 파일 업로드 실패

**해결방법**:
- 파일 크기가 10MB 이하인지 확인
- 필요시 이미지를 압축하거나 리사이즈

#### 4. 타임아웃

**증상**: `Image generation timed out`

**해결방법**:
- `.env` 파일에서 `BITSTUDIO_POLLING_MAX_ATTEMPTS` 값을 늘리세요 (기본값: 150)
- 또는 `BITSTUDIO_POLLING_INTERVAL` 값을 조정하세요

#### 5. 연결 오류

**증상**: `Connection refused` 또는 네트워크 오류

**해결방법**:
- 애플리케이션이 실행 중인지 확인
- 네트워크 연결 확인
- 방화벽 설정 확인

### 로그 확인

애플리케이션 로그를 확인하여 문제를 진단할 수 있습니다:

```bash
# Docker Compose 사용 시
docker-compose logs -f app

# 로컬 실행 시
# 콘솔에 출력되는 로그 확인
```

### 디버깅 팁

1. **로깅 레벨 조정**: `.env` 파일에서 로그 레벨을 `DEBUG`로 설정
2. **cURL로 직접 테스트**: API가 정상 작동하는지 확인
3. **BitStudio API 문서 확인**: [공식 문서](https://bitstudio.ai/docs) 참조

## 참고 자료

- [BitStudio 공식 웹사이트](https://bitstudio.ai)
- [BitStudio API 문서](https://bitstudio.ai/docs)
- [프로젝트 README](../README.md)

## 문의

문제가 지속되거나 추가 지원이 필요한 경우:
1. 프로젝트 이슈 트래커에 문의
2. BitStudio 지원팀에 문의
3. 개발팀에 연락


