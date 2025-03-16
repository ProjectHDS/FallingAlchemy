# Falling Alchemy - 坠落炼金

提供另一个偏门"炼金"方式的Minecraft 1.12.2模组，允许通过下落方块的物理交互触发物品转换。  
**功能**：与CraftTweaker集成，支持概率、条件判断配置。

---

## 目录
- [功能特性](#功能特性)
- [安装要求](#安装要求)
- [配置指南](#配置指南)
    - [基础语法](#基础语法)
    - [参数说明](#参数说明)
    - [配置示例](#配置示例)
- [条件系统](#条件系统)
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

🌦️ **条件系统**
- 生物群系限制
- 时间范围 (MC时间 `0~24000`)
- 天气状态 (晴天/下雨/雷暴)


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

val builder = FallingAlchemy.addConversion(
    fallingBlock as IItemStack,      // 触发方块
    consumedItem as IItemStack,      // 消耗物品
    radius as double,                // 检测半径（格）
    outputs as IItemStack[],         // 产物列表
    requiredCount as int,            // 需要消耗数量（可选，默认1）
    successChance as double,         // 成功率（可选，默认1.0）
    keepBlockChance as double        // 方块保留率（可选，默认0.0）
);

// 添加条件
builder.addBiomeCondition("minecraft:desert");
builder.addTimeCondition(6000, 18000); 

builder.register(); // 注册配方
```

---

### 参数说明
| 参数名            | 类型            | 说明                         |
|-------------------|----------------|----------------------------|
| `fallingBlock`    | IItemStack     | 必须为方块物品（如`<minecraft:sand>`）|
| `consumedItem`    | IItemStack     | 被消耗的物品（如`<minecraft:coal>`）|
| `radius`          | double         | 检测半径（建议1-5格）          |
| `outputs`         | IItemStack[]   | 产物列表，支持数量乘数（如`<minecraft:diamond>*2`）|
| `successChance`   | double         | 配方执行概率（0.0~1.0）        |
| `keepBlockChance` | double         | 触发后保留方块的几率（0.0~1.0） |

---

### 配置示例
#### 基础转换
```zenscript
// 沙子下落将1个煤炭转为1个钻石
FallingAlchemy.addConversion(
    <minecraft:sand>,
    <minecraft:coal>,
    1.5,
    [<minecraft:diamond>]
).register();
```

#### 多产物配方
```zenscript
// 消耗3个铁锭生成1个下界之星+2个金粒
FallingAlchemy.addConversion(
    <minecraft:anvil>,
    <minecraft:iron_ingot>,
    2.0,
    [<minecraft:nether_star>, <minecraft:gold_nugget>*2],
    3
).register();
```

#### 条件组合
```zenscript
val builder = FallingAlchemy.addConversion(
    <minecraft:gravel>,
    <minecraft:gold_ingot>,
    1.5,
    [<minecraft:emerald>],
    2,
    0.7,
    0.4
);

builder
.addBiomeCondition("minecraft:plains")
.addTimeCondition(13000, 23000) // 下午1点至11点
.addWeatherCondition(true, false); // 需要下雨但不要雷暴

builder.register();
```

---

## 条件系统
### 可用条件类型
| 方法                     | 参数示例                   | 说明                 |
|--------------------------|--------------------------|--------------------|
| `addBiomeCondition`      | `"minecraft:jungle"`     | 限定生物群系         |
| `addTimeCondition`       | `0, 12000`               | 时间区间（MC时间）   |
| `addWeatherCondition`    | `true, false`            | 是否下雨/雷暴        |

---


## 常见问题
❓ **条件不生效**
- 使用`/ct log`查看脚本加载状态
- 确保生物群系名称大小写正确

❓ **配方未触发**
- 确认下落方块已注册
- 检查检测半径是否覆盖物品位置
- 验证已设置条件是否匹配


---

## Todo

- [ ] 炼金完成/失败的粒子、声效提醒
- [ ] Y轴限制条件
- [ ] 添加对落地后方块底面方块的"炼金"支持

**License**: MIT  
**反馈/建议**: 提交至 [GitHub Issues](https://github.com/cneicy/FallingAlchemy/issues)  
**下载**: [Release](https://github.com/cneicy/FallingAlchemy/releases)