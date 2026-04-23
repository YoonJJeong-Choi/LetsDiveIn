# IDE 실행 문제 해결 가이드

## 문제: IDE에서 SwimMallApplication 실행 시 무한 렌더링

### 해결 방법 1: IntelliJ IDEA에서 프로젝트 재import

1. **프로젝트 닫기**
   - File → Close Project

2. **프로젝트 다시 열기**
   - Open → `backend` 폴더 선택
   - **중요**: `build.gradle` 파일이 있는 `backend` 폴더를 선택해야 합니다.

3. **Gradle 프로젝트로 인식 확인**
   - 우측 상단에 "Import Gradle Project" 알림이 뜨면 클릭
   - 또는 File → Settings → Build, Execution, Deployment → Build Tools → Gradle
   - "Build and run using"을 "Gradle"로 설정
   - "Run tests using"을 "Gradle"로 설정

4. **Gradle 동기화**
   - 우측 Gradle 탭에서 새로고침 버튼 클릭
   - 또는 File → Sync Project with Gradle Files

### 해결 방법 2: IDE 캐시 정리

1. **IntelliJ IDEA 캐시 무효화**
   - File → Invalidate Caches...
   - "Invalidate and Restart" 선택

2. **수동 캐시 삭제** (위 방법이 안 될 경우)

   > ⚠️ **안전성 확인**: 아래 폴더들은 모두 **캐시/임시 파일**이므로 삭제해도 **프로젝트 소스 코드에는 전혀 영향이 없습니다**. IDE나 Gradle이 필요시 자동으로 재생성합니다.

   **단계별 가이드:**

   **1단계: IntelliJ IDEA 완전 종료**
   - 모든 IntelliJ IDEA 창 닫기
   - 작업 관리자에서 `idea64.exe` 또는 `idea.exe` 프로세스가 남아있는지 확인
   - 있으면 종료 (Ctrl+Shift+Esc → 프로세스 탭 → 종료)

   **2단계: 삭제할 폴더 확인 및 삭제**

   각 폴더의 역할과 삭제 방법:

   **① Gradle 캐시 폴더** (전역 캐시)
   - 경로: `C:\Users\USER\.gradle\caches`
   - 역할: Gradle이 다운로드한 라이브러리와 빌드 캐시 저장
   - 삭제 영향: 없음 (다음 빌드 시 자동으로 재다운로드됨)
   - 삭제 방법:
     ```
     방법 A: 파일 탐색기에서
     1. 파일 탐색기 열기 (Win+E)
     2. 주소창에 `C:\Users\USER\.gradle\caches` 입력
     3. 폴더 선택 후 Shift+Delete (완전 삭제) 또는 Delete
     
     방법 B: PowerShell에서
     Remove-Item -Path "C:\Users\USER\.gradle\caches" -Recurse -Force
     ```

   **② IntelliJ 프로젝트 설정 폴더** (프로젝트별 설정)
   - 경로: `C:\Users\USER\Desktop\swim-mall\backend\.idea`
   - 역할: IntelliJ IDEA의 프로젝트별 설정 (코드 스타일, 실행 설정, 인덱스 등)
   - 삭제 영향: 없음 (프로젝트 다시 열면 자동으로 재생성됨)
   - 삭제 방법:
     ```
     방법 A: 파일 탐색기에서
     1. 파일 탐색기에서 `C:\Users\USER\Desktop\swim-mall\backend` 폴더 열기
     2. `.idea` 폴더가 보이지 않으면:
        - 보기 탭 → "숨긴 항목" 체크
        - 또는 주소창에 `.idea` 입력
     3. `.idea` 폴더 선택 후 Shift+Delete
     
     방법 B: PowerShell에서
     Remove-Item -Path "C:\Users\USER\Desktop\swim-mall\backend\.idea" -Recurse -Force
     ```

   **③ 빌드 결과물 폴더** (컴파일된 클래스 파일)
   - 경로: `C:\Users\USER\Desktop\swim-mall\backend\build`
   - 역할: Gradle이 컴파일한 `.class` 파일과 빌드 결과물 저장
   - 삭제 영향: 없음 (다음 빌드 시 자동으로 재생성됨)
   - 삭제 방법:
     ```
     방법 A: 파일 탐색기에서
     1. 파일 탐색기에서 `C:\Users\USER\Desktop\swim-mall\backend` 폴더 열기
     2. `build` 폴더 선택 후 Shift+Delete
     
     방법 B: PowerShell에서
     Remove-Item -Path "C:\Users\USER\Desktop\swim-mall\backend\build" -Recurse -Force
     
     방법 C: Gradle 명령어로 (권장)
     cd C:\Users\USER\Desktop\swim-mall\backend
     ./gradlew clean
     ```

   **3단계: 프로젝트 다시 열기**
   - IntelliJ IDEA 실행
   - Open → `C:\Users\USER\Desktop\swim-mall\backend` 폴더 선택
   - "Import Gradle Project" 알림이 뜨면 클릭
   - Gradle 동기화 완료될 때까지 대기 (우측 하단 진행 표시 확인)

   **💡 팁:**
   - 삭제 전에 IntelliJ IDEA가 완전히 종료되었는지 확인하세요 (작업 관리자에서 확인)
   - `.idea` 폴더는 숨김 폴더일 수 있으므로 "숨긴 항목" 표시를 활성화하세요
   - 삭제 후 첫 빌드/실행이 조금 느릴 수 있습니다 (캐시 재생성 때문)

### 해결 방법 3: 메인 클래스 설정 확인

1. **Run Configuration 확인**
   - Run → Edit Configurations...
   - "+" 버튼 클릭 → "Application" 선택
   - Name: `SwimMallApplication`
   - Main class: `com.swimshop.swim_mall.SwimMallApplication`
   - Use classpath of module: `swim-mall.main`
   - JRE: 17

2. **프로젝트 구조 확인**
   - File → Project Structure (Ctrl+Alt+Shift+S)
   - Project → SDK: Java 17
   - Modules → `swim-mall` → Sources 탭에서 `src/main/java`가 Sources로 표시되는지 확인

### 해결 방법 4: Gradle Wrapper 재생성

터미널에서 다음 명령 실행:

```bash
cd backend
./gradlew wrapper --gradle-version 8.5
```

### 해결 방법 5: 터미널에서 실행 (임시 해결책)

IDE 문제가 계속되면 터미널에서 실행:

```bash
cd backend
./gradlew bootRun
```

## 확인 사항

- [ ] Java 17이 설치되어 있고 IDE에서 인식되는지 확인
- [ ] Gradle이 제대로 동기화되었는지 확인 (우측 Gradle 탭 확인)
- [ ] `build.gradle` 파일이 올바른 위치에 있는지 확인
- [ ] 포트 8080이 비어있는지 확인 (`netstat -ano | findstr :8080`)

## 추가 디버깅

문제가 계속되면 다음 정보를 확인하세요:

1. **IDE 로그 확인**
   - Help → Show Log in Explorer
   - `idea.log` 파일 확인

2. **Gradle 빌드 로그 확인**
   - 터미널에서 `./gradlew clean build` 실행
   - 오류 메시지 확인
