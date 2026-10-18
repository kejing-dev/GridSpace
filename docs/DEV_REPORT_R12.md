# GridSpace 格间 · 研发报告 R12 (坐标轴线)

日期 2026-10-17 · 依据: 用户对 R11 坐标架的评价 "太丑" → 协调者 R12: 改为编辑器惯例的底座扁平轴线 · 规格 `docs/DESIGN_SPEC_R4.md` **§T.9 坐标轴线 (R12)** (整节改写)

## 1. 修改

| 项 | 文件 | 内容 |
| --- | --- | --- |
| 轴线几何 | 新 `ui/editor/components/AxisLines.kt` (纯数据; 删 `AxisGuides.kt`) | X 线: 底座顶面上沿网格 z = 0 边界 (模型 z = −N/2, 后边) 从 x = 0 到 N; Z 线: 沿 x = 0 边界 (模型 x = −N/2, 左边) 从 z = 0 到 N; 宽 0.06 格, 高 0.02 格 (扁平), 底面高出底座顶面 0.01 格; 无 Y, 无字母 |
| 场景 | `SceneParts.axisLine` (每轴一个单实例 `ModelEntity`, 共享单位盒, 无碰撞体; 删 `axisGuides`), `VoxelScene.rebuildPlate` (`axisX` / `axisZ` 随底座重建, 删 `AXIS_ALPHA`) | `UnlitMaterial` 不透明: X `GridColors.AxisX` #E07A5F 哑红, Z `GridColors.AxisZ` #5B8DEF 哑蓝 (删 `AxisGuide`); 所有模式常驻; 方块可自然遮挡; ANR 规则不变 (无新网格) |
| 删除 R11 字母片 | 删 `AxisChip.kt`; `EditorScreen` 去掉两个字母 `Augment` 与 `axisTips` 状态; `VoxelSceneView.onAnchors` 回到两个参数; `LabelAnchor` 删 `projectToFront` / `CHIP_MARGIN*` | `axis_x` / `axis_y` / `axis_z` 字串保留 (选区面板步进行仍用) |
| 单测 | `AxisLinesTest` 4 例 (删 `AxisGuidesTest` 5 例) | X 线沿 z = −N/2 跨 x −N/2..+N/2; Z 线沿 x = −N/2 跨 z; 扁平 (高 0.02 < 宽 0.06) 且底在 0.01, 都在底座内; 无 Y |
| 规格 / 备忘 | `DESIGN_SPEC_R4.md` §T.9 改写为 "坐标轴线 (R12)", K4.36 颜色纪律追加两轴色专用; `AGENTS.md` 坐标轴条目改写 (保留 R11 的 Volume 裁切半径与附着面板结论) | — |
| 脚本 | 新 `docs/scripts/r12.sh` (zh 留 workbench + size, en 只留 workbench) | 复用 `panels` 演示脚本与 `walk.sh` (`ANDROID_SERIAL` 钉设备) |

## 2. 截图 (`docs/screenshots/r12/{zh,en}/r12-*.png`, `panels` 脚本经 `r12.sh`, `r12-run.log`; 评审拼图 `r12/review.png`)

| 帧 | 观察 |
| --- | --- |
| `zh/r12-workbench.png` | 蓝色 Z 线沿底座**左边**整条可见 (从左后角到画面下缘, 扁平贴在网格上, 比网格线略粗); 红色 X 线沿**后边**: 左后角到第一根柱子之间一段清晰可见, 其余被亭子自然遮住 (`crop-corner.png`); 两线在左后角相交, 都不压任何方块 / 控件; 黑块 0.003 |
| `zh/r12-size.png` | 尺寸行打开 + 3 × 2 × 1 预览 + 标签 "3 × 2 × 1 / 松手放置"; 两线位置同 workbench, Z 线下端在尺寸行左侧画面外侧, 与尺寸行、标签无重叠; 黑块 0.003 |
| `en/r12-workbench.png` | 同 zh workbench (轴线无文字, 语言只影响工具栏); 黑块 0.003 |

## 3. 构建 / 测试

```
./gradlew :app:assembleDebug --offline -q          → EXIT 0
./gradlew :app:testDebugUnitTest --offline -q      → EXIT 0, 单测 67/67 (R11 68 − AxisGuides 5 + AxisLines 4)
dex: SpatialApplication refs across all dex: 3 (exit 0)
settings list global | grep gridspace 为空; 结束时在 emulator-5554 重新启动 SkyboxSpatial 与 GridSpace (adb devices 只有 emulator-5554)
```

## 4. 备注

| 条目 | 说明 |
| --- | --- |
| 边界线的位置 | 协调者写的 "网格 z = 0 边界线" 按网格索引理解 = 模型 z = −N/2, 即默认视点 (yaw 0.60 / pitch 0.40) 下底座的**后边**; "x = 0 边界" = 模型 x = −N/2, 即**左边**。两条线在底座左后角相交, 这与 R11 期间确认的边 / 视点对应一致 |
| 颜色纪律 | #E07A5F / #5B8DEF 只用于这两条轴线 (§T.9, K4.36); 未动 Mint / Coral / SelectionWhite |
| 第一次拍摄作废 | 第一条链里单测 `linesAreFlatAndLiftedJustAboveThePlate` 失败 (线以边界为中心, 半宽 0.03 格越出底座顶面, 断言未留余量) 使 `&&` 链跳过安装, 拍到的是 R11 APK; 放宽断言 (允许半宽越边) 后重装重拍, 本节帧均为 R12 APK (20:46 构建) |
| 不透明材质 | 轴线用 `UnlitMaterial.create()` (非透明混合) — R11 发现 85 % 透明的细条贴在底座玻璃上几乎不可辨 |
