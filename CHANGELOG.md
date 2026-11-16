# Changelog

## 0.3.5-alpha — 2026-11-16

### English

- NEI shortcuts on the virtual pattern grid now resolve the actual MUI screen wrapper (`ModularScreen.getCurrent()`) instead of checking the wrapper class name, which never matched. Literal A/R/P and NEI's configured `bookmark.add`, `recipe.recipe` and `recipe.usage` bindings both work; A calls NEI's public `BookmarkPanel.addOrRemoveItem` and preserves fluid display stacks, R opens crafting, P/U opens the custom pattern page.
- Disabled and filtered cells now draw a red X above the item/fluid icon, not under it. The cell icon subtree lets hover continue to the cell button, restoring the hover tooltip and A/R/P target.
- The block now writes the AE proxy owner on placement (`bindOwner(EntityPlayer)`), so a secured AE network accepts the first connection instead of scheduling `securityBreak` and dropping the block. An existing node is updated through `AENetworkProxy.readFromNBT(null)`.
- The tile re-registers its wireless EU owner with GregTech on `onReady`, so the server keeps working after a restart with the player offline. The block UI now shows the wireless link state and balance; zero balance is shown as `no_power`.
- Added a non-destructive `securityBreak` safety net so a rejected AE connection cannot delete the block or its installed patterns. Static build passed; in-game verification remains with the user.
- Fixed the client-side selection/view sync writes that raised `Sync handler is unable to send packets to server!` and caused the intermittent double-GUI flicker: selection and view refreshes now only send packets from the server side.

### 简体中文

- 虚拟样板格的 NEI 快捷键改为通过 `ModularScreen.getCurrent()` 解包真正的 MUI 屏幕，而不是判断包装类名（此前永远不匹配）。字面 A/R/P 与 NEI 配置的 `bookmark.add`、`recipe.recipe`、`recipe.usage` 都兼容；A 走 NEI 公开的 `BookmarkPanel.addOrRemoveItem`，流体保留真实流体显示栈；R 打开合成查询，P/U 打开自定义样板页。
- 禁用和筛除的格子把红叉画在物品/流体图标之上，而不是被图标覆盖。图标子树允许 hover 继续传递给格子按钮，恢复悬停 tooltip 与 A/R/P 的悬停目标。
- 方块放置时写入 AE 代理主人（`bindOwner(EntityPlayer)`），安全 AE 网络会接受首次连接，不再触发 `securityBreak` 掉落方块；已有节点通过 `AENetworkProxy.readFromNBT(null)` 立即补写主人。
- tile 在 `onReady` 重新向 GregTech 注册无线 EU 主人，服务器重启且玩家离线时仍可供电/合成。方块 UI 增加无线连接状态和余额显示；余额为 0 显示 `no_power`。
- 增加非破坏性的 `securityBreak` 安全网：AE 安全校验拒绝连接时不会删除方块和已安装样板。静态构建通过，实机验证仍由用户执行。
- 修复客户端错误发送 selection/view 同步导致的 `Sync handler is unable to send packets to server!` 与双 GUI 频闪：选择和视图刷新现在只在服务端发包。


### English

- Fixed the server-tick `IllegalAccessError` that crashed the integrated server and prevented the save from loading: the FML event handler was an anonymous package-private class, which the ASM event handler could not access. It is now a public static `ServerTickHandler` with a public `@SubscribeEvent` method.
- Bumped the mod version to `0.3.4-alpha`; static build passed and the handler bytecode access flags were verified public. In-game verification remains with the user.

### 简体中文

- 修复服务端 tick 的 `IllegalAccessError`：原先注册的是匿名包私有事件处理类，FML 的 ASM 事件处理器在另一个类加载器里访问不到它，导致服务端 tick 崩溃、存档无法进入。现改为公开静态 `ServerTickHandler`，处理器方法为 public `@SubscribeEvent`。
- 版本升至 `0.3.4-alpha`；静态构建通过，并用 `javap` 确认处理器类和方法的访问标志为 public。实机验证仍由用户执行。

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
