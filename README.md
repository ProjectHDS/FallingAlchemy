# Falling Alchemy - 坠落炼金

提供基于下落方块物理交互的炼金系统，支持高度自定义的配方配置。  
**核心扩展**：与CraftTweaker深度集成，支持概率控制、复合条件判断、事件监听与音效控制。

---

## 目录
- [功能特性](#功能特性)
- [安装要求](#安装要求)
- [配置指南](#配置指南)
    - [基础语法](#基础语法)
    - [参数说明](#参数说明)
    - [配置示例](#配置示例)
- [条件系统](#条件系统)
- [事件系统](#事件系统)
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
- 单次处理
- 铁砧下输入物防毁
- 方块位移检测及额外产出计算
- 检测半径调整
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

## 配置指南
### 快速开始
```zenscript
import mods.fallingalchemy.FallingAlchemy;
import mods.fallingalchemy.ConsumedItem;

// 1. 创建消耗品对象
val coalReq = FallingAlchemy.createConsumedItem(
    <minecraft:coal_ore>.withTag({Unbreakable: 1 as byte}),  // 匹配物品
    3,                  // 需要数量（可选，默认1）
    true,               // 匹配NBT（可选，默认false）
    true               // 模糊NBT匹配（可选，默认false）
);

// 2. 创建 Builder
val builder = FallingAlchemy.addConversion(
    <minecraft:anvil>,                                   // 触发方块
    [coalReq],                                           // 消耗品
    [<minecraft:diamond>]                                // 产物
);

// 3. 链式配置参数
builder.setRadius(2.0)              // 设置半径
       .setSingle(true)             // 开启单次处理（一次掉落只变一个）
       .setRescueItems(true)        // 开启物品保护（防止剩余煤炭被铁砧砸没）
       .setSuccessChance(0.8)       // 80% 成功率
       .addHeightCondition(0, 60)   // 高度限制
       .register();                 // 注册配方

```

---
### 方法说明
`FallingAlchemy.addConversion(IItemStack fallingBlock, ConsumedItem[] inputs, IItemStack[] outputs)`
返回一个 `ConversionBuilder` 对象。
### Builder方法一览
| 方法名                  | 参数                     | 说明                                               |
|----------------------|------------------------|--------------------------------------------------|
| `setRadius`          | `double`               | 检测半径（默认 2.0）                                     |
| `setSingle`          | `boolean`              | 设为 `true` 时，一次下落事件仅执行一次配方，不再消耗范围内所有材料。           |
| `setRescueItems`     | `boolean`              | 设为 `true` 时，若消耗品堆叠数量大于需求，模组会"复活"因铁砧掉落而判定死亡的剩余物品。 |
| `setPriority`        | `int`                  | 优先级，数值越大越先判断。                                    |
| `setSuccessChance`   | `double`               | 成功率 (0.0 - 1.0)。                                 |
| `setKeepBlockChance` | `double`               | 炼金后下落方块（如铁砧）不消失的概率。                              |
| `setDisplacement`    | `double, boolean`      | 设置最小下落位移，第二个参数控制是否根据额外位移增加产出。                    |
| `setSuccessSound`    | `string, float, float` | 设置成功音效ID、音量、音高。                                  |
| `setFailureSound`    | `string, float, float` | 设置失败音效ID、音量、音高。                                  |
| `register`           | `string`(可选)           | 配方注册，可传入字符串作为配方ID。                               |

**ConsumedItem参数说明**：
- `IIngredient ingredient`: 消耗物
- `int requiredCount`: 消耗数量(可选，默认为1)
- `bool matchNBT`: 是否严格匹配NBT(可选，默认为false)
- `bool fuzzyNBT`: 当matchNBT=true时，只验证存在的标签是否匹配（不检查额外标签）(可选，默认为false)

---

### 脚本示例

```zenscript
import mods.fallingalchemy.FallingAlchemy;
import mods.fallingalchemy.ConsumedItem;
import mods.fallingalchemy.event.FallingConversionPreEvent;
import mods.fallingalchemy.event.FallingConversionPostEvent;

// 场景：铁砧砸在 "附魔金苹果" 和 "重命名为 Magic 的木棍" 上 -> 生成 "下界之星"
val builder = FallingAlchemy.addConversion(
    <minecraft:anvil>, // 触发方块
    [
        // NBT 严格匹配示例：必须是附魔金苹果 (Metadata 1)
        FallingAlchemy.createConsumedItem(<minecraft:golden_apple:1>, 1),
        
        // NBT + 模糊匹配示例：
        // 需要名字叫 "Magic" 的木棍
        // 参数: 物品, 数量(1), 匹配NBT(true), 模糊匹配(true-允许有多余标签)
        FallingAlchemy.createConsumedItem(<minecraft:stick>.withTag({display: {Name: "Magic"}}), 1, true, true)
    ],
    [<minecraft:nether_star>] // 产物
);

// 链式配置详细参数
builder.setRadius(2.5)                            // 检测半径 2.5 格
       .setSuccessChance(0.8)                     // 80% 成功率
       .setKeepBlockChance(0.0)                   // 铁砧必定消失 (0% 保留)
       .setPriority(100)                          // 高优先级，优先于其他配方判定
       .setSingle(true)                           // 一次下落只处理一份配方，防止吞噬整组材料
       .setRescueItems(true)                      // 复活因铁砧伤害而消失的剩余材料
       .setSuccessSound("minecraft:ui.toast.challenge_complete", 1.0, 1.0)
       .setFailureSound("minecraft:entity.item.break", 1.0, 0.8)
       .addHeightCondition(0, 60)                 // 只能在地下进行 (Y < 60)
       .addMoonPhaseCondition(0)                  // 必须是满月
       .register("apple");                        // 注册生效


// Pre 事件：在配方判定成功即将生成产物前触发 (可取消、可修改产物)
events.onFallingConversionPre(function(event as FallingConversionPreEvent) {
        if (event.id == "apple") {
            // 示例：如果在下雨时进行，奖励翻倍
            if (event.world.raining) {
                event.addOutput(<minecraft:nether_star>); // 额外增加一颗下界之星
            }
            // 示例：如果玩家运气不好，则取消
            if (event.world.random.nextInt(100) < 5) {
                event.cancel(); // 取消本次配方执行，物品不消耗
            }
        }
    }
});

// Post 事件：在配方完全执行结束后触发
events.onFallingConversionPost(function(event as FallingConversionPostEvent) {
    // 打印日志记录炼金发生的位置
    print(event.position.x ~ "," ~ event.position.y ~ "," ~ event.position.z);
});
```

---

## 条件系统
### 条件类型大全
| 方法                      | 参数示例                 | 说明         |
|-------------------------|----------------------|------------|
| `addBiomeCondition`     | `"minecraft:jungle"` | 限定生物群系     |
| `addTimeCondition`      | `0, 12000`           | 时间区间（MC时间） |
| `addWeatherCondition`   | `true, false`        | 是否下雨/雷暴    |
| `addHeightCondition`    | `60, 128`            | Y轴高度范围     |
| `addMoonPhaseCondition` | `3`                  | 指定月相（0-7）  |

**特殊说明**：
- 天气条件自动修正逻辑：当设置需要雷暴(`requireThundering=true`)时，会自动启用下雨要求
- 高度条件支持单值模式：`addHeightCondition(100)`表示Y=100时触发

---
## 事件系统
模组提供了 `Pre` (执行前) 和 `Post` (执行后) 两个事件，允许使用 ZenScript 进行动态控制。

脚本开头需导入事件类。
```zenscript
import mods.fallingalchemy.event.FallingConversionPreEvent;
import mods.fallingalchemy.event.FallingConversionPostEvent;
```

1. Pre 事件 (FallingConversionPreEvent)

触发时机：配方匹配成功，即将执行消耗和生成产物时。
主要用途：取消配方、修改产物、添加额外逻辑。

| 属性 (Getter)        | 类型                 | 说明                               |
|--------------------|--------------------|----------------------------------|
| event.id           | `string`           | 配方ID（如果在 register 时设置了），否则为空字符串。 |
| event.fallingBlock | `IItemStack`       | 触发此次炼金的下落方块。                     |
| event.inputs       | `ConsumedItem[]`   | 配方所需的输入物品列表。                     |
| event.world        | `IWorld`           | 事件发生的世界。                         |
| event.position     | `IBlockPos`        | 事件发生的位置。                         |
| event.outputs      | `List<IItemStack>` | 即将生成的产物列表。                       |

| 方法 (Setter)             | 说明                      |
|-------------------------|-------------------------|
| event.cancel()          | 取消本次炼金。物品不会被消耗，也不会生成产物。 |
| event.addOutput(item)   | 向产物列表中添加一个新的ItemStack。  |
| event.setOutputs(items) | item[]，覆盖当前的产物列表。       |

2. Post 事件 (FallingConversionPostEvent)

触发时机：配方执行完毕，产物生成后。
主要用途：日志记录、统计、生成额外特效。

| 属性 (Getter)        | 类型                 | 说明         |
|--------------------|--------------------|------------|
| event.id           | `string`           | 配方ID。      |
| event.fallingBlock | `IItemStack`       | 触发的下落方块。   |
| event.inputs       | `ConsumedItem[]`   | 配方的输入物品列表。 |
| event.world        | `IWorld`           | 事件发生的世界。   |
| event.position     | `IBlockPos`        | 事件发生的位置。   |
| event.outputs      | `List<IItemStack>` | 最终生成的产物列表。 |


```zenscript
// Pre 事件监听
events.onFallingConversionPre(function(event as FallingConversionPreEvent) {
    
    // 方式一：通过 ID 判断（推荐）
    // 需要在配方注册时使用 .register("my_recipe_id")
    if (event.id == "my_recipe_id") {
        if (event.world.raining) {
            event.addOutput(<minecraft:slime_ball>); // 雨天额外产出
        }
        return;
    }

    // 方式二：通过下落方块判断
    if (event.fallingBlock.definition.id == "minecraft:anvil") {
        // 5% 概率失败
        if (event.world.random.nextInt(100) < 5) {
            event.cancel(); 
            print("炼金意外失败！");
        }
    }
});

// Post 事件监听
events.onFallingConversionPost(function(event as FallingConversionPostEvent) {
    print("炼金成功！ID: " + event.id + " 位置: " + event.position.x + "," + event.position.y);
});
```
---
## 音效系统
### 音效配置方法
```zenscript
// 在builder中直接指定
builder.setSuccessSound("音效ID", 音量, 音高);
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
- [x] 配方事件
- [x] 跨维度条件支持(暂时可用生物群系条件进行实现)
- [ ] 添加对落地后方块底面方块的"炼金"支持(可通过事件实现)

**License**: MIT  
**反馈渠道**: [提交Issue](https://github.com/cneicy/FallingAlchemy/issues)  
**下载地址**: [最新版本](https://github.com/cneicy/FallingAlchemy/releases)
