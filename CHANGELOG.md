# Changelog

## 0.3.1-alpha — 2026-11-16

### English

- Physical-pattern model: the Void Fabricator exposes 27 physical pattern slots; each pattern owns one GT machine and its complete recipe pool.
- Restored the 325×309 GT-style pattern UI with 13×7 visible entries, 20×20 cells, real fluid icons, tier-row toggles, search, scrolling, and a player inventory.
- Added one-row-per-output grouping: alternatives for the same output share a row and can be disabled individually.
- Added red-cross disabled styling, compact tooltips, same-output black/white-list toggles, and per-pattern deletion/restore.
- Added NEI `R` / `P` integration for virtual recipe cells, including a custom usage page with inputs, outputs, voltage, EU/t, duration, and source.
- Fixed machine persistence on pattern edits and current-hand NBT mirroring; added sneak-right-click guard to avoid the block/item double-GUI path.
- Updated MUI2 view format to 4 and documented the 13-alternatives-per-row limit.

### 简体中文

- 实体样板模型：虚空合成机为 27 个实体样板槽；每张样板绑定一台 GT 机器并拥有完整配方池。
- 恢复 325×309 GT 风格样板 UI：13×7 可见窗口、20×20 格子、真实流体图标、电压整层启停、搜索、滚动和标准玩家背包。
- 新增同产物一行分组：同一主输出的备选配方共占一行，可逐条禁用。
- 新增红色叉号禁用样式、紧凑 Tooltip、同产物黑白名单切换，以及每张样板的删除/恢复。
- 新增虚拟配方格的 NEI `R` / `P`：自定义 usage 页面显示输入、输出、电压、EU/t、持续时间和来源。
- 修复样板机器编辑后的持久化与当前主手 NBT 镜像；增加潜行右键守卫，避免方块/物品双入口同时打开 GUI。
- 将 MUI2 视图格式升级为 4，并记录每行最多 13 个备选的当前限制。

## 0.3.0-alpha

- Moved the design to 27 physical pattern slots.
- Added the GT-style single-machine pattern configuration center.
- Added AE-native provider/medium execution and optional AE2-QoL smart-doubling bridge.

## 0.2.0-alpha

- Established the AE-native virtual processing pattern pipeline and persistent output buffer.
