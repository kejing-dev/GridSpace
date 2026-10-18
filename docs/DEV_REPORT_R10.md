# GridSpace 格间 · 研发报告 R10 (面板开关)

日期 2026-10-17 · 依据: 用户反馈 (尺寸行与颜色栏应由用户从底部工具栏打开, 不常驻) → 协调者 R10 · 规格 `docs/DESIGN_SPEC_R4.md` **§T.8** (本轮追加)

## 1. 修改

| 项 | 文件 | 内容 |
| --- | --- | --- |
| 工具栏 | `ui/editor/components/EditorToolbar.kt` | `[添加][擦除][上色][选择] │ ↶ ↷ │ ⌖ │ [尺寸] [颜色] │ [项目] [导出]`; 尺寸 / 颜色 = `ToggleIconButton` 72 (`ToggleIconButtonDefaults.toggleIconButtonSize(72)`, 图标 `ic_cube` / 新增 `ic_palette.xml` 调色盘), checked = 面板显示; 项目 / 导出 `IconButton` 从颜色栏移回; 样式不变; 总宽 676 + 33 + 152 + 33 + 72 + 33 + 152 + 33 + 152 + 40 = **1376** 设计 dp |
| 状态 / 事件 | `EditorUiState.kt`, `EditorViewModel.kt`, `data/AppGraph.kt` | `showSizeRow` / `showColorRail` 默认 **false**; `ToggleSizeRow` / `ToggleColorRail`; 会话记忆 `AppGraph.panels: PanelMemory` (进程级 `@Volatile` 两个布尔, 不写盘; 构造注入, 单测用新实例); `showBrushRow = showSizeRow && mode ∈ {ADD, ERASE}`; 关闭颜色栏时 `picker = null` (取色卡 / 取色条随之关闭); 选区面板仍自动 |
| Augment 条件 | `EditorScreen.kt` | 颜色栏 Augment 仅 `state.showColorRail`; 笔刷行沿用 `showBrushRow`; 取色卡 / 取色条跟随 `picker` |
| 颜色栏 | `ColorRail.kt` | 动作行只剩 中|EN (表头不变) |
| 字串 | `values` / `values-zh` | `toggle_size` 尺寸 / Size, `toggle_colour` 颜色 / Colour (`contentDescription`); 104 / 104 键一致 |
| 演示 | `DemoMode.kt`, `docs/scripts/r10.sh` | walk: `size` 步前 `ToggleSizeRow`, `vivid` 前 `ToggleColorRail` (之后保持打开, burst 的表头仍可见); `size` / `edit` 探针脚本先切换; 新脚本 `panels`: workbench (双关) → size → rail → dragmove (双关, 选区面板自动) → edit; `r10.sh zh|en` 跑它 |
| 单测 | `PanelToggleTest` 3 例 | 默认双关 + 切换与记忆写回; 记忆应用到新编辑器; 关闭颜色栏清掉取色卡 |

## 2. 截图 (`docs/screenshots/r10/{zh,en}/r10-{workbench,size,rail,dragmove,edit}.png`, `r10-run.log`)

第一次拍摄 (`r10/{zh,en}-run1/`): 模拟器正被使用 (两个应用开着), 视点已漂移且压缩器处于黑块期 (10 帧中 8 帧 > 0.5); 按 AGENTS 重启模拟器、等 90 s 后重拍, 全部干净 (zh 每帧第 1 次, en 第 1–8 次)。

| 帧 | 观察 (zh / en 相同, 除文字) |
| --- | --- |
| `r10-workbench.png` | **两个面板都关着**: 只有模型 + 底座 (系统 Toolbar 在截图区下沿外, R1 起已知); 颜色栏与尺寸行均不在 |
| `r10-size.png` | 切换 尺寸 后: 三组尺寸行 `宽 X 3 │ 高 Y 2 │ 深 Z 1` / `W · X 3 │ H · Y 2 │ D · Z 1` 在底部, (1,0,1) 起 3 × 2 × 1 薄荷块 + 落点 + 标签 "3 × 2 × 1 / 松手放置"; 颜色栏仍关 |
| `r10-rail.png` | 尺寸行已关、颜色 已开: 右侧颜色栏 (表头 + 中|EN 行 + 3 槽 + 页签 + 色板 + 编辑颜色; 项目 / 导出 图标不再在栏上); 尺寸行不在; 右下为静默放的五块 |
| `r10-dragmove.png` | 两面板都关, 选择模式有选区 → 选区面板自动出现, 末行 "ΔX +2 · ΔY 0 · ΔZ −1 · 松手移动 5 块"; 变暗原块 + 薄荷幽灵; 尺寸行在 SELECT 下隐藏 (且开关也是关) |
| `r10-edit.png` | 颜色 已开 + 编辑颜色: 颜色栏 + 取色卡 (R7 位置), 尺寸行关 |

工具栏本身在模拟器截图区之外 (Volume 底边在画面下方), 其内容 (尺寸 / 颜色 切换钮, 项目 / 导出) 由代码代验 (`EditorToolbar.kt`), 真机 [H]。

## 3. 构建 / 测试

```
./gradlew :app:assembleDebug --offline -q          → EXIT 0
./gradlew :app:testDebugUnitTest --offline -q      → EXIT 0, 单测 63/63 (R9 60 + PanelToggle 3)
dex: SpatialApplication refs across all dex: 3 (exit 0)
settings list global | grep gridspace 为空; 结束时重新启动 SkyboxSpatial 与 GridSpace
```

## 4. 备注

| 条目 | 说明 |
| --- | --- |
| 默认双关 | 新打开的编辑器 (与新进程) 两个面板都关; 会话内记忆仅进程级 (`AppGraph.panels`), 杀进程即回到默认 |
| 颜色栏关闭时的取色 | 唯一路径是先打开颜色栏 (无其它入口); 关闭栏时取色卡 / 取色条同时关闭, 已在 `PanelToggleTest` 覆盖 |
| 工具栏宽 1376 | 与 R4 §R.1 数值相同 (< 1600); 截图不可见, 真机 [H] |
| 模拟器 | 第一次拍摄遇视点漂移 + 黑块期, 重启后恢复; 结束时 SkyboxSpatial 与 GridSpace 已重新启动 (`ps` 两进程在) |
| 现有脚本 | `r8.sh` 完整集仍可跑 (walk 已显式切换面板); `measure_r4.py` 未改 |
