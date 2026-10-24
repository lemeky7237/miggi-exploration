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

## 몬스터 (v0.5 진행 중) — 현재 상태
- 서버 쪽 완료·컴파일·서버 부팅 검사 통과: `mobs/` 패키지
  - `FantasyMonster`: 공통 기반 (전용 AI 목표, 공격 예비동작 후 타격, 애니메이션 이벤트 동기화)
  - 고블린(HP16, 공격3, XP15) / 오크(HP40, 공격8, 내려찍기·횡베기, XP40) / 멧돼지(HP30, 공격6, 들이받아 띄우기, XP20) / 미노타우르스(HP300, 공격14, 돌진·내려찍기, 보스, XP500)
  - 처치 시 RPG XP: `MiggiExploration`의 `ServerLivingEntityEvents.AFTER_DEATH`
  - 애니메이션 id·길이: `FantasyMonster.ANIM_*`, `animDuration()` / 클라이언트에서 `currentAnim(partial)`, `animTicks(partial)`로 읽음
- **아직 없음: 클라이언트 렌더러·3D 모델·텍스처.** 렌더러 등록 전에는 클라이언트에서 소환하면 게임이 멈출 수 있음
- 26.3에서 없는 것: `Zombie`/`Hoglin` 등 바닐라 몬스터 클래스 경로(옛 경로로 import 불가), `Entity.hurtMarked`, `options.hideGui`


## v0.5 클라이언트 몬스터 렌더링 추가 (GPT 제작)
### 이번에 바뀐 것
- 기존 서버 `mobs/` 구현은 **수정하지 않음**. `MiggiClient`에 `FantasyRenderers.register()` 한 줄을 추가해 4종 엔티티 렌더러 및 모델 레이어를 등록.
- `src/client/java/.../client/mobs/`에 `FantasyRenderState`, `FantasyModel`, `FantasyMobRenderer`, `FantasyRenderers` 추가.
- 고블린(1.35), 오크(2.35), 멧돼지(1.5), 미노타우르스(3.2) 각각 다른 큐브 조합: 관절 피벗, 미노타우르스 뿔, 오크·미노타우르스의 도끼를 손 파츠의 자식으로 구성. 각 종별 충돌 판정은 기존 `ModMobs`에서 그대로 관리.
- 128×128 PNG 텍스처 4장 및 ko_kr/en_us 몬스터 이름을 추가.
- `FantasyMonster` 이벤트로 전달받는 공격 종류·진행 틱을 `FantasyRenderState`로 복사하고 `setupAnim`에서 걷기/공격/스턴 기본 포즈를 적용.
- 신규 라이브러리, 빌드 설정, 서버 로직 변경, 파일 삭제 없음.

### 아직 안 된 것 / 확인 필요
- **GitHub Actions 컴파일과 실제 클라이언트 실행은 이 환경에서 확인하지 못함.** 전용 서버 부팅만으로는 클라이언트 렌더러 오류를 검출할 수 없음.
- 특히 `ModelLayerRegistry`·`EntityRenderers`의 26.3 정확한 패키지 및 `LivingEntityRenderState` 시선각 필드(`xRot`, `yRot`)는 dev 빌드 에러가 나면 확인해야 함. 26.2 공식 예시에서 큰 구조를 참조했지만 26.3 로컬 컴파일 검증은 없음.
- 텍스처는 초기형 픽셀 아틀라스이므로 정교한 UV 페인팅/추후 Blockbench 모델 개선이 필요.
- 공격 포즈는 키프레임 기반 보간의 초안이고, 타격 순간/무기 손잡이 위치의 실제 시각적 검증은 미완료.
- 월드 자동 스폰, 보스 미궁, 별도 사운드/입자, 인게임 스크린샷은 아직 구현/확인되지 않음.
- **테스트:** dev Actions 컴파일 후, 반드시 클라이언트 JAR로 `/summon miggi_exploration:goblin`, `orc`, `wild_boar`, `minotaur`를 각각 실행하여 화면 출력·이동·공격·콘솔 오류를 확인할 것.

### 기술 근거
- Fabric 공식 엔티티 튜토리얼(26.2): https://docs.fabricmc.net/develop/entities/first-entity
- Fabric 26.3 릴리스: https://www.fabricmc.net/2026/09/15/263.html


## v0.6 — 필드 자연 스폰·드롭 테이블 (GPT 제작)
### 이번에 바뀐 것
- `MonsterSpawns.java`: 26.3 Fabric biome API `BiomeModifications.addSpawn`를 사용해 일반 몬스터 3종을 바닐라 오버월드 바이옴에 추가.
  - 고블린: 숲/자작나무 숲/어두운 숲, weight 28, 무리 1~3
  - 오크: 어두운 숲/타이가/오래된 가문비 타이가, weight 11, 무리 1~2
  - 멧돼지: 숲/자작나무 숲/타이가, weight 22, 무리 1~3
  - `weight`는 다른 몬스터와의 상대적인 등장 가중치이며 확률(%)이 아님.
- `MiggiExploration.onInitialize`에서 엔티티 등록 직후 `MonsterSpawns.register()` 호출.
- `data/miggi_exploration/loot_table/entities/`에 몬스터별 바닐라 아이템 기반 전리품 테이블 4개 추가.
- 기존 `ServerLivingEntityEvents.AFTER_DEATH`의 RPG 경험치 지급은 **이미 구현**되어 있으므로 수정하지 않음: 고블린 15 / 오크 40 / 멧돼지 20 / 미노타우르스 500.
- 미노타우르스는 **자연 스폰 목록에 넣지 않음** (미궁 전용 보스 예정).
- 신규 의존성 / 삭제 파일 없음. 서버 AI 및 클라이언트 렌더링 코드 변경 없음.

### 아직 안 된 것 / 확인 필요
- 이 ZIP은 **로컬 Java 25 / Gradle 컴파일 및 전용 서버 부팅 검사 미실시**. GitHub `dev` Actions로 반드시 확인.
- `BiomeModifications.addSpawn`의 26.3 소스 시그니처는 확인했지만, 실제 몬스터 자연 생성은 **클라이언트에서 별도 확인 필요**. 특히 스폰 배치 조건, 조명/지형, 몬스터 캡에 따라 등장하지 않을 수 있음.
- 현재 멧돼지는 MONSTER 카테고리이며 낮에도 평범한 야생동물처럼 출현하도록 설계한 상태가 **아님**. 평화로운 동물 스폰으로 전환하려면 엔티티 카테고리와 AI 등 별도 설계 필요.
- 드롭은 기존 바닐라 아이템의 초기 임시 밸런스. 별도의 고블린 귀, 오크 송곳니, 미노타우르스 유물 아이템·텍스처는 아직 미구현.
- Loot table 데이터는 Java Edition 26.3의 `loot_table` 폴더 및 26.3 pool entry 필드 규칙을 참조했지만, 실제 몬스터 처치 시 드롭 확인 필요.
- 자연 스폰 빈도 및 보스 드롭은 플레이테스트로 밸런스 조정 예정.

### 26.3 근거
- Fabric 26.3 BiomeModifications 소스 (`addSpawn`): https://raw.githubusercontent.com/FabricMC/fabric/26.3/fabric-biome-api-v1/src/main/java/net/fabricmc/fabric/api/biome/v1/BiomeModifications.java
- Fabric 26.3 BiomeSelectors 소스 (`includeByKey`): https://raw.githubusercontent.com/FabricMC/fabric/26.3/fabric-biome-api-v1/src/main/java/net/fabricmc/fabric/api/biome/v1/BiomeSelectors.java
- Minecraft 26.3 릴리스 기술 변경: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3

### GitHub Actions 후 인게임 테스트
1. `/summon miggi_exploration:goblin ~ ~ ~` 등 4종 소환 후 처치해 RPG XP와 드롭 확인.
2. 난이도를 평화로움 이외로 설정한 후 숲·어두운 숲·타이가에서 자연 생성 확인. 조명과 주변 적대적 몬스터 수에 영향받음.
3. 미노타우르스가 오버월드에서 자연 생성되지 않는지 확인.
4. 클라이언트에 4종 모델·텍스처가 정상 출력되는지 확인 (`Actions` 전용 서버 부팅만으로 판정 불가).
