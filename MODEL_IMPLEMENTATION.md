# 26.3 Fabric: 독립 3D 모델 시도 빌드

## 무엇을 추가했나
- `FantasyMobModel.java`: 4종 자체 큐브 메시를 각각 선언. 뿔, 엄니, 무기, 머리, 팔/다리 모델 부품 포함.
- `FantasyMobRenderer.java`: 기존 좀비/호글린/파괴수의 바닐라 외형을 사용하지 않는 엔티티 렌더러.
- `FantasyRenderState.java`: 26.x의 EntityRenderState 기반 렌더링.
- `MonsterRenderers.java`: Fabric ModelLayerRegistry 및 EntityRendererRegistry 등록.
- `textures/entity/{goblin,orc,wild_boar,minotaur}.png`: 각 256×256 픽셀 텍스처 아틀라스.

## 테스트 (실제 게임의 치트 허용 필요)
1. GitHub Actions 빌드 성공을 확인하고 JAR 설치.
2. `/summon miggi_exploration:goblin ~ ~ ~`
3. `/summon miggi_exploration:orc ~ ~ ~`
4. `/summon miggi_exploration:wild_boar ~ ~ ~`
5. `/summon miggi_exploration:minotaur ~ ~ ~`

## 현재 한계 (중요)
- 이 런타임에는 Java 25/Fabric 26.3 게임 클라이언트가 없어 빌드와 렌더링을 실제 실행 검증하지 못함.
- 큐브 파트는 개별 root children이고, 현재 움직임은 기본 걷기 모션. 공격 타격 타이밍에 대응한 도끼 애니메이션/보스 패턴은 미구현.
- 텍스처는 기초 팔레트 및 노이즈 패턴만 있고 디테일 페인팅 필요.
- 일부 독립 파트는 애니메이션시 서로 떨어져 보일 수 있음. 게임에서 확인하여 축/부모 계층 재정렬 필요.
- 오크는 좀비 AI 상속, 멧돼지는 호글린 AI 상속, 미노타우르스는 파괴수 AI 상속. 전용 공격 AI/자연 스폰/미궁 소환/전리품 미구현.
- 모델 크기와 히트박스 일치 여부도 게임에서 시각 테스트 필요.

## 모델 튜닝 권장
- Blockbench Java Entity 프로젝트로 다시 조정하거나 이 파일의 `cube(...)` 지오메트리를 수정.
- 모델 및 텍스처는 프로젝트 신규 제작 원본. 외부 모드 에셋 미포함.
- 참고: https://docs.fabricmc.net/develop/entities/first-entity (26.2 예시, 26.3 빌드 검증 필요)
