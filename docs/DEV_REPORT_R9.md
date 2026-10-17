# GridSpace 格间 · 研发报告 R9 (小改)

日期 2026-10-17 · 依据: 用户对 `r8-size.png` 的反馈 (字母、数字、按钮连成一串) → 协调者 R9 · 规格 `docs/DESIGN_SPEC_R4.md` **§T.7** (本轮追加)

## 1. 修改

| 项 | 文件 | 内容 |
| --- | --- | --- |
| 笔刷行三组分隔 | `ui/editor/components/BrushRow.kt` | 每轴一组 300 × 72: 轴标 `label` 20 M 固定宽 88 起始对齐 · 12 · `IconButton` − 72 · 值 `label` 20 M 固定宽 56 居中 · `IconButton` + 72; 组间 24 + `VerticalDivider` 1 × 48 + 24; 行左右内边距 32 → **1062 × 80** 设计 dp (32 + 3 × 300 + 2 × 49 + 32), scale 1.0、anchor (0.5, 1, 0.97) / offset −320 不变; − 在 1 禁用, + 在 min(16, N) 禁用 (系统 alpha), 值 `labelPrimary`; 内容固定尺寸, 按钮 hover (SDK `IconButton`) |
| 字串 | `values` / `values-zh` | 新增 `size_axis_x/y/z` = "W · X" / "H · Y" / "D · Z" 与 "宽 X" / "高 Y" / "深 Z"; 删除 `size_times`; 两语言 102 / 102 键一致 |
| 规格 | `docs/DESIGN_SPEC_R4.md` §T.7 | 一张表, 取代 §T.1 的行布局与总宽 |
| 演示 | `DemoMode.kt` `size` 脚本 | 先标记 `workbench` (1 × 1 × 1) 再 3 × 2 × 1 预览标记 `size`, 一次运行出两帧 |

## 2. 截图 (`docs/screenshots/r9/{zh,en}/r9-{workbench,size}.png`, `gridspace_demo size` 脚本, 记录在各自 `r9-run.log`)

第一次尝试 (`r9/{zh,en}-run1/`): 用户此时正在用模拟器 (SkyboxSpatial 与 GridSpace 都开着), 视点已漂移 (模型更大更低, 行下缘出画) 且压缩器处于黑块期 (8 次重试均 > 0.5); 按 AGENTS 重启模拟器 (`pico-cli emulator stop / start`), 等 90 s, 重拍即干净 (每帧第 1–2 次即可)。

| 帧 | 观察 (zh / en) |
| --- | --- |
| `r9-workbench.png` | 行 `宽 X [−] 1 [+] │ 高 Y [−] 1 [+] │ 深 Z [−] 1 [+]` / `W · X … │ H · Y … │ D · Z …`: 三组由竖分隔线隔开, 轴标在组首、值在两钮之间, 肉眼可分; − 钮禁用态 (值 1), + 钮实色; 整行在底座近边前方 |
| `r9-size.png` | 值 3 / 2 / 1, − 钮启用; (1,0,1) 起 3 × 2 × 1 薄荷块 + 落点 + 标签 "3 × 2 × 1 / 松手放置" ("3 × 2 × 1 / Release to place") 在块上方 |
| 实测 | 六个步进钮圆 x 395..2050 px → **1.84 px/Volume-dp**; 行玻璃约 x 152..2109 px (宽 ≈ 1957 px), 左右余 152 / 51 px, 钮带 y 1940..2096 px (画面 2158); 无裁切, 保持 1062 宽 (协调者的 80 / 20 备选未启用) |

## 3. 构建 / 测试

```
./gradlew :app:assembleDebug --offline -q          → EXIT 0
./gradlew :app:testDebugUnitTest --offline -q      → EXIT 0, 单测 60/60
dex: SpatialApplication refs across all dex: 3 (exit 0)
settings list global | grep gridspace 为空; 结束时按要求重新启动 SkyboxSpatial 与 GridSpace
```

## 4. 备注

| 条目 | 说明 |
| --- | --- |
| 行宽 | 1062 设计 dp 在本视点 ≈ 1957 px, 画面内余 ≥ 51 px; 若真机 / 另一视点裁切, 按 §T.7 改轴标 80、间距 20 (1022 dp) |
| 模拟器 | 第一次拍摄遇视点漂移 + 黑块期 (见 §2), 重启后恢复; 结束时已重新启动 SkyboxSpatial 与 GridSpace (`ps` 两进程在) |
| 测量脚本 | `measure_r4.py` 的 r8 笔刷行测量 (深色圆钮游程) 对 r9 仍适用, 未改 |
