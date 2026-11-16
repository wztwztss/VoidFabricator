# Changelog

## 0.3.3-alpha — 2026-11-16

### English

- Same-output grouping now hides single-recipe outputs: a group row exists only when at least two recipes produce the same primary output.
- Recipe scans for large GT maps (assembler: 12,605 recipes) now run through a server-tick cooperative queue with a 3 ms / 64-record slice instead of blocking one tick; scan sessions are cached and reused.
- Pattern NBT now stores only the machine plus `DisabledKeys`; the 12k-entry `Entries` list is no longer duplicated into the item. The legacy root projection is capped at 256 entries.
- Recipe view construction no longer copies the full entry pool or builds a full snapshot map; only the visible 91 cells are encoded, and raw entries carry stable global indices for clicks.
- Filtered/blacklisted entries and manually disabled entries both render the red border and large red cross; `filtered` no longer uses the neutral-only dim.
- `VirtualPatternDetails` pattern tokens and condensed lists are lazy, and scan snapshots avoid a second deep stack copy.
- Bumped the mod version to `0.3.3-alpha`; static build and bilingual asset validation passed. In-game verification remains with the user.

### 简体中文

- “筛选相同”只保留有两条及以上相同主输出的分组；单条产物不再单独占一行。
- 大配方池（组装机日志中的 12,605 条）改由服务端 tick 合作队列扫描，每 tick 3 ms / 64 条预算，不再在单个 tick 内同步扫完；扫描会话支持缓存复用。
- 样板 NBT 只保存机器和 `DisabledKeys`，不再把 12k 条 `Entries` 复制进物品；旧根级投影最多保留 256 条。
- 配方视图不再复制整个配方池或构建全量快照 map，只编码当前可见的 91 格；原始条目带稳定的全局索引，点击仍命中正确配方。
- 黑白名单过滤条目和手动禁用条目都显示红边 + 大红叉；`filtered` 不再只有中性变暗。
- `VirtualPatternDetails` 的样板 token 和 condensed 列表改为懒加载，扫描快照避免二次深复制。
- 版本升至 `0.3.3-alpha`；静态构建与双语资源校验通过，实机验证仍由用户执行。

## 0.3.2-alpha — 2026-11-16

### English

- Added GregTech wireless EU power settlement for accepted virtual crafts through `WirelessNetworkManager`.
- The block now binds its owner on placement; legacy blocks without `OwnerUUID` bind the first player who opens them, and later players do not silently re-bind.
- Added an atomic balance check and one-time debit at `recipe EU/t × duration × batches`, before AE inputs are consumed; insufficient balance rejects the craft without a partial charge.
- Added refund when the output buffer rejects a craft after the debit, plus `no_owner`, `insufficient_power`, and `buffer_error` GUI status keys and an energy tooltip.
- Bumped the mod version to `0.3.2-alpha`; static build and bilingual asset validation passed. In-game verification remains with the user.

### 简体中文

- 通过 `WirelessNetworkManager` 为 AE 已接受的虚拟合成接入 GregTech 玩家/团队无线 EU 扣电。
- 方块放置时绑定主人；没有 `OwnerUUID` 的旧方块由第一个打开它的玩家绑定，之后不会静默改绑。
- 在消费 AE 输入前执行一次余额检查和原子扣电，金额为“配方 EU/t × 工作时长 × 工作份数”；余额不足直接拒绝，不会部分扣除。
- 扣电后输出缓冲拒绝时退回本次 EU，并新增 `no_owner`、`insufficient_power`、`buffer_error` GUI 状态键与供电 Tooltip。
- 版本升至 `0.3.2-alpha`；静态构建与双语资源校验通过，实机验证仍由用户执行。

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
