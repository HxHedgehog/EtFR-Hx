# 交接文档 — 创造栏排序 / 铁砧移除 / 翻译覆盖

> [!IMPORTANT]
> **给下一个 AI 的指令：阅读完本文件后，请删除本文件（creative_tab_handoff.md）再继续工作。**
> 本文件是一次性交接记录，不属于项目代码，不应长期保留。

> 状态：未完成，紧急暂时提交。待办：**测试 F7**。

## 本次会话改动

### 1. 彻底移除铁砧替换方块
原 `BlockNewAnvil` 只是原版 `ContainerRepair` 的复制粘贴，无任何新功能（对比 BrewingStand/Beacon/EnchantmentTable 都有实际功能差异）。已彻底删除，不考虑存档兼容。

- 删除 `blocks/BlockNewAnvil.java`、`inventory/ContainerAnvil.java`、`client/gui/inventory/GuiAnvil.java`
- `ModBlocks.java`：移除 `ANVIL` 枚举及 `ET_FUTURUM_NAMESPACE_BLOCKS` 条目
- `CommonProxy.java` / `GUIIDs.java`：移除 ANVIL 相关 switch case，GUI ID 重新编号
- `ConfigBlocksItems.java`：移除 `enableAnvil` 配置
- `EtFuturumWorldListener.java` / `ModRecipes.java` / `CompatThaumcraft.java` / `NEIEtFuturumConfig.java`：清理引用

### 2. 创造栏映射修复（ModdedCreativeTabs.java）
新增 `getNameAlias` / `getBaseItemName` / `getMetaForOfficialName` 映射：

| 1.21.4 名 | 1.7.10 名 | meta |
|---|---|---|
| `armor_stand` | `wooden_armorstand` | 0 |
| `nether_quartz_ore` | `quartz_ore` | 0 |
| `raw_copper` | `raw_ore` | 0 |
| `raw_iron` | `raw_ore` | 1 |
| `raw_gold` | `raw_ore` | 2 |

### 3. 重写 `dumpNotInCreative()` 缺失检测
旧实现遍历 `ItemRegistry`，被双台阶/技术方块/预留条目严重污染。新实现改为**从 CreativeTabData 条目角度**检测，用与 `displayAllReleventItems()` Part 1 完全相同的 3 层 lookup 路径：

- **Step 1**：无法解析到任何物品的条目（unresolved gap）
- **Step 2**：能解析但实际未显示在 tab 中的条目（resolved but not displayed）

`ClientEventHandler` 的 F7 现在同时调用 `dumpAllTabs()` 和 `dumpNotInCreative()`，输出到 `creative_tab_dump.txt` 和 `creative_tab_missing.txt`。

### 4. 翻译覆盖（vanilla_overrides/zh_CN.lang）
修正错别字 + 术语对齐 1.21.4：

- 砂岩系列：修复「沙岩」→「砂岩」，`smooth` 从错误的「切制砂岩」改为「平滑砂岩」
- 「雕纹」术语：雕纹石砖、雕纹石英块、雕纹砂岩
- 下界术语：下界岩（hellrock）、荧石（lightgem）、下界传送门（portal）

## 待办 / 后续需处理

### 立即待办
- [ ] **测试 F7**：进游戏按 F7，验证 `creative_tab_dump.txt` 和 `creative_tab_missing.txt` 内容

### 已知未解决的缺失物品（默认启用但未显示）
- 3 个 `etfuturum:` 命名空间方块：`brewing_stand` / `beacon` / `daylight_detector`（需要跨命名空间映射）
- `enchanting_table` vs `enchantment_table` 拼写差异
- `suspicious_stew`、`crying_obsidian`、`rose`、`old_gravel`、`cave_vine*`
- `dye_same`（白/蓝/棕/黑染料，4 变体）
- `music_disc_tears`、`music_disc_lava_chicken`
- `shulker_box_upgrade`、`barrel_upgrade`

### `isUnsupportedWood()` 隐藏 bug
`crimson_` / `warped_` / `mangrove_` 被标记为「不存在的木材」并在 Part 1 直接跳过。这些功能默认 `false`（ConfigExperiments），所以默认配置下没问题；但若用户手动开启 `enableCrimsonBlocks` 等，这些 mod 已注册的方块仍会被过滤，导致不显示。

## 相关配置默认值备忘
- Crimson / Warped / Mangrove 方块：默认 `false`（实验性）
- Raw Ores / New Dyes / 音乐唱片 / Suspicious Stew / Crying Obsidian / Rose / Old Gravel / Beetroots / Shulker Box / Barrel：默认 `true`

## 关键文件
- `creative/ModdedCreativeTabs.java` — 排序 + 缺失检测核心
- `creative/CreativeTabData.java` — 1.21.4 官方数据（1548 项，含预留）
- `resourcepacks/vanilla_overrides/assets/minecraft/lang/zh_CN.lang` — 翻译覆盖
