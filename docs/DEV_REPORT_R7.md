# GridSpace 格间 · 研发报告 R7 (收尾)

日期 2026-10-13 · SDK 6.1.9 · 模拟器 `Pico_Emulator_6_1_fresh` (与 R5/R6 同一视点) · 依据: 协调者 R7 三项 · R5 的侧卡尺度 / 摆位不变 (颜色栏 / 选区面板 1.5, 笔刷行 1.3, 侧卡 z 0.28 / 8)

截图 `docs/screenshots/r7/zh/` 与 `en/`: ghost · occupied · bar · box · edit · eyedrop · edit-close (`bash docs/scripts/r7.sh zh|en`: 完整 walk 后只保留这 7 张, `r7-run.log` 含全部 cap.sh 记录)。

## 1. 修改

| # | 修改 (文件) | 说明 |
| --- | --- | --- |
| 1 前面平面尺度 1.0 | `EditorScaled.kt`: 新增 `FRONT_PLANE_SCALE = 1.0f`, `LABEL_UI_SCALE` 1.4 → 1.0; `EditorScreen.kt` 取色卡 / 取色条两个 Augment 用 `EditorScaled(FRONT_PLANE_SCALE)`, 锚点 (0.88, 0.42, 0.90) CenterRight 不变; 卡片设计尺寸 560 × 624 不变 | 前面平面 (z ≈ 0.9) 在模拟器视点下 ≈ 1.7 px/Volume-dp, R6 的 1.5 使卡片 ≈ 1240 px 宽、盖住整个模型 |
| 2 目标标签缩小 | `TargetLabel.kt`: 360 × 96, 内边距 24 / 16, 行 1 图标 24 + `label` 20, 行 2 `caption` 16, 行间 6 垂直居中; `LabelAnchor.kt`: 卡半宽 0.1125 / 高 0.06, 锚点夹到 x ∈ [0.20, 0.58] (协调者 [0.14, 0.80], 见 §5), y ∈ [0.08, 0.97] (`X_MIN/X_MAX/Y_MIN/Y_MAX`), 投影规则 (z 0.92, 视者 0.5 深度) 不变; `LabelAnchorTest` 按新夹取改 | 卡片 ≈ 610 × 165 px; x ≤ 0.80 使卡右缘 (0.9125) 留在颜色栏投影区左侧 |
| 3 测量脚本 | `docs/scripts/measure_r4.py`: 各块按帧是否存在跳过 (R5 全套 / R6 / R7 局部都能跑), 新增 目标标签 (ghost / occupied / bar / box 对 edit-close 差分, 行中位数取卡边) 与 取色卡 / 取色条 (edit / eyedrop 对 edit-close 差分) 两组, 标签对颜色栏玻璃左缘的交叠判定 | `python -P docs/scripts/measure_r4.py docs/screenshots/r7 r7` |

## 2. 实测 (2160 px 截图)

`python -P docs/scripts/measure_r4.py docs/screenshots/r7 r7` (zh / en 数值相同):

| 表面 (scale) | 目标 | 设计 dp | R6 实测 px | **R7 实测 px** | px / Volume-dp | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| 目标标签 ghost (1.0, z 0.92) | 卡 | 360 × 96 | ≈ 900 × 260 (440 × 120 × 1.4) | **558 × 158** | 1.55 / 1.65 | 卡右缘 770 < 颜色栏玻璃左缘 1676 |
| 目标标签 occupied | 卡 | 360 × 96 | — | **590 × 156** | 1.64 / 1.62 | 右缘 789 |
| 目标标签 bar | 卡 | 360 × 96 | — | **591 × 158** | 1.64 / 1.65 | 右缘 840 |
| 目标标签 box | 卡 | 360 × 96 | 压在颜色栏中部 | **≈ 590 × 158** (差分 475: 卡左部与白梁同色) | ≈ 1.5 / 1.65 | 右缘 1636 < 1676 (夹取 0.58; 0.60 时 1677 与栏相触 1 px) |
| 取色卡 (1.0, z 0.90) | 卡 | 560 × 624 | ≈ 1240 × 1490 (×1.5) | **888 × 992** | 1.59 | 右缘 2089 在画面内; 占画面 41 % 宽 |
| 取色卡 | 完成钮 高 | 80 | ≈ 190 | **126** (宽 790) | 1.57 | 钮下近黑占比 0.000 |
| 取色条 (1.0, z 0.90) | 取消钮 高 | 72 | ≈ 175 | **113** (宽 138 / 175) | ≈ 1.6 | 右缘 2051 在画面内 |
| 颜色栏 (1.5, z 0.28, 不变) | 色板格 | 88 | 96 × 95 | 96 × 96 | 0.73 | 基线, 与 R5 一致 |

前面平面 (z 0.90–0.92) ≈ 1.55–1.65 px/Volume-dp, 侧卡 (z 0.28) 0.73: 同一设计 dp 在前面平面上已是侧卡的 2.2 倍, 因此前面平面的卡不再放大 (`FRONT_PLANE_SCALE` / `LABEL_UI_SCALE` = 1.0)。标签文字 `label` 20 sp × 1.6 ≈ 32 px 字高级别, 与侧卡 20 sp × 1.5 × 0.73 ≈ 22 px 相当。

## 3. 截图逐张

截图省略前缀 `docs/screenshots/r7/`; zh / en 同名对应 (box 两张在夹取改为 0.20..0.60 与 0.58 后各用 `gridspace_demo box` 脚本重拍两次, 命令与 cap.sh 记录追加在各自 `r7-run.log`)。

| 帧 | 观察 |
| --- | --- |
| `r7-ghost.png` | "✓ 已对齐 · 松手放置 / X1 · Y2 · Z2" ("✓ Aligned · release to place") 558 × 158 px, 在幽灵 (1,2,2) 上方偏左, 文字完整, 卡在前面 (z 0.92) 不被几何体挡, 幽灵与落点可见; 不碰颜色栏 (卡右缘 770 < 栏左缘 1676) |
| `r7-occupied.png` | "❗ 格已占用 / 换一个格子" ("Cell occupied / Try another cell") 590 × 156 px 在 (1,0,0) 上方, 珊瑚线框可见 |
| `r7-bar.png` | "1 × 4 × 1 / 松手放置" ("1 × 4 × 1 / Release to place") 591 × 158 px 在长条幽灵上方, 长条与落点可见 (R6 的原缺陷: `r6/en/r6-bar.png` 曾为空白卡; R7 前 R6 版卡 ≈ 900 px 宽) |
| `r7-box.png` | "5 × 1 × 1 / 松手放置" 在盒幽灵 (8..12,0,14) 上方; 第一版 (夹取 0.80) 卡落在 1797..2100 px 压住颜色栏中部 → 夹取改 0.60 后重拍, 卡右缘 1636 px < 栏左缘 1676 (0.60 时 1677, 相触 1 px → 0.58) |
| `r7-edit.png` | 取色卡 888 × 992 px (1.59 px/dp, R6 为 ≈ 1240 px 宽盖住整个模型), 右缘 2089 px 在画面内, 预览圆 / #FF3B30 / 从方块取色 / 色相 饱和 明度 (Hue Sat. Light) / 完成 全部可见; 卡在模型中右部前方, 模型左半与底座可见 |
| `r7-eyedrop.png` | 取色条 "点一个方块取色 · 取消" / "Tap a cube to pick its colour · Cancel" 在右上, 颜色栏变暗 |
| `r7-edit-close.png` | 卡片关闭, 颜色栏复原 (鲜艳页, 槽 1 为取到的珊瑚) |

## 4. 构建 / 测试

```
./gradlew :app:assembleDebug --offline -q          → EXIT 0
./gradlew :app:testDebugUnitTest --offline -q      → EXIT 0, 单测 60/60 (LabelAnchorTest 两次随夹取改: 投影用例改用 x 0.55 以免被 0.58 夹掉)
dex: SpatialApplication refs across all dex: 3 (exit 0)
r7.sh zh / en: walk 27 标记到 done, 保留 7 张 / 语言; logcat 无 AndroidRuntime / FATAL; settings list global | grep gridspace 为空
```

## 5. 备注

| 条目 | 说明 |
| --- | --- |
| 标签锚点 x 夹取 [0.14, 0.80] → **[0.20, 0.58]** | 前面平面在本视点下投影为 x_px ≈ 1092 + (x − 0.5) × 2560 (标签实测 1.55–1.65 px/Volume-dp); 0.80 时盒标签卡 (半宽 0.1125) 投影到 1797..2100 px, 正压在颜色栏 (玻璃左缘 1676) 中部 (第一版 `r7-box.png`, 两语言相同); 0.60 时卡右缘 1677 仍与栏相触 1 px, 0.58 → 1636 px (余 40 px); 下限 0.20 使卡左缘不出画 (0.14 时 −118 px)。y 夹取 [0.08, 0.97] 按协调者数值 |
| `measure_r4.py` 的 box 参考帧 | box 与 edit-close 不在同一色板页 (Soft / Vivid), 差分会把颜色栏算进去, 改用同页的 occupied 帧作参考; 取色条无"无卡"参考帧 (颜色栏变暗), 改量其取消钮 (72 dp) |
| 取色卡仍在模型前方 | 协调者 (1, 0.5, 0.95) CenterLeft 的"栏旁"位置在本视点出画 (R6 §4), 保持 (0.88, 0.42, 0.90) CenterRight; 1.0 倍后卡只盖模型中右部 (888 px 宽, 画面 41 %) |

## 附: QA R7 N7-01 修复 (orchestrator, 2026-10-13)

- 缺陷: `Regex.escape(...).replace("0", "(\d+)")` 把捕获组放进了 `\Q…\E`, 任何名字都不匹配, 所有新项目都叫 "未命名 1 / Untitled 1"。
- 修复: `ui/theme/Localization.kt` 新增 `Context.untitledPattern()` (按 `%1$d` 切分模板后逐段转义, 并把所有语言的模板用 `|` 连接, 中英文编号连续) 与纯函数 `untitledTemplateRegex()`; `data/ProjectRepository.kt` 新增 `Regex.untitledNumber()` 取首个非空捕获组; `EntryScreen.kt` / `EditorScreen.kt` 改用 `context.untitledPattern()`。
- 验证: `assembleDebug + testDebugUnitTest --offline` EXIT 0, 单测 **64/64** (新增 `UntitledPatternTest` 4 项), dex refs 3; 模拟器 `gridspace_demo newproject` 后 `files/projects/index.json` 最新项目为 "未命名 2" (此前索引里已有 "Untitled 1" / "未命名 1"), logcat 无 AndroidRuntime。
