# 미끼탐험모드 (Miggi Exploration)

Fabric 모드 · Minecraft 26.3 · Java 25 · Fabric Loader 0.19.5 · Fabric API 0.164.0+26.3

## 지금 들어 있는 기능
- **탐험 일지** (J 키): 왼쪽 페이지는 발견한 장소(바이옴) 목록, 오른쪽 페이지는 도전과제와 현재 칭호
- **새로운 장소 발견 두루마리**: 처음 들어간 바이옴 이름이 화면 위에 표시됨 (도전과제 달성도 같은 두루마리)
- **도전과제 13개**: 달성하면 칭호를 얻음. 마지막에 달성한 것의 칭호가 "현재 칭호"

## 파일 구조
```
src/main/java/com/miggi/exploration/
  MiggiExploration.java   서버 쪽: 0.5초마다 플레이어 바이옴 확인 → 발견/도전과제 기록
  ExploreData.java        플레이어별 진행 데이터 (발견 장소, 달성 도전과제) — 저장 + 클라이언트 자동 동기화
  Challenges.java         도전과제 목록 (id, 목표치, 진행도 계산식)
src/client/java/com/miggi/exploration/client/
  MiggiClient.java        J 키, 새 발견 감지 → 두루마리 띄우기
  JournalScreen.java      탐험 일지 화면
  DiscoveryBanner.java    화면 위 두루마리 알림
src/main/resources/assets/miggi_exploration/
  lang/ko_kr.json, en_us.json   모든 글자 (도전과제 이름·설명·칭호 포함)
  textures/gui/                 journal.png, discovery_scroll.png, arrows.png
tools/art/                      위 텍스처를 만드는 파이썬 스크립트
.github/workflows/build.yml     푸시하면 빌드 + 서버 부팅 검사 + jar 브랜치에 jar 게시
```

## 도전과제 추가하는 법
1. `Challenges.java`의 `ALL` 목록에 `new Challenge("id", 목표치, d -> 진행도)` 한 줄 추가
2. `ko_kr.json` / `en_us.json`에 `challenge.miggi_exploration.<id>`, `.desc`, `.title` 세 줄 추가

## 텍스처 규칙
- `journal.png`: 1024×512 파일 왼쪽 위 768×480 영역에 책 그림 (게임에선 384×240으로 2배 축소해서 그림)
- `discovery_scroll.png`: 1024×256 파일 왼쪽 위 640×160 영역 (게임에선 240×60)
- `arrows.png`: 64×32, 32×32 칸 두 개 (보통 / 마우스 올렸을 때)
- 그림을 바꿀 땐 크기·위치만 지키면 코드 수정 필요 없음
- 다시 만들기: `cd tools/art && python3 journal.py <출력폴더>` (scroll.py, arrows.py 동일, numpy·scipy·Pillow 필요)

## 26.3 API 주의점 (예전 버전 자료와 이름이 다름)
- 26.x는 난독화 해제 버전: 매핑 없이 `implementation`으로 의존성 추가
- 화면 그리기: `GuiGraphics` → `GuiGraphicsExtractor`, `render(...)` → `extractRenderState(...)`
- 화면 열기: `Minecraft.getInstance().gui.setScreen(...)`, 현재 화면은 `client.gui.screen()`
- 글자: `graphics.text(font, component, x, y, argbColor, shadow)` (색은 반드시 알파 포함 ARGB)
- 그림: `graphics.blit(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, w, h, 영역w, 영역h, 텍스처w, 텍스처h[, argb])`
- 변환: `graphics.pose()`는 2D 행렬 — `pushMatrix / translate(x, y) / scale(s) / popMatrix`
- 키: `KeyMapping.Category.register(id)`, 키 코드는 `org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_*`
- 마우스: `mouseClicked(MouseButtonEvent event, boolean doubleClick)`, 좌표는 `event.x()`, `event.y()`
- HUD: `HudElementRegistry.addLast(id, (graphics, deltaTracker) -> ...)`
- 플레이어 데이터: Fabric Data Attachment API (`AttachmentRegistry.create(...).persistent(...).syncWith(...)`)
- `ResourceLocation` → `Identifier` (`Identifier.fromNamespaceAndPath(ns, path)`)

## 빌드
`main` 브랜치에 푸시하면 GitHub Actions가 컴파일하고, 전용 서버를 띄워 크래시 없이 켜지는지 확인한 뒤
완성된 jar를 `jar` 브랜치에 올림. (공개 저장소라 Actions 무료)


## RPG 시스템 추가 (GPT-6 초안)
- `RpgData.java`: 레벨·현재 XP·현재 MP·아이템 보너스 MP의 별도 플레이어 attachment.
- `RpgSystem.java`: 레벨 상한 150, 다음 레벨 XP `100 + 25*(level-1)`, Lv업당 최대 MP +20, 기본 100 MP, 4틱마다 1 MP 회복(20 TPS 기준 초당 5).
- `RpgHud.java`: 26.3 `GuiGraphicsExtractor` 기반 숫자 HUD. 기존 탐험 일지와 분리.
- `MiggiExploration.java`: 신규 탐험 도전과제가 처음 완료될 때만 RPG XP를 지급한다. **보상량은 아직 미확정이어서 임시로 도전과제당 100 XP** (`RpgSystem.CHALLENGE_REWARD_XP`).
- 사망 시 attachment는 복사되어 레벨/XP가 유지된다. 사망 후 리스폰 시 `ServerPlayerEvents.AFTER_RESPAWN`에서 MP를 최대치로 복원한다.
- 장래 아이템 확장용 `bonusMana` 필드를 지금부터 저장한다.
- 주의: 이 수정본은 로컬에서 Java 25/Gradle 빌드가 불가능하여 컴파일 검증되지 않았다. GitHub Actions의 실제 빌드 및 서버 구동 로그를 확인한 뒤 통합해야 한다.
