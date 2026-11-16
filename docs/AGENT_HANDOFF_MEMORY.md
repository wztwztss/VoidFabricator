# VoidFabricator — 当前交接记忆

更新时间：2026-11-16（0.3.5-alpha：NEI A/R/P、红叉可见性、AE owner 直连、无线 EU 状态与离线恢复、客户端同步频闪修复）  
当前基线：**方块 27 张实体样板 + 每张样板一台机器 + 独立 325×309 GT 标准机壳 UI + 普通 AE 原生下单 + 方块主人无线 EU 原子扣电**。  
本文取代“一张样板 27 个独立机器池”的交接记录；不要按旧语义继续修改。

## 1. 用户最终锁定的目标

- 虚空合成机方块中部为 9×3 共 27 个实体样板槽。
- 每张实体样板只放一台 GT 机器，识别该机器完整配方池。
- 手持样板普通右键打开自己的独立 UI，仿 TenkeiTech 模板输入总线。
- UI 有搜索、直接物品/流体图标、逐行滚动、滑块、点击排除变暗、同产物一行分组、黑白名单和删除/恢复。
- 悬停虚拟配方格时支持 NEI `R` 查真实 GT 配方、`P` 查样板输入/输出/电压/持续时间/来源。
- 流体直接显示真实流体纹理和数量，不显示桶。
- 样板必须在普通 AE 合成终端正常可见、正常下单，由普通 AE Crafting CPU 执行；不是只能在方块里下单。
- 保留智能倍增、持久化输出缓冲和执行只扣一次料。
- 用户自己进行游戏测试；助手不得启动 GTNH/Prism，也不得宣称实机通过。

用户已否决：一张样板内部 27 机器池、旧双页面、旧三页签/详情 dock、私有 CPU、自研递归规划器、执行时二次扣料、流体桶图标。

## 2. 路径与最终产物

项目：

```text
E:\wzt\MC\modcreater\VoidFabricator
```

实例：

```text
E:\wzt\MC\PL genmulu\GT New Horizons 2.9.0_migrated\.minecraft
```

最终 jar：

```text
build/libs/VoidFabricator-0.3.5-alpha.jar
大小：174,591 字节
SHA-256：B48AA1E0F8CC4599D9D935EA114C1DD0A690A3EE370B41FE184741DB9C887C2D
```

已安装并核对同哈希：

```text
E:\wzt\MC\PL genmulu\GT New Horizons 2.9.0_migrated\.minecraft\mods\VoidFabricator-0.3.5-alpha.jar
大小：174,591 字节
SHA-256：B48AA1E0F8CC4599D9D935EA114C1DD0A690A3EE370B41FE184741DB9C887C2D
```

0.3.2 备份：

```text
build/backup/VoidFabricator-0.3.2-alpha-before-0.3.3.jar
大小：160,882 字节
SHA-256：4E8AFB054CE935E096AC9F969CF3961360526F7EFFE719EFC857C7DFC4F66405
```

0.3.1 备份：

```text
build/backup/VoidFabricator-0.3.1-alpha-before-0.3.2.jar
大小：157,651 字节
SHA-256：3509B679E0FB369DDD0DE1715D9C0401F35EC5BB253071A17A3AB46F8E8D0B23
```

0.3.0 / 0.2.0 备份：

```text
build/backup/VoidFabricator-0.3.0-alpha-before-0.3.1-alpha.jar
build/backup/VoidFabricator-0.2.0-alpha-before-0.3.0-alpha.jar
大小：137,655 字节
SHA-256：DC4181ACF42FC3A5DF25C7817DD39D9B248C7E331458DEDE4A4427209A65A4FD
```

旧 270×320 UI/旧持久化版备份：

```text
build/backup/VoidFabricator-0.2.0-alpha-before-persistence-ui.jar
大小：132,675 字节
SHA-256：13AB831A758DCE920FCE09A22C3EFE298C3AF28D43E1697C81260120C26F3969
```

旧 FDEC 版备份：

```text
build/backup/VoidFabricator-0.2.0-alpha-before-27-physical-patterns.jar
大小：132,536 字节
SHA-256：FDEC249E8023BB258E2AEF3DED1CA634DD06C08A921D6E639094BD360D0C133E
```

另有更早备份 `before-27-independent-pools.jar`、`before-pattern-ui.jar`、`before-gui.jar`。不要再用 FDEC 哈希判断当前安装版本。

## 3. 数据层

### `TileVoidFabricator.java`

- `ItemStack[27]`、`PatternPoolData[27]`、`patternOwners`。
- 世界 NBT 主结构 `Patterns`，每项包含 `Slot`。
- 保留 `PoolPattern` / `Machine` 旧读取入口；旧机器可迁入首个兼容样板。
- 暂时没有样板的旧机器保存在 `legacyMachine`，拆除仍掉落。
- `reloadPatterns` 逐张样板扫描、筛选并汇总；单槽异常隔离。
- 相同 RecipeKey 只发布一个 VirtualPatternDetails，记录所有来源样板。
- `ownerUuid` 写入 NBT `OwnerUUID`；放置时绑定放置玩家，旧方块首次右键绑定当前玩家，之后不改绑。
- `pushPattern` 先校验输出缓冲、再按 `EnergyCost=EU/t×Duration×multiplier` 检查并原子扣除主人无线 EU，最后消费 AE 输入；缓冲失败退款。
- 不再保存当前方块机器槽或单总样板。

### `PatternPoolData.java`

- `CONFIG_FORMAT=4`，运行态只有一个 `RecipePoolSlot(0)`。
- 读取旧格式时才临时扩展到最多 27 池；迁移后 `trimToPrimaryPool()`。
- 保存时 `Pools` 只写主池；根 `Machine/Entries` 保留兼容投影。
- 共享筛选、删除和倍增配置均属于这一张实体样板。
- `GRID_COLUMNS=9`、`VISIBLE_ROWS=3`、`PREVIEW_PAGE_SIZE=27`。
- 搜索只改变显示匹配集合，保留 globalIndex。
- `toView(requestedRow, ...)` 按 `startRow * 9` 截取最多 27 条。
- `getAllowedCount()` 使用唯一 RecipeKey，与 AE 去重发布一致。

### 旧 27 池迁移

- 根节点保留 `LegacyPools` 原始备份。
- 选择主机器，主池清空后按该机器重扫，只恢复相同 RecipeKey 的启停状态。
- 不追加其他机器条目，避免重复和串池。
- 这是有损迁移：其余 26 台机器不会自动拆成新实体样板，只在 LegacyPools 留底。

### `PatternEntry.java` / `RecipePoolSlot.java`

- 条目含 `displayFluid`、真实 FluidStack getter/setter、深拷贝和 NBT 持久化。
- 流体主输出不再创建 `Items.bucket`。
- 每台机器完整扫描，不受可见窗口 27 条限制。
- 重扫按 RecipeKey 保留启停，Deleted 不补回。

## 4. UI 与同步

### `VoidFabricatorGui.java`

- 220×286，顶部 AE 状态，中部 9×3 实体样板格，底部玩家背包。
- 槽组第二参数为每行 9，不是总槽数 27。
- 手持样板潜行右键放入第一个空槽；普通右键仍打开样板 UI。
- 手持 GT 机器右键只提示放入样板 UI。
- 空心圆角矩形组合已移除。

### `PatternItemPanel.java`

- 325×309 GT 标准机壳（`GTGuiThemes.STANDARD`）：一个机器槽、机器名、当前电压、状态、搜索框、13×7 图标窗口、电压标签、纵向滑块、底部 GT 按钮和玩家背包。
- 顶部同一行压缩黑白名单、筛选相同、删除、智能倍增；底部恢复删除、全部启用/停用和 GT logo。
- 左键启停/恢复，右键切换同主输出筛选。
- 物品用 ItemDisplayWidget；流体用 FluidDrawable，图标叠加数量，Tooltip 显示流体名 + mB。
- 动态控件按当前窗口实时取值，滚动后不沿用首次打开的旧图标。
- 空心圆角矩形组合已移除。

### `PatternGuiSession.java`

- 当前池固定 0；搜索由 StringSyncValue 同步，输入时回首行。
- `requestedRow` 为服务端首行偏移；`DoubleSyncValue` 同步滑块比例。
- `rowCount=ceil(matched/13)`，`maxRow=max(0,rowCount-7)`。
- 滚轮每次一行，列表/图标/滑块统一走服务端 scroll。
- 每次只同步当前最多 91 条，不同步整池。
- 点击映射 `requestedRow * 13 + localIndex`，再还原 globalIndex。
- 删除、重扫或数量变化后重新钳制首行。

### `PatternItemGui.java`

- 使用 `GuiFactories.playerInventory().openFromMainHand()`。
- 服务端从 `PlayerInventoryGuiData.getUsedItemStack()` 取当前样板；`pool.setOnSaved(() -> mirrorToHand(...))`，每次保存后把完整 NBT 深拷贝到当前槽并 `setUsedItemStack`；关闭时再 `persistNow()`。
- 不再清空主手，不再复制后关闭归还。
- 已移除 `settings.canInteractWith`，只校验主手仍是有效池样板。
- 右键秒关根因尚未实机闭环；该修改属于防御性修正，不能宣称已实机修复。

## 5. AE 发布、执行与智能倍增

普通 AE 终端 → 普通 Crafting CPU → `pushPattern`：先检查输出缓冲与方块主人无线 EU，原子扣电后只消费 AE 已交付输入，输出进入持久化 OutputBuffer。不私有下单、不二次扣料。

无线 EU 语义：

- `energy/WirelessGridEnergy` 通过 `WirelessNetworkManager.getUserEU` 读余额，`addEUToGlobalEnergyMap(owner, -cost)` 原子扣电，`+cost` 退款。
- 计费公式为 `max(0,EUt) × max(0,Duration) × batches`，batches 来自 `validateInputs` 的输入倍率。
- 余额不足返回 false，不修改输入、不改变电量；输出缓冲 `add` 抛异常时退回本次扣电。
- 零/负 EU/t 按 0 计，不要求 owner。
- 超频、并行、机器 tier 与低于 1 tick 的不完美超频没有单独模拟；当前只使用 `RecipeSnapshot` 基础值。

智能倍增仍由 `SmartDoublingIntegration` 可选反射桥接 AE2-QoL `ISmartDoublingMedium`：

- 任一样板开启可使方块桥接报告可用。
- 对重复配方，任一来源关闭智能倍增则该配方倍率限制为 1。
- 所有来源均开启时取最严格最大轮数，并受缓冲容量和硬上限约束。
- 无 QoL 时回退普通单轮 Medium。

## 6. 当前有意语义，勿误改

- 27 格在方块中，是实体样板；每张样板只有一台机器。
- 每张样板各有一套筛选、删除和倍增配置。
- “筛选相同”开关切换同产物一行分组：同一主输出一行、行内列为该产物备选；只有该产物有两条及以上配方时才占行，单条产物不单独成行；右键仍切换主输出黑白名单。
- 删除按完整 RecipeKey，重扫不补回。
- 多张实体样板相同 RecipeKey 只在 AE 发布一次。
- 重复配方的倍增采用所有来源最严格设置，防止绕过关闭状态。
- 无线 EU 扣电对象是方块主人，不是样板物品主人；团队按 GT leader 共享。
- 首次绑定后不静默改绑；旧无主方块必须先由玩家右键一次。
- 搜索不修改未匹配配方；可见 27 条不是完整池上限。
- 旧 27 池样板有损折叠并保留 LegacyPools，不宣称无损。

## 7. 构建与静态证据

使用 Java 17：

```powershell
$env:JAVA_HOME='E:\java17'
$env:Path="E:\java17\bin;$env:Path"
.\gradlew.bat spotlessApply build --offline
python tools/check_lang.py
python tools/validate_assets.py
```

最终结果：

- 本轮 `spotlessApply build --offline` 最终 `BUILD SUCCESSFUL`（无线 EU 版本）。
- `spotlessCheck`、`checkstyleMain`、`jar`、`reobfJar`、`build` 全通过。
- `test NO-SOURCE`。
- 中英文语言 key 各 264，无重复、无集合差异。
- Java 直接文案引用 95 个，双语均无缺失。
- Tooltip 32/32；PNG 和 jar 资源检查通过；`ASSET_VALIDATION=PASS`。
- jar 内 `mcmod.info`、中英文 lang 均与源文件一致；`mcmod.info` 版本 `0.3.2-alpha`。
- 构建与实例 SHA-256 均为 `4E8AFB054CE935E096AC9F969CF3961360526F7EFFE719EFC857C7DFC4F66405`。

## 8. 未验证与用户实测清单

没有任何本轮游戏内通过证据。构建、资源检查和安装不等于实机验收。

优先实测：

1. 手持样板普通右键是否仍秒关；放入机器后关闭重开是否保留，取出机器后关闭重开是否不再残留，切换主手、断线和保存重启。
2. 325×309 GT 机壳在常用 GUI 缩放下是否完整，13×7 图标、电压标签、搜索框、滑块、底部按钮和玩家背包是否被裁切。
3. 搜索、逐行滚动、拖滑块；非首行点击、筛选、删除和恢复是否命中正确条目；滚轮是否通过 C2S 刷新窗口。
4. 开启“筛选相同”后同产物是否一行、行内备选是否可逐条禁用、行首数量和产物名是否正确；超过 13 个备选时的截断表现。
5. 鼠标悬停格按 `R` 是否打开真实 GT 物品/流体配方，按 `P` 是否打开自定义样板页并显示输入/输出、电压、持续时间、来源。
6. 物品/流体图标、流体纹理与数量、Tooltip、排除变暗、红色叉号、黑白名单恢复和全部启停。
7. 无线 EU：放置方块后 owner 是否正确；无无线电网/零余额时下单是否被拒绝且不进料；余额足够时是否只扣一笔；多份合成是否按倍率扣电；输出缓冲失败是否退款；旧方块首次右键是否完成绑定。
8. 方块 27 槽放入、取出、Shift 点击、拆除掉落、保存重启和旧机器迁移。
8. 普通 AE 终端全部去重样板可见、正常下单、普通 CPU 只扣一次料、输出回注。
9. 重复来源中一张关闭倍增、不同最大轮数、输出缓冲满和断网重连。
10. 旧单池样板、旧 27 池样板、LegacyPools 和多张样板首次扫描性能。

若秒关仍存在，需要新时间点的 `fml-client-latest.log` 和具体关闭路径；不要再用 11 月 8 日 AE selfRep 旧报告作根因。

## 9. 旧资料与项目边界

- `docs/ui-preview-pattern-pools.png`、`docs/ui-preview-pattern-browser.png` 仍是已否决的旧 27 池双页面，不代表当前 UI。当前静态预览是 `docs/ui-preview-pattern-ui-325x309.png`；`tools/RenderPatternUi.java` 已改成当前 325×309/13×7 布局，只作静态预览，不是实机截图。
- 当前 README、设计、复核、交接和 mcmod.info 已改为 27 实体样板槽 + 325×309 GT 机壳口径。
- 不修改 AE2-QoL、TenkeiTech 或游戏实例其它文件；仅安装本项目 jar 和读取日志/源码。
- 旧 `PatternEditorPanel` / `PatternEditorGui` 是未使用死代码，不是当前入口。
- 未建立可靠的项目 Git 基线；重要旧 jar 已备份，不要覆盖备份文件。
