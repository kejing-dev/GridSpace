# GridSpace 格间 · 研发报告 R11 (坐标轴标注)

日期 2026-10-17 · 依据: 用户反馈 (宽 X / 高 Y / 深 Z 与 ΔX·ΔY·ΔZ 需要底座上的方向参照; 二次反馈: 竖直 Y 杆与字母挡住模型, 只保留 X / Z) → 协调者 R11 · 规格 `docs/DESIGN_SPEC_R4.md` **§T.9** (本轮追加)

## 1. 修改

| 项 | 文件 | 内容 |
| --- | --- | --- |
| 坐标架几何 | 新 `ui/editor/components/AxisGuides.kt` (纯数据) | **近边中点小坐标架** (最终): 原点在底座近边 (+z) 中点外侧 0.6 格 = 模型 (0, 0.04, +8.6); +X 杆从原点沿 +x 1.2 格, +Z 杆沿 +z 1.2 格, 粗 0.08, 浮于底座顶面上 0.04; 箭头 = 端部三个递缩方块 0.35 / 0.25 / 0.15 (尖端 = 原点 + 1.95); 字母位 = 原点 + 2.45; **无 Y 杆**; 整个坐标架在网格外 (z > N/2), 方块挡不住它, 它也不压模型。协调者给的右角外侧原点 (N + 0.6, ·, N + 0.6) 实测整体不可见 (`corner-gizmo/`): Volume 裁掉离中心 ≈ 830–890 dp 以外的内容 (底座角 848 dp 仍可见, 角外坐标架 ≥ 896 dp 消失), 故移到边中点并缩短使尖端 ≈ 790 dp |
| 场景 | `SceneParts.axisGuides` (一个实例化 `ModelEntity`, 共享单位盒, 无碰撞体), `VoxelScene.rebuildPlate` 随底座重建; 颜色 `GridColors.AxisGuide` #AAB9B8 **不透明** (`AXIS_ALPHA` 1.0) | 所有模式常驻, 不可交互; ANR 规则不变 (无新网格) |
| 字母 X / Z | `AxisChip.kt` + `EditorScreen.kt` (两个 `Augment`, `AugmentContentAlignment.Center`, 前面平面 scale 1.0) + `LabelAnchor.projectToFront` + `VoxelSceneView.onAnchors` 第三个参数 | **Augment 备选路径**: 56 × 56 设计 dp 轴色圆片 + `label` 20 M `BaseInk` 字母, 锚点 = 尖端点投影到 z 0.92 面 (与目标标签同一视者规则), 夹到 x [0.03, 0.97] / y [0.03, 0.80] (`CHIP_MARGIN_BOTTOM` 0.20: 面的下部在画面外, +Z 圆片自然位置 ≈ 0.85 被画面下缘切半, 0.93 的夹值不起作用, 0.80 完整可见), 随 yaw / pitch 每帧重算; 字串复用 `axis_x` / `axis_z`; 内容固定, 只有锚点变 |
| SDK 路径 (已验证、未采用) | — | javap: foundation `attachmentPanelComponent(context, size, alignment, content)` + `panelSize(dp, dp)`, core `AttachmentPanelComponent` / `LookAtComponent` (`setViewerAsTarget()`, `alignLocalUpToWorldUp`, `lookAtForwardDirection`) 都存在。实做 (合成期建 3 个组件并 `remember`, 挂在 `root` 下带显式 `TransformComponent` 的尖端实体上, 每帧按 `localToView` 定位): logcat 出现 3 个 112 × 112 px 附着窗 (`isViewAttachment=true`, 位置日志正确), 画面里只在 +Y 尖端看到一块**空白浅色方片**, Compose 内容 (圆片 + 字母) 不渲染 (`probe/p7-crop.png`); 换 `NEGATIVE_Z` 朝向无改善 → 按协调者备选改 Augment |
| 单测 | `AxisGuidesTest` 5 例 | 原点在近边中点外 0.6 格; 所有盒在网格外且浮于顶面, 尖端半径 ≤ 10.6 格 (裁切半径内); 杆从原点起、尖端 / 字母位; 箭头递缩; 无 Y 导线 |
| 规格 / 备忘 | `DESIGN_SPEC_R4.md` §T.9, `AGENTS.md` | 几何表 (含前提), 颜色, 字母方法与 SDK 路径结论 |

## 2. 截图 (`docs/screenshots/r11/{zh,en}/r11-{workbench,size}.png`, `panels` 脚本经 `r11.sh`, `r11-run.log`; 带 Y 杆的上一版帧保留在 `r11/with-y/{zh,en}/`)

| 帧 | 观察 |
| --- | --- |
| `zh/r11-workbench.png` / `en/r11-workbench.png` | 最终版 (近边中点小坐标架): 画面右下、底座近边外侧可见完整的 L 形坐标架 — **+X 杆**由原点向右后伸, 端部三级箭头 + "X" 圆片在尖端外侧; **+Z 杆**由原点向观者伸, 箭头朝下、"Z" 圆片被夹在 y 0.80, 落在 Z 杆右侧、箭头旁, 完整在画面内 (`crop-axes.png` 左半); 杆、箭头、字母都在底座玻璃区域内 / 其外沿, 不与亭子或任何方块重叠, 不被遮挡; 无 Y 杆; 四帧黑块占比均 0.003 |
| `zh/r11-size.png` / `en/r11-size.png` | 尺寸行打开 + 3 × 2 × 1 预览 + 标签 "3 × 2 × 1 / 松手放置"; 坐标架位置同 workbench, 尺寸行 (前面平面底部, y 0.935..0.985) 压在 Z 杆下段 / "Z" 圆片之前 (同一视点下尺寸行本就盖住底座近角), X 杆与 "X" 圆片完整可见 |
| `corner-gizmo/{zh,en}/` | 第四版 (右角外侧坐标架 2.5 格) 留档: 杆、箭头、字母全部不可见 — Volume 裁切半径的实测依据 |
| `far-edge/{zh,en}/` | 第三版 (X 后边 / Z 右边) 留档: Z 杆 + "Z" 正常, X 杆被亭子挡住, "X" 圆片投影在屋顶梁上 — 用户四次反馈的依据 |
| `near-edge/zh/` | 第二版 (X 近右边 / Z 左前边, 无 Y) 留档: 两根杆投影成出画的长杆, Z 字母在画面外 (`near-edge/en` 归档时目录被占用 (Permission denied) 而被覆盖, 其内容只有黑块帧, 无损失) |
| `with-y/{zh,en}/` | 第一版 (含 +Y 杆 + "Y" 圆片, 在 `emulator-5556` 拍摄) 留档: `with-y/zh/r11-workbench.png` 近边中点竖起的 Y 杆与其字母正挡在亭子前 — 即用户二次反馈的依据 |
| 旋转视角 | 无 (演示无法转相机) |

## 3. 构建 / 测试

```
./gradlew :app:assembleDebug --offline -q          → EXIT 0
./gradlew :app:testDebugUnitTest --offline -q      → EXIT 0, 单测 68/68 (R10 63 + AxisGuides 5)
dex: SpatialApplication refs across all dex: 3 (exit 0)
settings list global | grep gridspace 为空; 结束时在 emulator-5554 重新启动 SkyboxSpatial 与 GridSpace (adb devices 只有 emulator-5554)
```

## 4. 备注

| 条目 | 说明 |
| --- | --- |
| 几何演进 (四版) | ① 协调者原案: 整边杆 (0..N + 0.6) + 角上 Y 杆 — 第一版什么都看不见 (X 杆在亭子后、Y 杆在后角、Z 杆 85 % 太淡; 画面里沿左边那条淡线是 Volume 自己的边, R9/R10 帧也有); ② 以边中点为中心 ±4.7 格, X / Y 移到近边 (`with-y/`): 三根都可见, 但用户反馈 Y 杆挡模型 → 去掉 Y (`near-edge/zh/`); 用户反馈近边两杆投影成出画长杆、Z 字母出画 → ③ X 后边 / Z 右边 (`far-edge/`): Z 正常, X 杆被亭子挡住、"X" 圆片投影在屋顶梁上 → ④ 右角外侧坐标架 (`corner-gizmo/`): 整体被 Volume 裁掉, 四帧里什么都没有 → ⑤ **近边中点小坐标架** (最终, §1) |
| 颜色不透明 + 浮起 0.04 | 85 % 的 0.08 格杆 (≈ 5 px) 贴在底座玻璃上几乎不可辨 (`probe/p4`); 不透明并浮起后清晰 (`probe/p6`, `p7`) |
| 字母用 Augment 而非附着面板 | 见 §1 "SDK 路径" |
| 与协调者几何的偏差 | 右角外侧原点 (N + 0.6, 0.04, N + 0.6) + 2.5 格杆按字面实现后四帧全空 (`corner-gizmo/`), 原因是 Volume 的裁切半径 (≈ 830–890 dp); 改为近边中点外 0.6 格 + 1.2 格杆 (尖端 ≈ 790 dp), 其余 (粗细、颜色、箭头、字母、无 Y、网格外) 不变。若要恢复角上位置, 需把底座缩小到 ≤ 1000 dp 或放大 Volume |
| 模拟器挂死一次 | 最终版第一次拍摄 (19:24) 四帧全是黑块 (0.48–0.91); `adb emu kill` 后 `pico-cli emulator start` (默认 AVD `Pico_Emulator_6_1`) 起的实例停在 `offline` 40 分钟且 taskkill 杀不掉 (PowerShell `Stop-Process -Force` 才行), 另一个 `--avd Pico_Emulator_6_1_fresh` 实例因 5554 被占而成为 5556; 杀掉两者后再 `pico-cli emulator start --avd Pico_Emulator_6_1_fresh -y` 回到 5554, 等 90 s 后重拍四帧全部干净 (0.003) |
| 无旋转视角帧 | 演示脚本无法转动相机 (yaw / pitch 是视图内部状态, 没有事件), 协调者允许跳过; 字母随 yaw / pitch 重算锚点的逻辑与目标标签相同 (R6–R8 已验证) |
| 模拟器 | 本轮 `pico-cli emulator stop` 没有停掉用户的 `emulator-5554`, `start` 另起了一个实例 `emulator-5556` (无 SkyboxSpatial): 之后 adb 对两台设备报 "more than one device", 第一次 en 拍摄失败; 在 5556 上用 `ANDROID_SERIAL` 钉住重拍时 `walk.sh` 的 `pico-cli app launch` 仍未带 `-d` (sed 改写失败), 两套 walk 超时无帧。最终: `walk.sh` 用 python 改为 `pico-cli app launch -d $ANDROID_SERIAL`, `adb -s emulator-5556 emu kill` 结束我起的实例, **最终 zh / en 帧在用户的 `emulator-5554` 上拍摄** (`ANDROID_SERIAL=emulator-5554`), 最终 APK 装在 5554, 两台的 `gridspace_*` 均已清, 结束时在 5554 重新启动 SkyboxSpatial 与 GridSpace — 其中 **SkyboxSpatial 需从其工程的 `app/build/outputs/apk/debug/app-debug.apk` (10-06) 重新安装**: 我在 R11 开头 `adb -s emulator-5554 emu kill` 掉的那台正是用户装有 SkyboxSpatial 的原实例 (当时误判为残留条目), 之后的 5554 是新实例, 数据里没有它。用户在拍摄期间打开的第三方应用 (`com.technomancer.neonlong` 飞龙、`com.uaybalam.conjure` 飞碟生成器) 的窗口曾挡在亭子前 (`r11/with-y/zh-foreign-window/`, `probe/p1-crop.png`), 拍摄前一并 force-stop (未重新启动) |
