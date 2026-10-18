# GridSpace 格间 · 研发报告 R11 (坐标轴标注)

日期 2026-10-17 · 依据: 用户反馈 (宽 X / 高 Y / 深 Z 与 ΔX·ΔY·ΔZ 需要底座上的方向参照; 二次反馈: 竖直 Y 杆与字母挡住模型, 只保留 X / Z) → 协调者 R11 · 规格 `docs/DESIGN_SPEC_R4.md` **§T.9** (本轮追加)

## 1. 修改

| 项 | 文件 | 内容 |
| --- | --- | --- |
| 轴杆几何 | 新 `ui/editor/components/AxisGuides.kt` (纯数据) | 以**边中点为中心**: +X 杆沿近边 (模型 z = +N/2 + 0.35) x ∈ ±4.7; +Z 杆沿左边 (x = −N/2 − 0.35) z ∈ ±4.7; **无 Y 杆** (二次反馈后删除, `guides()` 只返回 X / Z); 粗 0.08, 浮于底座顶面上 0.04; 箭头 = + 端三个递缩方块 0.35 / 0.25 / 0.15; 字母位 = 尖端 + 0.5 格; 一切在中心半径 10.4 格内 |
| 场景 | `SceneParts.axisGuides` (一个实例化 `ModelEntity`, 共享单位盒, 无碰撞体), `VoxelScene.rebuildPlate` 随底座重建; 颜色 `GridColors.AxisGuide` #AAB9B8 **不透明** (`AXIS_ALPHA` 1.0) | 所有模式常驻, 不可交互; ANR 规则不变 (无新网格) |
| 字母 X / Z | `AxisChip.kt` + `EditorScreen.kt` (两个 `Augment`, `AugmentContentAlignment.Center`, 前面平面 scale 1.0) + `LabelAnchor.projectToFront` + `VoxelSceneView.onAnchors` 第三个参数 | **Augment 备选路径**: 56 × 56 设计 dp 轴色圆片 + `label` 20 M `BaseInk` 字母, 锚点 = 尖端点投影到 z 0.92 面 (与目标标签同一视者规则), 随 yaw / pitch 每帧重算; 字串复用 `axis_x` / `axis_z`; 内容固定, 只有锚点变 |
| SDK 路径 (已验证、未采用) | — | javap: foundation `attachmentPanelComponent(context, size, alignment, content)` + `panelSize(dp, dp)`, core `AttachmentPanelComponent` / `LookAtComponent` (`setViewerAsTarget()`, `alignLocalUpToWorldUp`, `lookAtForwardDirection`) 都存在。实做 (合成期建 3 个组件并 `remember`, 挂在 `root` 下带显式 `TransformComponent` 的尖端实体上, 每帧按 `localToView` 定位): logcat 出现 3 个 112 × 112 px 附着窗 (`isViewAttachment=true`, 位置日志正确), 画面里只在 +Y 尖端看到一块**空白浅色方片**, Compose 内容 (圆片 + 字母) 不渲染 (`probe/p7-crop.png`); 换 `NEGATIVE_Z` 朝向无改善 → 按协调者备选改 Augment |
| 单测 | `AxisGuidesTest` 5 例 | 杆在底座外且浮于顶面 (LIFT); 以边中点为中心 + 尖端 / 字母位; 箭头递缩; 全部在 Volume 半径内; 无 Y 导线 |
| 规格 / 备忘 | `DESIGN_SPEC_R4.md` §T.9, `AGENTS.md` | 几何表 (含前提), 颜色, 字母方法与 SDK 路径结论 |

## 2. 截图 (`docs/screenshots/r11/{zh,en}/r11-{workbench,size}.png`, `panels` 脚本经 `r11.sh`, `r11-run.log`; 带 Y 杆的上一版帧保留在 `r11/with-y/{zh,en}/`)

__SHOTS__

## 3. 构建 / 测试

```
./gradlew :app:assembleDebug --offline -q          → EXIT 0
./gradlew :app:testDebugUnitTest --offline -q      → EXIT 0, 单测 68/68 (R10 63 + AxisGuides 5)
dex: SpatialApplication refs across all dex: 3 (exit 0)
settings list global | grep gridspace 为空; 结束时重新启动 SkyboxSpatial 与 GridSpace
```

## 4. 备注

| 条目 | 说明 |
| --- | --- |
| 杆不沿整边, 以边中点为中心 (±4.7 格) | 第一版按协调者几何 (整边 + 0.6 越出 + 角上 Y 杆) 什么都看不见: 底座 1200 dp 的角到中心 848 dp, 超出 1600 dp Volume 的 800 dp 半宽, 角、边端、箭头 (940 dp) 与字母在任意 yaw 下都被 Volume 裁掉 (§T.3 已注底座近角被裁); 画面里沿左边那条淡线是 Volume 自己的边 (R9/R10 帧也有)。半径 ≤ 10.4 格 (780 dp) 后任意 yaw 可见 |
| "近边" = 模型 z = +N/2 | 协调者写 z = −0.35 (格坐标 z = 0 侧); 本应用默认视点 (yaw 0.60) 下 z = 0 边在模型后面, +Y 杆与 +X 杆被亭子挡住 (`probe/p6-crop.png` 只见 +Z 杆); 改到 +z 边后三根杆都在前面 |
| 颜色不透明 + 浮起 0.04 | 85 % 的 0.08 格杆 (≈ 5 px) 贴在底座玻璃上几乎不可辨 (`probe/p4`); 不透明并浮起后清晰 (`probe/p6`, `p7`) |
| 字母用 Augment 而非附着面板 | 见 §1 "SDK 路径" |
| 无旋转视角帧 | 演示脚本无法转动相机 (yaw / pitch 是视图内部状态, 没有事件), 协调者允许跳过; 字母随 yaw / pitch 重算锚点的逻辑与目标标签相同 (R6–R8 已验证) |
| 模拟器 | 本轮 `pico-cli emulator stop` 没有停掉用户的 `emulator-5554`, `start` 另起了一个实例 `emulator-5556` (无 SkyboxSpatial, 只有 gridspace / neonlong / conjure): 之后 adb 对两台设备报 "more than one device", 第一次 en 拍摄失败。处理: 全部截图工具用 `ANDROID_SERIAL=emulator-5556` 钉住 (adb / cap.sh / r11.sh 直接生效; `walk.sh` 的 `pico-cli app launch` 加 `-d $ANDROID_SERIAL`), 在 5556 上重拍 zh / en; 结束时把最终 APK 装到用户的 5554、在 5554 上重新启动 SkyboxSpatial 与 GridSpace、两台都清掉 `gridspace_*`, 然后 `adb -s emulator-5556 emu kill` 结束我起的实例, adb 回到单设备。另外用户在拍摄期间打开的第三方应用 (`com.technomancer.neonlong` 飞龙、`com.uaybalam.conjure` 飞碟生成器) 的窗口曾挡在亭子前 (`r11/with-y/zh-foreign-window/`, `probe/p1-crop.png`), 拍摄前一并 force-stop (未重新启动) |
