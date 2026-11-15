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

기본 실행은 로비입니다. 방 목록은 페이지당 8개이며, 방을 만들 때 2~8명 정원을 선택할 수 있습니다.
생성자는 방장으로, 기존 방에 입장하면 참가자로 대기실에 표시됩니다.
대기실은 방 정보·채팅·퇴장을 제공하며 준비와 게임 시작은 후속 작업입니다.
로비 메뉴에서 BGM과 효과음을 함께 음소거할 수 있습니다.
제공받은 MP3 중 Camp는 로비, Patrit는 대기실, Ntc_GameReady는 입장 알림에 임시 배정했습니다.
오디오 장치가 없거나 재생에 실패하면 로그를 남기고 화면은 계속 사용할 수 있습니다.

인게임 직접 실행은 `./gradlew run --args="--app.start-screen=game"`을 사용합니다.
