# 영화 리뷰장

웹서비스설계 JWT 게시판 과제입니다.

## 작성자와 서비스 소개

- 학번 / 이름: 202212969 / 오승재
- 서비스 목적과 대상 사용자: 영화를 좋아하는 사람들이 자기가 본 영화를 짧게 리뷰하고 평가하며, 다른 사람이 작성한 리뷰를 읽을 수 있는 게시판입니다. 로그인하지 않아도 리뷰를 읽을 수 있고, 로그인하면 리뷰를 작성할 수 있습니다. 수정과 삭제는 본인이 작성한 리뷰만 할 수 있습니다.
- 선택한 기술: React + Spring Boot
- 주요 기능: 공개 목록·상세 조회, 로그인과 JWT 발급, 내 정보 조회, 본인 글 작성·수정·삭제, 토큰 만료·로그아웃 처리, SQLite 데이터 유지

## 보고서와 테스트 증거

- [구현 보고서 PDF](docs/report.pdf)
- [테스트 증거](docs/evidence/)

## 프로젝트 구조

```text
.
├── README.md
├── .gitignore
├── frontend-react/    # React 프론트 소스와 의존성 파일
├── backend-spring/    # Spring Boot 백엔드 소스와 의존성 파일
├── tools/
│   └── check_api.py   # 제공 자동 API 검사 도구
└── docs/
    ├── report.pdf
    └── evidence/      # 테스트 캡처
```

## 실행 환경

| 항목 | 사용 버전 |
|---|---|
| 운영체제 | Windows 11 |
| 프론트 런타임 Node.js | Node.js 26.7.0, npm 11.19.0 |
| 백엔드 런타임 | JDK 18 (JDK 17 이상 필요) |
| 프레임워크 | React 19.3.0, Vite 8.3.4, Spring Boot 3.5.16 |
| 데이터 저장 | SQLite (필수) |

## 설치와 실행

터미널 두 개를 사용합니다. 터미널 A는 백엔드, 터미널 B는 프론트입니다.

### 1 저장소 복제

```sh
git clone https://github.com/h1he11o/jwt-assignment.git
cd jwt-assignment
```

### 2 백엔드 준비와 실행 (터미널 A)

먼저 JWT 비밀 키를 새로 만듭니다. 출력된 64자리 16진수 문자열을 아래 명령의 `<새 키>` 자리에 넣습니다.

```sh
node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"
```

Windows PowerShell:

```powershell
cd backend-spring
$env:JWT_SECRET='<새 키>'
.\mvnw.cmd package
java -jar target/jwt-board-1.0.0.jar
```

macOS / Linux:

```sh
cd backend-spring
export JWT_SECRET='<새 키>'
chmod +x mvnw
./mvnw package
java -jar target/jwt-board-1.0.0.jar
```

개발 중에는 `.\mvnw.cmd spring-boot:run` (macOS/Linux는 `./mvnw spring-boot:run`)으로도 실행할 수 있습니다.

- 환경설정 예시 파일: `backend-spring/env.example.txt`
- JWT_SECRET 설정 방법: 위 node 명령으로 새 키를 만들고, 서버를 실행할 터미널에서 환경변수로 넣습니다. Spring은 `.env`를 자동으로 읽지 않습니다. 환경변수는 터미널 창마다 따로이므로 새 창에서는 다시 넣어야 합니다. 빈 키, 형식이 틀린 키, 예시 키는 서버가 거부합니다. 실제 키는 저장소와 문서에 기록하지 않습니다.
- TOKEN_TTL: 기본값 90초. 만료 테스트는 서버를 끄고 `$env:TOKEN_TTL='3'` (macOS/Linux는 `export TOKEN_TTL=3`)을 넣은 뒤 다시 실행합니다. 원래대로 돌리려면 `Remove-Item Env:TOKEN_TTL` (macOS/Linux는 `unset TOKEN_TTL`) 후 다시 실행합니다.
- 백엔드 주소: http://127.0.0.1:8000
- 정상 실행 확인 방법: 브라우저에서 http://127.0.0.1:8000/api/health 를 열어 `status`가 `ok`인지 확인합니다.

### 3 프론트 빌드와 로컬 실행 (터미널 B)

백엔드를 켜 두고 새 터미널에서 실행합니다.

```sh
cd frontend-react
npm ci
npm run build
npm run preview
```

- 최종 확인 주소: http://127.0.0.1:4173
- 개발 중 실행: `npm run dev`, http://127.0.0.1:5173
- API 연결 방식과 포트 변경 방법: 브라우저는 4173번으로 접속하고, 프론트가 보내는 `/api` 요청은 Vite 프록시가 8000번 백엔드로 전달합니다. 백엔드 포트를 바꾸면 `frontend-react/vite.config.js`의 `server.proxy`와 `preview.proxy`를 모두 바꾸고 프론트를 다시 시작합니다.
- 종료: 프론트와 백엔드를 실행한 각 터미널에서 Ctrl+C
- 코드 변경 후 재실행: 프론트는 Ctrl+C 후 `npm run build` → `npm run preview`, 백엔드는 Ctrl+C 후 `.\mvnw.cmd package` → `java -jar target/jwt-board-1.0.0.jar`

### 4 테스트 계정과 DB

| 사용자 | 이메일 | 실습용 비밀번호 |
|---|---|---|
| Alice | alice@class.test | Classroom!2026 |
| Bob | bob@class.test | Classroom!2026 |

- SQLite 파일 위치와 자동 생성 방식: `backend-spring/data/board.sqlite`. 서버를 처음 실행하면 폴더와 테이블이 자동으로 만들어집니다.
- DB 초기화 방법: 서버를 종료한 뒤 `backend-spring/data/board.sqlite` 파일을 삭제하고 다시 실행합니다.
- 서버 재시작 후 데이터 유지 확인 결과: 서버를 끄고 다시 켠 뒤에도 기존 글과 작성자·내용이 그대로 남아 있었습니다. (보고서 4절 T15)

위 계정은 과제 전용 테스트 계정입니다. 실제 서비스 계정 정보나 서버 비밀 키를 기록하지 않습니다.

## 구현 완료 범위

| 기능 | 완료 여부 | 주요 파일 또는 함수 |
|---|---|---|
| 공개 목록과 상세 | 완료 | BoardController.java, PostStore.java (제공 코드) |
| 로그인과 JWT 발급 | 완료 | AuthService.java / issueToken(), api.js / login() |
| JWT 검증과 내 정보 조회 | 완료 | AuthService.java / currentUserId(), api.js / authorizationHeaders() |
| 글 작성과 작성자 저장 | 완료 | BoardController.java / create(), AuthService.java / currentUserId() |
| 본인 글 수정·삭제 | 완료 | AuthService.java / assertOwner(), api.js / canEdit() |
| 토큰 만료와 로그아웃 | 완료 | api.js / request(), clearSession() |
| 입력 오류와 권한 오류 처리 | 완료 | main.jsx / describeError(), run() |

알려진 문제와 미완성 기능:
- 로그아웃은 브라우저 메모리의 토큰만 지우므로, 이미 복사된 토큰은 만료 전까지 서버에서 유효합니다. (과제 기준상 정상)
- 토큰을 메모리에만 저장하므로 새로고침하면 로그인이 풀립니다. (과제 기준상 정상)
- 백엔드가 꺼져 있으면 화면에 영어 오류 문구가 그대로 보입니다.

## 테스트 방법과 결과 요약

- 실행한 검사 명령과 실행 위치: 저장소 루트에서 `py tools/check_api.py --mode final` (macOS/Linux는 `python3`) 실행 결과 모든 항목 PASS. `TOKEN_TTL=3`으로 서버를 재시작한 뒤 `py tools/check_api.py --mode final --expiry` 실행 결과 48개 항목 모두 PASS. 검사 도구는 백엔드(8000번)에 직접 요청합니다.
- Alice/Bob 권한 구분 확인 결과: Bob의 토큰으로 Alice 글을 수정·삭제하면 403 FORBIDDEN이고 글은 바뀌지 않았습니다. Bob은 본인 글의 작성·수정·삭제에 성공했습니다.
- HTTP 상태와 오류 code 확인: 200·201·204와 400 BAD_INPUT, 401 INVALID_CREDENTIALS·TOKEN_REQUIRED·INVALID_TOKEN·TOKEN_EXPIRED, 403 FORBIDDEN, 404 NOT_FOUND를 직접 확인했습니다. (보고서 4절 HTTP 응답 확인 기록)
- 응답 헤더와 본문 확인: 등록 201에 `Location: /api/posts/{id}`, 보호 API 401에 `WWW-Authenticate: Bearer`, 삭제 204에 빈 본문을 확인했습니다.
- 토큰 없음·변조·만료 확인 결과: 토큰 없음과 헤더 형식 오류는 401 TOKEN_REQUIRED, 서명을 바꾼 토큰은 401 INVALID_TOKEN, 만료된 토큰은 401 TOKEN_EXPIRED였고 실패 후 데이터는 바뀌지 않았습니다.
- 전체 T01~T17 결과와 증거: 모두 PASS. [보고서](docs/report.pdf) 4절과 [docs/evidence/](docs/evidence/) 참고

## 핵심 구현 설명

1. 로그인: `api.js`의 `login()`이 이메일·비밀번호를 보내면 `BoardController.login()`이 계정을 확인하고, `AuthService.issueToken()`이 HS256으로 서명한 JWT(sub, iss, aud, iat, exp)를 발급합니다.
2. 토큰 보관·전송: 받은 토큰과 사용자는 `api.js`의 `session` 변수(메모리)에 저장하고, `authorizationHeaders()`가 `Authorization: Bearer <JWT>` 헤더를 붙입니다. 401을 받으면 `clearSession()`으로 로그아웃합니다.
3. 서버 검증: `AuthService.currentUserId()`가 서명, HS256, 만료, iss, aud, sub 형식, 실제 사용자를 확인하고 검증된 사용자 번호만 넘깁니다.
4. 작성자 권한: 글 작성자는 토큰의 사용자 번호로 저장하고, 수정·삭제는 `AuthService.assertOwner()`가 사용자 번호와 글 작성자 번호를 비교해 다르면 403을 보냅니다.

상세 코드 설명은 보고서 3절에 있습니다.

## AI 활용 요약과 참고 자료

- AI 사용 여부: 사용
- 도구와 사용 목적: Claude Opus 5.5 / 개념 학습, TODO 코드 초안 생성, 오류 원인 확인, 테스트 절차, 보고서 문장 정리, README 작성
- 반영 위치와 직접 수정·검증한 내용: AuthService.java, api.js, main.jsx의 구현 코드는 AI 초안을 그대로 반영하고, 한 줄씩 설명을 들으며 주석을 직접 달았습니다. T01~T17 수동 테스트와 자동 검사로 동작을 확인했습니다. 이 README도 AI가 제출용 양식에 맞춰 작성했고, 새 폴더에 clone해 README의 명령대로 실행되는 것을 직접 확인했습니다.
- 상세 활용 기록: [보고서](docs/report.pdf) 5절
- 참고한 수업 자료·외부 코드·공식 문서: 과제 제공 자료(01~07), 과제 제공 시작 템플릿(frontend-react, backend-spring), 과제 제공 검사 도구(tools/check_api.py)

## 제출 확인

- [x] README의 안내 문구를 실제 내용으로 교체했다.
- [x] 새 폴더에 clone하여 README만으로 설치·환경설정·빌드·로컬 실행했다.
- [x] 4173번 빌드 화면에서 API 연결과 전체 필수 기능을 확인했다.
- [x] GitHub에서 보고서와 증거 링크가 열린다.
- [x] 교수자가 저장소를 열람·복제할 수 있다.
- [x] 비밀 키·실제 토큰·DB·node_modules·.venv·target을 커밋하지 않았다.
- [x] 최종 push 후 저장소 URL과 제출 기준 커밋 SHA를 LMS에 제출했다.
