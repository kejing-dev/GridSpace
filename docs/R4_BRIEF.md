# GridSpace 格间 · R4 简报 (空间化尺寸 / 多模态输入 / 双语 / Noto Sans)

日期 2026-10-13 · 基线: `docs/DEV_REPORT_R3.md` + `docs/QA_REPORT_R2.md` (36 通过 / 3 部分 / 6 需真机) · 用户新要求原文:

> 基于 Figma, 用 PICO Spatial SDK, PICO Spatial UI, Noto Sans, 中英双语, 做 shared space 空间应用, 物块有空间 snap
> 对齐网格功能, UI 选颜色、按钮等需要更大、更好用, 符合空间交互易于选中和交互, 支持眼手交互, 手势交互 (direct
> manipulation), 手柄交互, 在模拟器里跑通验证, 分 design / eng / QA agent 循环验证, 保证应用占空间排布充分、合理。

## 1. R3 现状对照新要求 (orchestrator 观察, 见 `docs/screenshots/r3/`)

| 要求 | R3 现状 | 差距 |
| --- | --- | --- |
| 2D UI 更大、易选中 | 标题条 22/15 sp, 色槽热区 56, 模式钮 64 高, 笔刷段控 48, 选区面板钮 88×72, 标签卡 300×88 | 在 1600 dp Volume 旁、臂长距离下全部偏小 (截图里标题条文字约 15 px / 2160 px); 目标最小 56 dp 只是系统下限 |
| 取色更好用 | 长按色槽才弹 700 dp Popup; 面板贴画面底, 滑杆/按钮在可见区外 | 长按是隐藏入口; 面板位置随 Toolbar 落在视野底部 |
| 空间排布充分合理 | 入口 Planar 窗被 Volume 挡在正后方; 系统 Toolbar 在 Volume 底边外 (截图看不到); 四周 Augment 只用了上 (标题) / 下 (笔刷) / 左 (选区) | 右侧空着; 入口窗与 Volume 重叠; 面板分散 |
| snap 对齐网格 | 幽灵方块按格吸附 + 标签 | 没有底座落点高亮; 选区只能用 ± 步进, 不能直接拖动整体吸附移动 |
| 眼手 / 手势 direct manipulation / 手柄 | 统一走 `detectSpatialTap/Drag/ScaleGesture`; 2D hover 用 `spatialHoverEffect`, 3D 用 `HoverEffectComponent` | 没有对 direct (poke / directPinch) 的专门考虑; 方块不能直接抓取移动; 面板没有都加 hover |
| 中英双语 | `BuildFlags.FORCE_CHINESE = true`, 有 values / values-zh 各 91 条 | 英文不可达; 无语言切换 |
| Noto Sans | 系统字体 | 未捆绑 (ElementSpace 有 NotoSans-VF.ttf 2 MB + NotoSansSC-VF.ttf 17 MB 可复用) |

## 2. 已核实的 SDK 事实 (供设计直接引用, 来源 agent-vault 6.1 文档)

- 输入: PICO OS 6 在 眼手 (gaze+pinch) / 直接手势 (poke, directPinch, 手靠近对象时自动切换) / 手势射线 / 手柄射线 + 手柄近场 poke/pinch 之间**自动切换**; 应用用同一套 spatial gesture 接收, 可按事件 source type 过滤。不需要为手柄另做分支。
- 单位: 2D 面板用 dp, 系统按 dmm (角度毫米) 动态缩放, 远近 FOV 恒定; **Volumetric 窗口与 3D 模型不缩放**。系统交互热区下限 56 × 56 dp。
- Augment 指南: 与窗口边距 ≤ 32 dp; 放前方时前移 16 dp; 横向 Augment 宽不超过主窗宽, 纵向不超过主窗高; 左/上 = 一级导航, 下 = 主要操作, 右 = 辅助操作与信息; 优先用系统 Toolbar / TabBar 而不是自造。
- 字体: 官方字体内置, 但用户指定 Noto Sans; CJK ≤ 17 sp 用 Medium 不用 Bold; 最小 12 sp。
- 窗口 API: `LocalSpatialNavigator.current.openWindowContainer(id, tag, bundle)` / `closeWindowContainer(id, tag)`; 默认窗 id 来自 Manifest (`GridSpaceMainWindow`)。默认窗关闭后能否重开 = 本轮探针待验 (见 §4)。
- 模拟器限制不变: 无法注入空间手势, 只能靠 `gridspace_demo` 脚本 + 截图验证; 眼手 / 手柄 / direct 仅能代码审查。

## 3. R4 目标 (验收以此为准)

1. **尺寸升级**: 所有可点目标 ≥ 72 dp (主按钮 ≥ 80 高), 文字: 标题 28 sp, 按钮 20 sp, 正文 18 sp, 说明 16 sp, 最小 14 sp; 取色面板色板格 ≥ 88 dp; 标签卡 ≥ 400 × 112。
2. **空间排布**: Volume 四周都有用: 上 = 标题/导出/语言, 左 = 选区 (有选区时), 右 = **常驻颜色栏** (3 槽 + 当前预设色板, 不再靠长按), 下 = 系统 Toolbar (模式/撤销/重做/回正) + 笔刷行; 入口窗不再被 Volume 挡住 (方案见 §4)。所有 Augment 边距 ≤ 32 dp, 尺寸不超过 Volume 的边。
3. **snap 与直接操控**: 添加模式幽灵吸附 + 底座落点格高亮; **选择模式下在已选方块上捏合拖动 = 整体按格吸附移动** (预览 + 标签 "ΔX ΔY ΔZ", 松手提交 1 步), 步进按钮保留; 旋转/缩放不变。
4. **多模态**: 所有 2D 可点元素 `spatialHoverEffect`; 3D 方块 `HoverEffectComponent`; 代码注释明确 tap/drag 对 gaze+pinch / direct pinch / 手柄 的行为一致; 直接手势 (手靠近) 拖动与射线拖动走同一路径。
5. **双语 + Noto Sans**: 跟随系统语言 (zh → 中文, 其他 → 英文), 入口与标题条提供 中/EN 切换 (持久化); 捆绑 Noto Sans / Noto Sans SC 变量字体 (从 ElementSpace 复制, 带 OFL), 字体随语言切换; 模拟器 (en-US) 截图需同时给出中英两套。
6. **模拟器跑通**: 构建 + 单测 + walk 脚本全程无崩溃/ANR; 截图覆盖 入口 / 工作台 / 添加幽灵+落点 / 占用 / 选区拖动预览 / 颜色栏 / 取色编辑 / 导出, 中英各一套。

## 4. 待定方案 (由探针结果决定)

入口窗与 Volume 重叠:
- 方案 A (首选): 打开编辑器时关闭入口窗 (`closeWindowContainer("GridSpaceMainWindow")`), 标题条加 "项目" 按钮 → `Sheet` 列最近项目 / 新建; 关闭编辑器时重开入口窗 (`openWindowContainer("GridSpaceMainWindow")`)。**前提: 默认窗能重开** (探针验证)。
- 方案 B (兜底): 入口窗保留但缩为 560 × 720 的"项目抽屉", 打开编辑器后内容切换为"正在编辑 X · 返回"; 用户自行拖到一侧。

## 5. 角色与产物

| 角色 | 输入 | 产物 |
| --- | --- | --- |
| Design | 本简报 + `DESIGN_SPEC.md` + Figma V2 + R3 截图 + SDK 文档 | `docs/DESIGN_SPEC_R4.md`: 对 v1.0 的**增量**规格 (只写变化; 尺寸表、布局图、交互表、字体/语言、验收清单 K4.x) |
| Eng | `DESIGN_SPEC_R4.md` + AGENTS.md | 代码 + `docs/DEV_REPORT_R4.md` + `docs/screenshots/r4/` |
| QA | 以上全部 | `docs/QA_REPORT_R4.md` (逐条 K4.x, 缺陷表, 是否需 R5) |

循环直到 QA 无 P0/P1 且 orchestrator 目视截图满意。
