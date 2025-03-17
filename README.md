# Falling Alchemy - 坠落炼金

提供基于下落方块物理交互的炼金系统，支持高度自定义的配方配置。  
**核心扩展**：与CraftTweaker深度集成，支持概率控制、复合条件判断与音效定制。

---

## 目录
- [功能特性](#功能特性)
- [安装要求](#安装要求)
- [配置指南](#配置指南)
    - [基础语法](#基础语法)
    - [参数说明](#参数说明)
    - [配置示例](#配置示例)
- [条件系统](#条件系统)
- [音效系统](#音效系统)
- [常见问题](#常见问题)
- [Todo](#Todo)

---

## 功能特性
✅ **核心机制**
- 自定义下落方块（沙子/沙砾/铁砧等）
- 检测范围内指定物品并替换
- 支持多产物生成和数量比例控制


⚙️ **高级配置**
- 配方成功率 (`0.0~1.0`)
- 方块保留几率 (`0.0~1.0`)
- 检测半径动态调整
- 失败/成功音效定制化
- NBT模糊匹配 (`fuzzyNBT`)
- 配方执行优先级 (`priority`)


🌦️ **条件系统**
- 生物群系限制
- 时间范围 (MC时间 `0~24000`)
- 天气状态 (晴天/下雨/雷暴)
- 高度限制
- 月相限制

---

## 安装要求
1. Minecraft 1.12.2
2. Cleanroom版本: **0.3.0+**
3. 必须模组:
    - [CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker) (版本4.1.20+)
4. 将本模组放入 `mods/` 文件夹

---

## 配置指南
### 基础语法
```zenscript
import mods.fallingalchemy.FallingAlchemy;
import mods.fallingalchemy.ConsumedItem;

val builder = FallingAlchemy.addConversion(
    fallingBlock as IItemStack,      // 触发方块
    consumedItems as ConsumedItem[], // 消耗物品数组
    radius as double,                // 检测半径（格）
    outputs as IItemStack[],         // 产物列表
    successChance as double,         // 成功率（可选，默认1.0）
    keepBlockChance as double,       // 方块保留率（可选，默认0.0）
    priority as int,                 // 优先级（可选，默认0）
    successSound as string,          // 成功音效（可选）
    successVolume as float,          // 成功音量（0.1-2.0）
    successPitch as float,           // 成功音高（0.5-2.0）
    failureSound as string,          // 失败音效（可选）
    failureVolume as float,          // 失败音量
    failurePitch as float            // 失败音高
);

// 创建消耗品对象
val coalReq = FallingAlchemy.createConsumedItem(
    <minecraft:coal_ore>.withTag({Unbreakable: 1 as byte}),  // 匹配物品
    3,                  // 需要数量（可选，默认1）
    true,               // 匹配NBT（可选，默认false）
    true               // 模糊NBT匹配（可选，默认false）
);

// 添加复合条件
builder.addHeightCondition(60, 128)
       .addMoonPhaseCondition(4);

builder.register(); // 注册配方
```

---

### 参数说明
| 参数名                   | 类型             | 说明                                                   |
|-----------------------|----------------|------------------------------------------------------|
| `fallingBlock`        | IItemStack     | 必须为方块物品（如`<minecraft:sand>`）                         |
| `consumedItems`       | ConsumedItem[] | 消耗品数组（需使用createConsumedItem创建）                       |
| `priority`            | int            | 配方优先级（数值越大越优先执行）                                     |
| `radius`              | double         | 检测半径（建议1-5格）                                         |
| `outputs`             | IItemStack[]   | 产物列表，支持数量乘数（如`<minecraft:diamond>*2`）                |
| `successChance`       | double         | 配方执行概率（0.0~1.0）                                      |
| `keepBlockChance`     | double         | 触发后保留方块的几率（0.0~1.0）                                  |
| `successSound`        | string         | 成功音效资源路径（如`minecraft:entity.experience_orb.pickup`）  |
| `failureSound`        | string         | 失败音效资源路径                                             |

**ConsumedItem参数说明**：
- `IIngredient ingredient`: 消耗物
- `int requiredCount`: 消耗数量(可选，默认为1)
- `bool matchNBT`: 是否严格匹配NBT(可选，默认为false)
- `bool fuzzyNBT`: 当matchNBT=true时，只验证存在的标签是否匹配（不检查额外标签）(可选，默认为false)

---

### 配置示例
#### 多消耗品配方
```zenscript
// 同时消耗煤炭(需要NBT匹配)和金锭
val coal = FallingAlchemy.createConsumedItem(
    <minecraft:coal_ore>.withTag({Unbreakable: 1 as byte}),
    2, true, false
);
val gold = FallingAlchemy.createConsumedItem(
    <ore:ingotGold>,
    1, false, false
);

FallingAlchemy.addConversion(
    <minecraft:anvil>,
    [coal, gold], // 消耗品数组
    2.5,
    [<minecraft:diamond>*3],
    0.8,
    0.3,
    5 // 高优先级
).register();
```

#### 全功能示例
```zenscript
val builder = FallingAlchemy.addConversion(
    <minecraft:sand>,
    [FallingAlchemy.createConsumedItem(<minecraft:ender_pearl>, 1)],
    3.0,
    [<minecraft:ender_eye>],
    0.75,
    0.2,
    10,
    "minecraft:entity.endermen.teleport", 1.0, 1.2,
    "minecraft:block.glass.break", 0.8, 0.9
);

builder.addHeightCondition(80, 255)
       .addMoonPhaseCondition(0)
       .addWeatherCondition(true, false)
       .setSuccessSound("minecraft:block.enchantment_table.use", 0.5, 1.5)
       .register();
```

---

## 条件系统
### 条件类型大全
| 方法                     | 参数示例                   | 说明           |
|--------------------------|--------------------------|--------------|
| `addBiomeCondition`      | `"minecraft:jungle"`     | 限定生物群系       |
| `addTimeCondition`       | `0, 12000`               | 时间区间（MC时间）   |
| `addWeatherCondition`    | `true, false`            | 是否下雨/雷暴      |
| `addHeightCondition`     | `60, 128`                | Y轴高度范围       |
| `addMoonPhaseCondition`  | `3`                      | 指定月相（0-7）    |

**特殊说明**：
- 天气条件自动修正逻辑：当设置需要雷暴(`requireThundering=true`)时，会自动启用下雨要求
- 高度条件支持单值模式：`addHeightCondition(100)`表示Y=100时触发

---

## 音效系统
### 音效配置方法
```zenscript
// 方法1：在addConversion时直接指定
builder.setSuccessSound("音效ID", 音量, 音高);

// 方法2：单独设置
builder.setFailureSound("minecraft:block.anvil.land", 1.0, 0.8);
```

**音效参数规范**：
- 音量范围：`0.1` ~ `2.0`（默认1.0）
- 音高范围：`0.5` ~ `2.0`（默认1.0）
- 资源格式：使用Minecraft标准音效ID，如`"minecraft:entity.lightning.thunder"`

---

## 常见问题
❓ **配方未触发**
- 确认下落方块已注册
- 检查检测半径是否覆盖物品位置
- 验证已设置条件是否匹配

❓ **音效不播放**
- 确认音效ID是否正确（可使用F3+H查看物品音效ID）
- 检查音量/音高参数是否在合法范围内

❓ **多消耗品匹配异常**
- 检查消耗品NBT匹配模式
- 确保所有消耗品的总数量满足要求

❓ **优先级不生效**
- 检查priority参数是否设置（默认0）
- 高优先级配方应设置更大数值（如10 > 5）

---

## Todo
- [x] 炼金完成/失败的声效提醒
- [x] Y轴限制条件
- [ ] 添加对落地后方块底面方块的"炼金"支持
- [ ] 跨维度条件支持(暂时可用生物群系条件进行实现)
- [ ] 配方事件

**License**: MIT  
**反馈渠道**: [提交Issue](https://github.com/cneicy/FallingAlchemy/issues)  
**下载地址**: [最新版本](https://github.com/cneicy/FallingAlchemy/releases)
