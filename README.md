
## v0.9.1 - 서버 시작 오류 수정 (2026-10-24)

**이번에 바뀐 것**
- `ArcaneContent.java`에서 `ancient_bark`를 일반 `Block`으로 등록하면서 `Blocks.OAK_LOG` 속성을 복사한 오류 수정. 일반 `Block`은 `axis` 상태를 정의하지 않으므로 26.3에서 블록 초기화 중 `IllegalArgumentException` 발생.
- `ancient_bark`의 속성 복사 원본을 `Blocks.OAK_PLANKS`로 수정. 기존 텍스처, 모델, 아이템 ID, 자연 생성 파일과 나머지 콘텐츠 유지.
- 기존 `build.gradle`, `gradle.properties`, `.github` 변경 없음. 삭제 파일 없음. 새 의존성 없음.

**아직 안 된 것 / 확인 필요**
- 이 v0.9.1 ZIP의 Java 25 GitHub Actions 컴파일 및 전용 서버 부팅은 아직 검증하지 못함.
- 인게임 모델, 나무 자연 생성 및 유적 배치의 실제 성공 여부는 별도 테스트 필요.
- `ancient_bark`는 현재 방향 전환(`axis`) 없는 일반 블록이며, 향후 회전 가능한 원목이 필요하면 `RotatedPillarBlock` 및 블록스테이트 모델을 함께 구현해야 함.

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


## v0.7 — 마법 숲 구역과 잊힌 유적 (GPT 제작)
### 이번에 바뀐 것
- **새 Java 클래스와 의존성 없이** 26.3 데이터팩 기반 Jigsaw 구조물 자동 생성 2종을 추가했음. 기존 탐험·RPG·몬스터·빌드 코드 유지.
- `arcane_grove`: 약 35×18×35 블록의 **작은 마법 숲 구역**. 발광 나무 6그루, 이끼 바닥, 자수정 중심 제단, 랜턴. 이는 **독립 바이옴이 아니라 오버월드 숲에 드물게 생성되는 구조물**임.
- `forgotten_ruin`: 약 25×10×25 블록의 잊힌 제단 유적. 무너진 석재 기둥과 중앙 제단. **미노타우르스는 아직 배치하지 않음**(미궁 전용 보스 유지).
- NBT 템플릿의 `DataVersion`은 Minecraft Java 26.3의 **5023** 사용. `worldgen/structure`, `structure_set`, `template_pool` 등록.
- 구조물 생성 위치는 숲·타이가 등 명시한 바이옴으로 한정. `spacing` 22청크(마법 숲 구역), 55청크(유적), `separation` 8/20청크. 이는 고정 거리 보장값이 아닌 random_spread 후보 그리드임.
- 파일 삭제·기존 파일 수정 없음(README 제외). `build.gradle`, `gradle.properties`, `.github` 수정 없음. 외부 의존성 없음.

### 시험용 명령어 (치트 허용, 월드에서 실행)
```
/place structure miggi_exploration:arcane_grove
/place structure miggi_exploration:forgotten_ruin
/locate structure miggi_exploration:arcane_grove
/locate structure miggi_exploration:forgotten_ruin
```
- `/place structure`가 정상 동작하는지와 `/locate structure`가 실제 새 청크 생성 결과를 탐색하는지는 **별개**로 검증. 이미 생성된 청크에는 자연 생성 구조물이 새로 생기지 않음.
- `/place template miggi_exploration:arcane_grove` / `/place template miggi_exploration:forgotten_ruin`으로 NBT 구조물만 직접 배치하는 방법도 있음. 템플릿 배치는 자동생성 JSON 문제의 구분 테스트에 유용.

### 아직 안 된 것 / 확인 필요
- **GitHub Actions 컴파일 및 전용 서버 부팅, 인게임 구조물 로딩/자연 생성은 이 환경에서 실행하지 못함.** JSON과 NBT 포맷은 자체 검사함.
- Minecraft 26.3의 Jigsaw 구조물/프로젝션·template pool 스키마에 변경이 있는 경우 서버 월드 로딩 단계에서 데이터 파싱 오류 가능. 특히 `size: 0`, `start_height`, heightmap projection은 dev 실측 후 조정 필요.
- **마법 숲은 독립 biome ID가 아님.** 추후 고유 바이옴, 몬스터 스폰 분포, 환경색, 배경음악 등은 별도 구현.
- 유적은 블록 기반 탐험 장소의 초기형이며 상자/전리품/퍼즐/몹 보초/퀘스트/미궁 연결은 미완료.
- 지형과의 경계(땅속 묻힘/허공/기존 나무 충돌)는 실제 지형에서 시각 검증 필요. 블록 범위가 청크를 넘어가며 자연 지형을 덮을 수 있음.

### 참고한 26.3 기술 자료
- Minecraft 26.3 버전 데이터 및 worldgen 변경: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3
- Jigsaw 구조물 JSON 구조: https://minecraft.wiki/w/Jigsaw_structure
- 구조물 템플릿 풀 JSON: https://minecraft.wiki/w/Template_pool
- 구조물 정의와 /locate 테스트: https://minecraft.wiki/w/Tutorial:Custom_structures


## v0.8 — 독립 마법 숲 바이옴 + 숲 내부 유적 (GPT 제작)
### 이번에 바뀐 것
- 오버월드에 `miggi_exploration:arcane_forest` 독립 바이옴을 데이터로 정의. 풀/물/하늘의 색상과 숲·꽃·동굴·광물 배치 초안을 설정.
- Biolith의 데이터 기반 `replacements`로 기존 숲(11%)·자작나무 숲(8%)의 **일부 패치**를 마법 숲으로 대체하도록 설정. 이는 전체 월드가 아닌 해당 바이옴 구역 기준 비율이며 블록 기준의 정확한 생성 확률을 의미하지 않음. 마법 숲의 실제 규모는 월드 시드·Biolith 설정에 좌우됨.
- `arcane_grove` (기존 35×35 마법 제단 숲)와 `forgotten_ruin` 구조물은 마법 숲 바이옴에서만 생성되도록 제한. 기존 NBT·구조물 세트 유지.
- ko_kr/en_us에 마법 숲 바이옴 이름 추가. 서버 AI, 3D 렌더러, 도전과제, RPG 데이터, 빌드 설정, GitHub Actions 변경 없음. 삭제 파일 없음.
- **런타임 추가 의존성:** Biolith Fabric v3.8.0-beta.1 (Minecraft 26.3)를 게임 클라이언트와 전용 서버 양쪽에 설치해야 함. `fabric.mod.json`, `build.gradle`, `gradle.properties`는 변경하지 않음. Biolith 미설치 시 배치 파일은 적용되지 않아 자연 생성하지 않음. Biolith 데이터 API이므로 모드 Java 코드에 직접 의존하지 않음. 참고: https://www.curseforge.com/minecraft/mc-mods/biolith/files/all , https://github-wiki-see.page/m/TerraformersMC/Biolith/wiki/Upgrading-v3.0-to-v3.1

### 아직 안 된 것 / 확인 필요
- **이 ZIP은 로컬 Java 25 빌드, 서버 부팅 및 실제 클라이언트 월드 생성 미검증.** GitHub Actions 검사와 실제 인게임 테스트 필요. 현재 CI에 Biolith가 설치되지 않아도 기존 서버 부팅은 가능하지만, 바이옴 생성 자체는 검증하지 못함.
- **주의: 현재 26.3 바이옴 `features`의 일부 바닐라 placed feature ID와 `attributes` 효과 키는 로컬 서버 바이옴 레지스트리로 검증되지 않음.** 월드 로드/`/locate biome` 오류 로그가 나오면 수정할 것. 마법 숲의 특수 나무·발광 생물은 현재 기존 나무·꽃·발광 이끼 및 기존 NBT 구조물 사용; 독자적 마법 나무 블록, 파티클, 음악, 안개 셰이더는 아직 없음.
- 고대 유적 생성 보장 아님: biome 조건과 structure_set spacing 모두 충족해야 생성. 새로운 월드 또는 신규 청크에서 테스트해야 함.
- `/locate biome miggi_exploration:arcane_forest` 및 `/locate structure miggi_exploration:forgotten_ruin`으로 시험. Biolith v3.8 배치 데이터 실제 적용 여부 확인 필요.
- 게임에 Biolith 의존성을 추가하는 것 자체가 기존 CI 설계에 적합하지 않다면, 자동 바이옴 배치를 비활성화하고 바이옴 정의/구조물만 유지한 다음 다른 호환 방식으로 변경해야 함.


## v0.8.1 — 26.3 월드 생성 부팅 오류 수정
### 이번에 바뀐 것
- `worldgen/biome/arcane_forest.json`: 서버 레지스트리에서 찾지 못한 배치 기능 `minecraft:trees_birch_and_oak` 참조 제거. **이 패치는 대체 나무 생성 ID가 확인될 때까지 해당 나무 생성을 임시 비활성화**한다. 나머지 기존 feature는 유지. 마법 숲은 제단 구조물과 바이옴으로 계속 등록되지만 숲다운 밀도는 추가 개선 필요.
- `worldgen/structure/arcane_grove.json`, `forgotten_ruin.json`: 26.3 코덱에서 요구한 `use_expansion_hack: false` 명시.
- 기존 탐험 도전과제·RPG·몬스터·NBT 구조물·Biolith 데이터 변경 없음. 기존 파일 **삭제 없음**. `build.gradle`, `gradle.properties`, `.github` 변경 없음. 새 의존성 없음.

### 아직 안 된 것 / 확인 필요
- 이 ZIP에 대한 Java 25 컴파일·전용 서버 부팅·게임 실행은 **미검증**. GitHub Actions에서 재검사 필요.
- 바닐라 placed feature 목록을 Minecraft 26.3 레지스트리에서 추출해, 마법 숲에 맞는 정상 나무 배치 기능을 재설정할 것. 다른 잘못된 ID가 있으면 서버가 추가 오류를 출력할 수 있다.
- Biolith 설치 환경에서 바이옴 대체가 적용되는지 및 유적이 실제 바이옴 내부에 생성되는지 인게임 확인 필요.
- GitHub 웹 업로드 시 원본에서 삭제되는 파일 없음.


## v0.8.2 — 고대 유적 생성 진단 수정 (빌드/인게임 미검증)

### 이번에 바뀐 것
- `worldgen/structure/forgotten_ruin.json`: Jigsaw 크기 깊이 0 → 1, 시작 높이 오프셋 -1 → 0, 지형 적응 `beard_thin` → `none`으로 변경. 26.3에서 시작 템플릿 배치 성공 여부를 재검증하기 위한 진단용 조정임.
- `worldgen/structure_set/forgotten_ruin.json`: 간격 55 → 18청크, 최소 분리 20 → 6청크. 마법 숲처럼 드문 바이옴에서 생성 후보를 늘림.
- NBT 구조물, 마법 숲 바이옴, 몬스터, 탐험 도전과제, RPG 시스템, 빌드 설정 변경 없음. 삭제 파일 없음. 추가 의존성 없음.

### 아직 안 된 것 / 확인 필요
- Java 25 GitHub Actions 컴파일·전용 서버 부팅 재확인 필요.
- 게임에서 `/place structure miggi_exploration:forgotten_ruin`, `/locate structure miggi_exploration:forgotten_ruin` 성공 여부 미확인.
- `/place template miggi_exploration:forgotten_ruin ~ ~ ~` 명령은 사용자 테스트에서 로딩 성공 메시지가 확인됨. 템플릿 로드 성공만으로 자동 생성까지 증명되지 않음.
- 이미 생성된 청크에는 새로운 자연 구조물 생성이 소급 적용되지 않으므로 새 월드/미탐험 청크에서 테스트.
- 마법 숲의 바이옴 규모는 시드에 따라 달라짐. 자동 생성은 마법 숲 내 배치 가능 후보 좌표가 있을 때만 일어남.
- `/place structure`가 여전히 실패하면 전체 latest.log와 위치·바이옴 정보를 확인하여 원인을 재분석.


## v0.8.3 — 마법 숲 전용 나무 / 신규 아이템 / 고대 유적 석재 (GPT)

### 이번에 바뀐 것
- `ArcaneContent.java`: 독립 블록 5종(마력목 원목·잎, 룬 석재·금 간 룬 석재·발광 룬 석재) + 수집 아이템 `arcane_shard` 등록.
- 커스텀 16px 블록·아이템 텍스처와 블록상태·모델·아이템 정의, 한영 이름, 블록 채굴 드롭, 도구 태그 포함.
- 마법 숲 바이옴의 식생 단계에 `arcane_tree` 기능 추가. Minecraft 26.3의 `worldgen/feature` + `placed_feature` 경로 적용. 마력목과 청록색 잎으로 구성된 높이 6~11블록 내외의 자연 생성 나무 시도.
- 기존 `forgotten_ruin.nbt`를 33×13×33블록 석조 유적 템플릿으로 교체. 신규 룬 석재를 지면·기둥·중앙 제단에 사용. **수동 템플릿 배치는 자연 생성 성공을 보증하지 않음.**
- 기존 13개 도전과제, RPG, mobs/ 서버 AI, .github, Gradle 설정 변경하지 않음. 새 외부 의존성 없음.
- 삭제 파일 없음. 기존 유적 템플릿 NBT는 **교체**됨.

### 아직 안 된 것 / 꼭 확인할 사항
- Java 25 컴파일, Actions 전용 서버 부팅, 인게임 자연 나무 생성·유적 생성·텍스처 렌더링은 **이 환경에서 확인하지 못함**. 초록불은 서버 데이터가 로드됨을 뜻할 뿐 모든 월드 생성이 확인되지는 않음.
- **26.3 API 확인 필요:** `ArcaneContent`의 `LeavesBlock` 생성자·레지스트리 키·BlockItem 메서드, `minecraft:tree`의 26.3 trunk/foliage 구조, `placed_feature`의 `in_square` modifier가 26.3에서 허용되는지. 실제 빌드 로그가 나오면 수정.
- Biolith는 마법 숲 바이옴 자연 배치를 위해 클라이언트와 서버에 계속 필요.
- 구조물의 `/place structure` 실패 원인이 수정되었다고 주장하지 않음. `/place template miggi_exploration:forgotten_ruin ~ ~ ~`부터 시험 권장.
- 기존 월드의 **생성 완료된 청크**에는 새 나무·유적 자연 생성이 소급 적용되지 않음. 새 월드를 권장.
- 원목은 v0.8.3에서 단순 큐브 블록이므로 가로눕힘 `axis` 속성이 없음. 잎은 드롭시 마력 파편이 약 12%로 나오도록 초기화. 새 묘목/재식재, 나무 제작대 가공, 전용 상자 보상은 차기 버전 과제.

### 테스트 명령어
```
/give @s miggi_exploration:arcane_log 64
/give @s miggi_exploration:arcane_leaves 64
/give @s miggi_exploration:rune_stone 64
/give @s miggi_exploration:glowing_rune_stone 64
/give @s miggi_exploration:arcane_shard 16
/locate biome miggi_exploration:arcane_forest
/place feature miggi_exploration:arcane_tree
/place template miggi_exploration:forgotten_ruin ~ ~ ~
/place structure miggi_exploration:forgotten_ruin
```

### 기술 자료
- https://www.fabricmc.net/2026/09/15/263.html
- https://docs.fabricmc.net/develop/blocks/first-block
- https://docs.fabricmc.net/develop/items/first-item
- https://minecraft.wiki/w/Configured_feature


## v0.8.4 — Java 26.3 LeavesBlock 생성자 수정

### 이번에 바뀐 것
- `ArcaneContent.java`: `LeavesBlock`의 26.3 생성자가 요구하는 `AmbientLeavesBlockSoundPlayer` 인자를 추가. 기본적으로 `noAmbientSound()`를 사용하여 별도 나뭇잎 환경음을 비활성화.
- GitHub Actions #15 로그의 `LeavesBlock` 생성자 인수 오류에 대응. 다른 Java 소스, 데이터, 텍스처, 기존 RPG·도전과제·몬스터 시스템은 유지.
- 삭제 파일 없음. Gradle 설정, `.github` 변경 없음. 새 의존성 없음.

### 아직 안 된 것 / 확인 필요
- Java 25/Fabric 26.3 빌드 및 전용 서버 부팅은 이번 수정본에 대해 **실행 검증 전**. GitHub Actions에서 다시 확인해야 함.
- 신규 마력목 자연 생성, 유적 자연 생성, 클라이언트 텍스처 표시·블록 드롭은 인게임 미검증.
- 공식 타입 참조: `LeavesBlock(AmbientLeavesBlockSoundPlayer, Properties)` 및 `AmbientLeavesBlockSoundPlayer.noAmbientSound()` (Minecraft 26.3).


## v0.8.5 — Minecraft 26.3 마력목 서버 부팅 오류 수정

### 이번에 바뀐 것
- `worldgen/feature/arcane_tree.json`의 누락된 `below_trunk_provider`를 추가했습니다.
- 트렁크와 잎 블록 상태 공급자를 `minecraft:simple_state_provider`에서 26.3의 `minecraft:simple`로 변경했습니다.
- 상태 필드를 `Name`/`Properties`에서 26.3의 `id`/`properties`로 수정했습니다.
- 다른 worldgen JSON에서 해당 구형 표기가 남아 있는지 검색했고, JSON 파싱을 확인했습니다.
- 기존 탐험·RPG·몹·텍스처·유적 데이터는 유지했습니다.
- `build.gradle`, `gradle.properties`, `.github`는 수정하지 않았습니다. 삭제한 파일은 없습니다.

### 아직 안 된 것 / 확인 필요
- Java 25를 이용한 GitHub Actions 컴파일 및 전용 서버 부팅 검사는 **아직 실행하지 못했습니다**.
- 마법 나무의 자연 생성, 유적의 `/place structure` 및 자연 생성, 실제 텍스처 렌더링은 아직 인게임 검증이 필요합니다.
- 외부 라이브러리나 모드는 이번 수정에서 추가하지 않았습니다.
- 자료: https://www.minecraft.net/ko-kr/article/minecraft-java-edition-26-3 및 https://www.fabricmc.net/2026/09/15/263.html


## v0.9 — 마법 숲 대형 콘텐츠 업데이트 (GPT 제작)
### 이번에 바뀐 것
- 신규 블록 **5종**: arcane_planks, ancient_bark, mossy_rune_stone, rune_pillar, rune_altar. 코드 등록, 블록 모델, 16×16 텍스처, 아이템 모델, 채굴 태그, 자기 자신을 드롭하는 테이블 추가.
- 신규 수집 아이템 **4종**: goblin_talisman, orc_fang, minotaur_horn, ancient_tablet. 현재는 능력 없는 수집용 재료이며 제작법은 미구현.
- 고블린/오크/미노타우르스의 기존 전리품을 유지하면서 전용 아이템 추가 드롭 풀을 연결 (고블린 10%, 오크 20%, 미노타우르스 100% 기본 수치).
- 기본 마력목 외 **고대 마력목**(키 큰 변종)과 **어린 마력목**(작은 변종) 배치 데이터를 독립적으로 추가. 마법 숲 바이옴에 세 변종을 배치. 26.3에서 성공한 arcane_tree 데이터 구조를 재사용.
- 신규 탐험 구조물 **마력 감시탑** 25×26×25: 4개 층과 계단·발광 룬 장식. 마법 숲 한정 구조물 등록 + NBT 수동 배치 지원.
- 신규 **미궁 입구 템플릿** 43×17×43: 방 5개, 복도, 중앙 룬 제단·봉인문. 이 단계는 수동 템플릿이며 유적 하부에 연결되는 자동 생성·보스 AI 소환은 아직 적용하지 않음.
- 기존 기능(탐험 도전과제, 일지, 칭호, RPG, 4종 몬스터 AI, 3D 모델, 바이옴, 원본 유적)은 유지.
- 기존 build.gradle, gradle.properties, .github 및 fabric.mod.json은 수정하지 않음. **추가 의존성 없음**. Biolith는 기존처럼 바이옴 배치에 필요.
### 아직 안 된 것 / 확인 필요
- Minecraft 26.3 Actions 컴파일 및 전용 서버 부팅은 본 환경에서 검증하지 못함. JSON 문법/NBT 내부 좌표/ZIP 구조만 검증.
- **특히 새 나무 feature JSON과 감시탑 worldgen 등록**은 서버 시작 시 레지스트리 검증을 통과해야 하므로 Actions 로그 확인 필수.
- 현재 3D 애니메이션은 기존 구현을 유지. GeckoLib와 Lithostitched는 사용자 다운로드 확인되었지만 **모드 Java/Gradle 의존성에 통합하지 않았음**. 두 라이브러리 설치만으로 기능이 생기지는 않음.
- 감시탑/원본 유적 자동 생성은 게임 월드에서 실측 검증 전. 신규 청크에서 /locate structure로 검사.
- 미궁 입구 자동 연결 및 미노타우르스 보스방 소환, 상자 전리품, 퍼즐은 아직 미구현.
- 미궁 템플릿은 평평한 빈 공간에 `/place template miggi_exploration:labyrinth_entrance ~ ~ ~`로 수동 배치하여 외형부터 확인.
### 새 시험 명령어
```
/give @s miggi_exploration:ancient_tablet
/give @s miggi_exploration:minotaur_horn
/give @s miggi_exploration:rune_altar
/place template miggi_exploration:arcane_watchtower ~ ~ ~
/place template miggi_exploration:labyrinth_entrance ~ ~ ~
/locate structure miggi_exploration:arcane_watchtower
```

## v1.0-dev — 독립 미궁 전투·보물 템플릿 (GPT 제작)
### 이번에 바뀐 것
- **미궁 시험장 `labyrinth_trial.nbt` 추가**: 45×18×45블록, 룬 석재 바닥·내벽·장식·출입부·보스 전투 구역, 입구 보급 상자 2개.
- 시험장 NBT에 엔티티 3종 배치 데이터: 미노타우르스 1, 오크 1, 고블린 1. 실제 템플릿 배치 시 엔티티 생성 여부는 **인게임 검증 필요**.
- **보물방 `labyrinth_vault.nbt` 추가**: 25×12×25블록, 상자 3개와 복도·장식.
- 전용 상자 전리품 테이블 `labyrinth_supplies`, `labyrinth_treasure` 추가: 빵, 황금 당근, 마력 파편, 다이아몬드, 고대 석판 등. 실제 개봉 시 결과 검증 필요.
- `tools/build_labyrinth.py` 추가: 위 NBT를 표준 라이브러리만으로 다시 생성할 수 있는 편집용 스크립트. 기존 모델·AI·XP·도전과제·자동 바이옴·구조물 정의 변경 없음.
- 원본 `build.gradle`, `gradle.properties`, `.github/` 변경 없음. 삭제한 파일 없음. 새 외부 모드·개발 의존성 없음.

### 인게임 테스트 (치트 허용, 백업 월드 권장)
1. 최신 모드 JAR로 접속. **평화로움 이외 난이도**에서 4종 엔티티가 정상 렌더되는지 먼저 확인.
2. 비어 있는 넓은 지역에서 `/place template miggi_exploration:labyrinth_trial ~ ~ ~` 실행. 최소 45×18×45블록 공간을 점유하며 기존 건물을 덮을 수 있음.
3. 시험장 안의 미노타우르스/오크/고블린이 생성되는지 확인. 구조물 로드 성공과 엔티티 스폰 성공은 별개.
4. 별도의 안전한 위치에서 `/place template miggi_exploration:labyrinth_vault ~ ~ ~` 실행. 상자 3개 개봉 및 전리품 테이블 확인.
5. 서버 부팅 테스트 통과는 NBT 엔티티·상자 실제 배치까지 입증하지 않음. 실패 로그 또는 인게임 스크린샷을 보내면 조정 가능.

### 아직 안 된 것 / 확인 필요
- 유적 → 미궁으로 향하는 자동 연결(계단, 입장 상호작용), 자연 생성되는 미궁, 문 봉인/보스 처치 후 해제, 함정 서버 로직, 보스 체력바는 미구현.
- 고대 유적의 `/place structure` 실패 및 자동 배치 문제는 아직 해결했다고 판단하지 않음. 기존 설정을 수정하지 않았음.
- 템플릿 NBT의 엔티티 로드 여부, 상자의 LootTable NBT 동작, 템플릿이 블록을 실제로 파내는 동작을 게임에서 확인해야 함.
- 별도 애니메이션 라이브러리 GeckoLib·월드생성 라이브러리 Lithostitched는 사용자가 설치했지만 **프로젝트 코드와 통합하지 않음**. 추가 의존성 없이 기존 정상 빌드를 유지.

## 26.3 구조물(NBT) 형식 주의
- 블록 팔레트 키가 바뀜: `Name` → `id`, `Properties` → `properties`. 옛 키를 쓰면 `/place`가 성공으로 나와도 **블록이 하나도 안 놓임**(엔티티만 생김).
- `blocks[].pos`는 정수 리스트(`[x, y, z]`)여야 함. IntArray로 쓰면 안 됨.

## v1.0.1 — Claude 검증·수정 (실제 게임 자동 테스트로 확인)
- **자동 인게임 테스트** (`client-test` 작업): 깃허브에서 실제 게임을 띄워 몬스터·애니메이션·구조물·숲 스크린샷과 결과를 `test-results` 브랜치에 올림. 테스트 코드: `client/test/MiggiClientGameTest.java`
- 몬스터 4종 모델 새로 제작 (`tools/models/specs.py` → `MobLayers.java` + 텍스처 자동 생성), 공격 키프레임 애니메이션 (`FantasyModel.java`)
- 오크·미노타우르스 무기 사거리 (`attackReach`): 덩치 때문에 플레이어 앞에서 멈추고 못 때리던 문제 수정
- **구조물이 블록 0개로 생성되던 문제 수정**: NBT 팔레트를 26.3 형식(`id`/`properties`)으로 변환. `arcane_grove` 구조물 설정 수정
- 마법 숲: 바이옴 기능 순서를 바닐라와 맞춤(순서 충돌 경고 제거), `arcane_log`를 진짜 원목(회전·잎 유지)으로, 잎 투명 처리, 블록·아이템 텍스처 교체
- 새 월드 첫 발견 두루마리가 안 뜨던 문제, RPG HUD가 채팅과 겹치던 문제 수정
