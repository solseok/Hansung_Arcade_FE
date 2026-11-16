# Hansung Univ - Network Programming

## 개발 환경

| name | description |
| :---: |:-----------:|
| Java |     21      |
 | Gradle |    9.5.1    |

## 빌드 · 실행

### JDK 21 설치
JDK 미설치 시 먼저 설치합니다. 설치되어 있다면, 건너뛰셔도 됩니다.
```bash
# macOS (Homebrew)
brew install --cask temurin@21
```

Windows의 경우
```bat
winget install EclipseAdoptium.Temurin.21.JDK
```

설치 후 `java -version`으로 확인하세요. Windows는 cmd 창을 새로 열어야 `java`를 인식합니다.

### 빌드, 실행, 테스트

```bash
# Linux, macOS, Git bash
./gradlew build                      # 컴파일 + 테스트
./gradlew run                        # 실행
./gradlew clean test                 # 캐시 없이 테스트 실행
```

`permission denied`가 나오면 `chmod +x gradlew`를 한 번 실행한 뒤 다시 시도하세요.

Windows의 경우
```bat
gradlew build
gradlew run
gradlew clean test
```

PowerShell에서는 앞에 `.\`를 붙입니다. 예: `.\gradlew build`

### 테스트 결과

`build/reports/tests/test/index.html`을 브라우저로 열면 테스트 결과를 볼 수 있습니다.

## IDE에서 열기

- **IntelliJ**: 저장소 폴더를 열면 Gradle 프로젝트로 인식합니다. 알림이 뜨면 *Load Gradle Project*를 누르세요.
- **Eclipse**: File → Import → Gradle → Existing Gradle Project에서 저장소 폴더를 선택합니다.

`.idea/`, `.project`, `.classpath`, `.settings/`는 각자 IDE가 만드는 개인 설정이라 커밋하지 않습니다.

## 컨벤션

### 브랜치

| 브랜치 | 용도 |
| --- | --- |
| `main` | 제출용. 직접 push하지 않고 `develop`에서 PR로만 합칩니다. |
| `develop` | 개발 통합 브랜치이자 GitHub 기본 브랜치. 모든 작업 브랜치의 PR 대상입니다. |
| `<tag>/<이슈번호>-<설명>` | 티켓 하나당 하나. `develop`에서 만듭니다. 예: `feat/12-waiting-room` |

작업 브랜치의 설명은 영어 소문자와 `-`로 씁니다.

### 티켓 (이슈)

제목은 `[tag] 제목` 형식으로 씁니다. 예: `[feat] 대기실 화면 구현`, `[fix] 물풍선이 블럭을 통과함`

| tag | 용도 |
| --- | --- |
| `feat` | 새 기능 · 화면 |
| `fix` | 버그 수정 |
| `refactor` | 동작은 그대로 두고 구조 개선 |
| `test` | 테스트 추가 · 수정 |
| `docs` | 문서 |
| `style` | 포맷 · 코드 스타일 |
| `chore` | 빌드 · CI · 설정 |

티켓 하나 = 브랜치 하나 = PR 하나로 작업합니다.

### 커밋 메시지 (Gitmoji)

```
:gitmoji: 제목

본문 (선택)
```

- 제목은 50자 이내로, 끝에 마침표 없이 무엇을 했는지 적습니다. (`~ 구현`, `~ 추가`, `~ 수정`)
- 깃모지는 이모지 문자 대신 `:sparkles:` 같은 코드로 씁니다. GitHub에서는 이모지로 보이고, 터미널에서도 그대로 읽힙니다.
- 본문은 제목 아래 한 줄을 비우고, 무엇을 왜 바꿨는지 적습니다.

| 깃모지 | 코드 | 언제 | 티켓 tag |
| :---: | --- | --- | --- |
| 🎉 | `:tada:` | 프로젝트 시작 | |
| ✨ | `:sparkles:` | 새 기능 | `feat` |
| 💄 | `:lipstick:` | UI · 화면 디자인 | `feat` |
| 🍱 | `:bento:` | 이미지 · 사운드 에셋 추가 · 수정 | `feat` |
| 🐛 | `:bug:` | 버그 수정 | `fix` |
| 🚑️ | `:ambulance:` | 급한 버그 수정 | `fix` |
| ♻️ | `:recycle:` | 리팩터링 | `refactor` |
| 🚚 | `:truck:` | 파일 · 폴더 이동, 이름 변경 | `refactor` |
| 🔥 | `:fire:` | 코드 · 파일 삭제 | `refactor` |
| ✅ | `:white_check_mark:` | 테스트 추가 · 수정 | `test` |
| 📝 | `:memo:` | 문서 | `docs` |
| 🎨 | `:art:` | 코드 포맷 · 구조 정리 | `style` |
| 🔧 | `:wrench:` | 설정 파일 | `chore` |
| 👷 | `:construction_worker:` | CI | `chore` |
| ➕ | `:heavy_plus_sign:` | 의존성 추가 | `chore` |
| 🔀 | `:twisted_rightwards_arrows:` | 브랜치 병합 | |
| ⏪️ | `:rewind:` | 변경 되돌리기 | |

전체 목록은 [gitmoji.dev](https://gitmoji.dev)에서 볼 수 있습니다.

```
:sparkles: 대기실 화면 구현
:bug: 물풍선이 블럭을 통과하는 문제 수정
:construction_worker: CI 워크플로 추가
```

### PR

- 제목은 티켓 제목과 같게 씁니다. 예: `[feat] 대기실 화면 구현`
- 본문은 템플릿을 채우고 `Closes #이슈번호`를 남깁니다. `develop`에 머지되면 티켓이 자동으로 닫힙니다.
- CI(`Build & Test`)가 통과하고 다른 팀원 1명이 Approve하면 머지합니다.
- 머지는 **Create a merge commit**으로 합니다. 커밋 하나하나의 깃모지와 작성자 기록을 그대로 남기기 위해서입니다.

### 코드 스타일 (Java)

| 대상 | 규칙 | 예 |
| --- | --- | --- |
| 클래스 · 인터페이스 · enum | PascalCase | `GameScene`, `ObjectManager` |
| 메서드 · 변수 | camelCase | `placeBalloon()`, `playerSpeed` |
| 상수 | UPPER_SNAKE_CASE | `TILE_SIZE`, `MAX_PLAYERS` |
| 패키지 | 소문자 | `core`, `scene` |

- 들여쓰기는 스페이스 4칸, 파일 인코딩은 UTF-8입니다.
- 멤버 변수에 `m_` 같은 접두사를 붙이지 않습니다.
- 와일드카드 import(`import java.util.*;`)는 쓰지 않습니다.

## 아웃게임 목업 실행

기본 실행은 로그인입니다. 닉네임(1~12자), 서버 주소·포트 입력을 검사한 뒤 오프라인 로비로 이동합니다.
`--app.start-screen=lobby`로 로비에 바로 들어갈 수 있습니다.
로비는 페이지당 8개 방을 표시하며, 방 만들기 정원은 2~8명입니다.
대기실에는 8개 슬롯, 캐릭터·팀 선택, 방장 전용 맵 선택과 시작, 참가자 준비, 채팅·퇴장이 있습니다.
다른 참가자는 준비된 목업입니다. 참가자의 준비는 로컬 상태만 바꾸며 서버로 전달하지 않습니다.
방장은 혼자서도 화면 흐름을 확인할 수 있습니다.
게임 시작은 선택한 맵·캐릭터로 기존 인게임을 실행합니다. 실제 다인 게임 동기화와 승패 연동은 후속 작업입니다.
인게임 아래의 ‘목업 결과 확인’으로 샘플 순위·승자·플레이 시간을 표시하고 같은 대기실로 돌아갈 수 있습니다.
결과에서 돌아올 때 방 인원을 유지하고, ‘로비로 나가기’에서만 퇴장합니다.

메뉴에서 음소거할 수 있습니다. 음원은 임시 배정이며 Camp는 로그인·로비, Patrit는 대기실,
Village는 게임 미리보기·결과에 사용합니다. 입장에는 Ntc_GameReady, 결과에는 Ntc_GameEnd를 재생합니다.
오디오 장치가 없거나 재생에 실패하면 로그를 남기고 화면은 계속 사용할 수 있습니다.
`src/main/resources/ui`의 두 버튼 PNG는 사용자가 제공한 참고 화면에서 사용 허락을 받아 추출했습니다.

인게임 직접 실행은 `./gradlew run --args="--app.start-screen=game"`을 사용합니다.

### Eclipse와 Codex에서 같은 코드 사용

두 도구에서 같은 저장소 폴더를 사용한다. 외부 수정 후 Eclipse에서 F5로 새로고침하고,
의존성이 바뀌면 프로젝트 우클릭 → Gradle → Refresh Gradle Project를 실행한다.
실행 중인 게임은 종료 후 다시 실행해야 변경 내용을 볼 수 있다.

### 아웃게임 구현 범위

- #6~#11: 로그인, 채팅 컴포넌트, 8개씩 표시하는 로비, 방 생성·입장,
  8인 대기실, 결과 화면을 오프라인 목업으로 연결했다.
- 로비 채팅은 사용자 요청으로 제외했다. 방 채팅은 유지한다.
- #10: 팀 색 8종을 선택하며, 방장이 고른 맵은 방 목록과 시작 데이터에 함께 반영된다.
  참가자가 준비하지 않았으면 시작할 수 없고, 시작 시 게임중·결과 복귀 시 대기중으로 바뀐다.
  다른 참가자는 기본적으로 준비된 가짜 참가자다. 실제 다른 PC와의 준비 동기화는 없다.
- #12: 화면 BGM, 버튼 클릭, 방 입장·퇴장, 시작·결과 알림과 음소거를 제공한다.
  클릭에는 제공 음원 Ntc_Count1, 퇴장에는 Ntc_GameEnd를 임시 사용한다.
- #25: 현재 develop 인게임과 목업 결과로 화면 순환을 확인한다.
  PR #33의 실제 MatchResult 수신 및 다인 게임 시작 연결은 해당 인게임 통합 후 진행한다.
  아웃게임의 팀 색 선택값은 시작 데이터에 담으며, 현재 단일 캐릭터 미리보기에는 색 변환을 적용하지 않는다.

### 주간 제출용 소스 폴더

제출 기준은 Eclipse에서 실행하는 Hansung_Arcade 저장소다.
Hansung_Arcade_worktrees는 별도 Git 보조 작업 공간이므로 제출하지 않는다.
ZIP에는 src/, gradle/wrapper/, gradlew, gradlew.bat, build.gradle, settings.gradle,
README.md 등 소스와 실행에 필요한 파일을 넣는다.
.git/, .gradle/, build/, bin/, output/과 보조 worktree는 제외한다.
IDE 개인 설정은 필수가 아니며 Eclipse에서는 Gradle 프로젝트로 가져올 수 있다.
제출 시점에는 팀 작업이 통합된 커밋과 실행 상태를 확인한 뒤 그 버전으로 ZIP을 만든다.

로비에는 상태별 탭을 두지 않는다. 대기중 방을 앞에, 게임중 방을 뒤에 정렬한 하나의 목록을 8개씩 표시한다.
로그인은 마스코트 배경과 하단 주황색 입력 패널로 구성하며 닉네임·서버 주소·포트를 받는다. 계정 비밀번호나 2P 로그인은 제공하지 않는다.
