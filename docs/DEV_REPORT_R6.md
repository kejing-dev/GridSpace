# GridSpace 格间 · 研发报告 R6 (轻量)

日期 2026-10-13 · SDK 6.1.9 · 包名 `com.kejing.gridspace` · 模拟器 `Pico_Emulator_6_1_fresh` (emulator-5554, 与 R5 同一视点, 未重启) · 依据: 协调者 R6 三项 (基于 R5 截图) · R5 的全部尺度 / 摆位设置不变 (`EditorScaled` 1.5 / 1.4 / 1.3, 侧卡 z 0.28 / 8)

截图 `docs/screenshots/r6/zh/` 与 `en/`: 只重拍受影响的步骤 workbench · ghost · occupied · bar · box · dragmove · dragmove-commit · edit · eyedrop · edit-close (`bash docs/scripts/r6.sh zh|en` 跑完整 walk 后只保留这 10 张, `r6-run.log` 保留全部 cap.sh 记录); 摆位探针 `docs/screenshots/r6/probe/`。

## 1. 逐项: 修复 → 证据

| # | 修复 (文件:行) | 证据 |
| --- | --- | --- |
| **1 拖动移动无浮动标签** | `EditorScreen.kt`: `labelCell` 对 `Preview.MoveSelection` 为 null, `TargetLabelAugment` 对它不建卡; `moveTexts()` 组合现有字串 `label_move_delta` + " · " + `label_move_hint / label_move_release / label_move_overwrite` 成一行 `liveHint` 传入 `SelectionPanelState` (`SelectionPanel.kt` 新字段), 面板末行固定一行 `caption` 16 (`maxLines 1`, 省略号, `fillMaxWidth`), 拖动中 `labelPrimary`, 结束后回到 `selection_drag_hint` `labelSecondary`; 步进值仍显示实时 Δ (`translation + dragDelta`, R4 不变) | `zh/r6-dragmove.png` 面板末行 "ΔX +2 · ΔY 0 · ΔZ −1 · 松手移动 5 块", 无浮动卡; `en/` "ΔX +2 · ΔY 0 · ΔZ −1 · Release to move 5 cubes"; `r6-dragmove-commit.png` 末行回到 "拖动已选方块可整体移动" / "Drag a selected cube to move all" |
| **2 目标标签在目标上方、前面** | 新 `ui/editor/components/LabelAnchor.kt` `place(centre, cellN)`: z = **0.92**; 锚点 = 格顶 + 1.5 格 (格心 − 2 格) 的 x/y 按"视者在前面 1.0 个 Volume 深度"投影到 z 0.92 平面 (偏离中心量 × (1.0 + 0.08) / (1.0 + 1 − z格)); 再夹到 x ∈ [0.03 + 0.1925, 0.97 − 0.1925], y ∈ [0.03 + 0.105, 0.97] (卡 616 × 168 Volume-dp 的半宽 / 高); `VoxelSceneView.kt` 只算 `LabelPlacement(anchor)`, `EditorScreen.kt` `TargetLabelAugment` 用 `BottomCenter` 零偏移向上长; 旧的翻边 / 侧移逻辑 (`labelCardOnRight`, `LABEL_UP/SIDE/FORWARD_CELLS`) 删除; 单测 `LabelAnchorTest` 3 例 (正前方、深处收向中心、夹取) | `zh/r6-ghost.png` / `occupied` / `bar` / `box` 与 `en/` 同名: 文字完整、卡在目标上方且不被几何体挡、目标格可见 (§2 逐张说明) |
| **3a 取色卡裁切** | 原因: `SpatialPopup(defaultMinWidth 560, defaultMinHeight 624)` 按未缩放 dp 建窗, 内容 ×1.5 (840 × 936) 被窗裁掉左侧 (预览圆、色相/饱和/明度标签)。R6 改为 Augment (3b), 窗口按内容 (`ColorEditCard` 固定 560 × 624 设计 dp 在 `EditorScaled(1.5)` 内) 建立, 不再有 `defaultMin*` | `zh/r6-edit.png` / `en/r6-edit.png`: 预览圆完整, 色相 / 饱和 / 明度 (Hue / Sat. / Light.) 三个标签可见 |
| **3b 取色卡深度** | javap (design-6.1.9 / foundation-6.1.9): `SpatialPopup(onDismissRequest, modifier, popupPositionProvider, cornerRadius, defaultMinWidth, defaultMinHeight, …)` 三个 float 为圆角 / 最小宽 / 最小高, `rememberSpatialPopupPositionProvider(HorizontalPlacement, VerticalPlacement)` 只有二维位置, `SpatialPopupDefaults` 只有 width / height / cornerRadius, foundation `Modifier.depth()` 是内容布局深度而非窗口 z → **无深度参数, 走 Augment 路线**: `ColorEditPopup.kt` 改为 `ColorEditCard(State<ColorEditState>)` + `EyedropperBar`, `EditorScreen.kt` 两个 Augment (编辑卡 / 取色条, 互斥), `ColorEditPlacement` = **(0.88, 0.42, 0.90) `CenterRight` 零偏移** — 不是协调者给的 (1, 0.5, 0.95) CenterLeft +8, 原因见 §4 (`gridspace_popup` 可覆盖; 新增 `gridspace_demo edit` 脚本直接打开卡片做探针); `ColorRail` 不再承载 Popup; `onDismissRequest` (点外关闭) 不再有, 关闭靠 ✕ / 完成 | `zh/r6-edit.png` / `en/`: 卡片在颜色栏前方 (z 0.95), 珊瑚长条不再穿过完成钮; `r6-eyedrop.png` 取色条; `r6-edit-close.png` 关闭后颜色栏复原 |

## 2. 截图逐张

截图省略前缀 `docs/screenshots/r6/`; `zh/` 与 `en/` 同名文件内容对应 (标签 / 面板帧来自整套 walk, edit / eyedrop / edit-close 三帧在卡片 x 0.92 → 0.88 后用 `gridspace_demo edit` 脚本重拍, 命令与 cap.sh 记录追加在各自 `r6-run.log` 末尾)。

| 帧 | 观察 (zh / en 相同, 除文字) |
| --- | --- |
| `r6-ghost.png` | 卡 "✓ 已对齐 · 松手放置 / X1 · Y2 · Z2" ("✓ Aligned · release to place") 在画面左上, 文字完整, 卡在前面平面 (z 0.92) 不被梁柱挡; 薄荷幽灵 (1,2,2) 与底座落点在卡下方可见。R5 同帧卡片左缘出画 (`r5/zh/r5-ghost.png`) |
| `r6-occupied.png` | "❗ 格已占用 / 换一个格子" ("Cell occupied / Pick another cell") 在目标 (1,0,0) 上方, 珊瑚线框可见 |
| `r6-bar.png` | "1 × 4 × 1 / 松手放置" ("1 × 4 × 1 / Release to place") 完整 (R6 第 2 项的原缺陷: `r5/en/r5-bar.png` 为空白卡), 卡底边贴着长条幽灵顶端, 长条与落点可见 |
| `r6-box.png` | "5 × 1 × 1 / 松手放置" 完整; 目标行 (8..12,0,14) 在右前方, 锚点被夹到 x ≤ 0.7775, 卡在前面平面上投影到颜色栏左半的前方 (卡在前, 栏在后; 新规则只要求卡在前面之内, 不再要求与颜色栏投影无交集) |
| `r6-dragmove.png` | 无浮动卡; 面板末行 "ΔX +2 · ΔY 0 · ΔZ −1 · 松手移动 5 块" / "ΔX +2 · ΔY 0 · ΔZ −1 · Release to move 5 cubes" 单行完整 (无省略号), 步进值 2 / 0 / −1; 变暗原块 + 白落点 + 薄荷幽灵不变 |
| `r6-dragmove-commit.png` | 末行回到 "拖动已选方块可整体移动" / "Drag a selected cube to move all", 5 块在新位置带白框 |
| `r6-edit.png` | 卡片在模型右半前方 (z 0.90), 预览圆完整, 色相 / 饱和 / 明度 (Hue / Sat. / Light) 三个标签与三条滑杆、从方块取色、完成 全部可见, 右缘在画面内; 珊瑚长条不再穿过完成钮 (R5 `r5/en/r5-edit.png` 左侧裁切 + 长条穿卡) |
| `r6-eyedrop.png` | 取色条 "点一个方块取色 · 取消" / "Tap a cube to pick its colour · Cancel" 在同一锚点 (右上), 颜色栏变暗 |
| `r6-edit-close.png` | 卡片关闭, 颜色栏复原 |
| `r6-workbench.png` | 与 R5 相同 (颜色栏 / 笔刷行 / 模型), 作为本轮基线 |

第一次 R6 整套跑 (x 0.92) 的 zh `eyedrop` 帧拍到了取色之后的状态 (walk 的 20 s ack 超时被 cap.sh 的 5 次重试耗尽), 所以 `DemoMode.ACK_TIMEOUT_MS` 20 → 40 s, 三帧按上面说明重拍; en 的三帧又重拍两次 (第二次: `edit` 脚本改为启动后等 30 s 再开卡, `EDIT_SCRIPT_START_MS`, 否则冷启动的笔刷行在帧里还是半黑; 第三次: 第二次的 edit 帧里卡片下半 (明度 / 完成) 还没绘出 — 新建窗口的内容纹理滞后, AGENTS 已知), 三次重拍命令都追加在 `en/r6-run.log`, 现存三帧完整。edit 脚本不经过 walk 的取色步, 所以 `r6-edit-close.png` 槽 1 仍是样例的 #F3BC9C。

## 3. 构建 / 测试 / 静态

```
./gradlew :app:assembleDebug --offline -q          → EXIT 0
./gradlew :app:testDebugUnitTest --offline -q      → EXIT 0, 单测 60/60 (R5 61 − LabelSide 4 + LabelAnchor 3; 0 失败 0 跳过)
dex: SpatialApplication refs across all dex: 3 (exit 0)
r6.sh zh / en: walk 27 标记到 done, 保留 10 张 / 语言 (edit / eyedrop / edit-close 由 edit 脚本重拍, 见 §2); logcat 无 AndroidRuntime / FATAL; settings list global | grep gridspace 为空
单测首次 60 tests / 1 failed: LabelAnchorTest.cardStaysInsideTheFace 的夹取用例用了深处格 (VIEWER_DISTANCE 0.5 下 x 0.01 投影到 0.297, 未触发夹取), 改为前面平面的角格后 60/60
```

## 4. 偏离 / 备注

| 条目 | 说明 |
| --- | --- |
| 取色卡摆位 (1, 0.5, 0.95) CenterLeft +8 → **(0.88, 0.42, 0.90) CenterRight** | 探针 `probe/p-left-edit.png` (规格摆位): 卡片整张在画面右侧之外; `p-right-edit.png` (同锚点 CenterRight): 卡片左缘 1047 px、预览圆 227 px (≈ 1.7 px/Volume-dp), 右面 x = 1 投影在 ≈ 2440 px, 右侧 ≈ 280 px 被裁 (从方块取色 / 滑杆右端 / 完成钮)。用颜色栏 (z 0.28 → 0.73 px/dp) 与笔刷行 (z 0.75 → 1.2) 拟合透视 px/dp ∝ 1/(D + 1 − z) 得视者距前面只有 D ≈ 0.48 个 Volume 深度, 所以前面平面上 840 Volume-dp 的卡片在本视点必然出画。取 z 0.90 (仍在全部方块之前: 样例旋转后最近角 z ≈ 0.86)、y 0.42 (卡片 y 0.13..0.71, 底座在 0.725 之下不穿卡)、x 0.88 CenterRight (画面内最靠右: 0.94 / 0.92 时卡右缘在 ≈ 2134 / 2186 px, `probe/p2-edit.png` 与第一次 R6 跑的 edit 帧) — 卡片位于模型右半前方而非颜色栏旁; 真机视场更宽时可用 `gridspace_popup 1,0.5,0.95,8,0,0,1` 复核规格摆位 |
| `LabelAnchor.VIEWER_DISTANCE` = 0.5 | 协调者未给数值; 按上面的拟合取 0.5 (模拟器), 真机若更远则标签略偏外, 夹取保证不出面 |
| 取色卡失去"点外关闭" | Augment 没有 `onDismissRequest`; 关闭靠 ✕ / 完成 |
| 面板末行长度 | en "ΔX +2 · ΔY 0 · ΔZ −1 · Release to move 5 cubes" 在 352 设计 dp 内以 `caption` 16 单行显示, 超长时省略号 (见 §2 实拍是否省略) |

## 5. 仅真机可验 [H]

- `LabelAnchor.VIEWER_DISTANCE` = 1.0 个 Volume 深度是假定值: 真机默认放置距离不同时, 深处格子的标签会偏离目标上方 (近则偏外, 远则偏内); 可按真机截图调该常数。
- 取色卡在 Volume 右面前方 (z 0.95) 在真机视场内的可见性 (`gridspace_popup`)。
- 取色卡失去"点击外部关闭": 真机上只能用 ✕ / 完成。
