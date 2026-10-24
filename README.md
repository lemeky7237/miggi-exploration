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
