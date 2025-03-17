package work.crash.fallingalchemy;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import work.crash.fallingalchemy.condition.ICondition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ZenRegister
@ZenClass("mods.fallingalchemy.FallingAlchemy")
public class FallingAlchemyTweaker {
    public static final Map<Block, List<ConversionRule>> RULES = new ConcurrentHashMap<>();

    @ZenMethod
    public static ConsumedItem createConsumedItem(
            IIngredient ingredient,
            int requiredCount,
            @Optional boolean matchNBT,
            @Optional boolean fuzzyNBT
    ) {
        return new ConsumedItem(ingredient, requiredCount, matchNBT, fuzzyNBT);
    }

    @ZenMethod
    public static ConversionBuilder addConversion(
            IItemStack fallingBlock,
            ConsumedItem[] consumedItems,
            double radius,
            IItemStack[] outputs,
            @Optional double successChance,
            @Optional double keepBlockChance,
            @Optional int priority
    ) {
        ItemStack mcFalling = CraftTweakerMC.getItemStack(fallingBlock);
        Block block = (mcFalling.getItem() instanceof ItemBlock) ?
                ((ItemBlock) mcFalling.getItem()).getBlock() : null;

        if (block == null) {
            CraftTweakerAPI.logError("Falling block must be a valid block item!");
            return null;
        }

        // 转换输出物品
        List<ItemStack> mcOutputs = Arrays.stream(outputs)
                .map(CraftTweakerMC::getItemStack)
                .collect(Collectors.toList());

        if (mcOutputs.isEmpty()) return null;

        float success = (float) MathHelper.clamp(successChance, 0.0, 1.0);
        float keep = (float) MathHelper.clamp(keepBlockChance, 0.0, 1.0);
        int finalPriority = priority;

        ConversionRule rule = new ConversionRule(
                block,
                Arrays.asList(consumedItems),
                (float) radius,
                mcOutputs,
                success,
                keep,
                finalPriority
        );

        return new ConversionBuilder(rule);
    }

    public static class ConversionRule implements Comparable<ConversionRule> {
        final Block triggerBlock;
        final float radius;
        final List<ConsumedItem> consumedItems; // 使用List存储多个消耗条件
        final float successChance;
        final float keepBlockChance;
        final List<ItemStack> outputs;
        final List<ICondition> conditions = new ArrayList<>();
        final int priority;

        public ConversionRule(Block trigger, List<ConsumedItem> consumedItems,float radius,
                              List<ItemStack> output, float success, float keep, int priority) {
            this.triggerBlock = trigger;
            this.consumedItems = consumedItems;
            this.radius = radius;
            this.outputs = output;
            this.successChance = success;
            this.keepBlockChance = keep;
            this.priority = priority;
        }

        // 优先级排序（降序）
        @Override
        public int compareTo(ConversionRule other) {
            return Integer.compare(other.priority, this.priority);
        }

    }

    @ZenClass("mods.fallingalchemy.ConversionBuilder")
    public static class ConversionBuilder {
        private final ConversionRule rule;

        public ConversionBuilder(ConversionRule rule) {
            this.rule = rule;
        }

        // 添加生物群系条件（保持不变）
        @ZenMethod
        public ConversionBuilder addBiomeCondition(String biomeId) {
            rule.conditions.add((world, pos) ->
                    world.getBiome(pos).getRegistryName()
                            .equals(new ResourceLocation(biomeId))
            );
            return this;
        }

        @ZenMethod
        public ConversionBuilder addTimeCondition(int min, @Optional int max) {
            int finalMax = max >= 0 ? max : min;
            rule.conditions.add((world, pos) -> {
                long time = world.getWorldTime() % 24000;
                return time >= min && time <= finalMax;
            });
            return this;
        }

        // 优化后的天气条件方法
        @ZenMethod
        public ConversionBuilder addWeatherCondition(
                @Optional boolean requireRaining,
                @Optional boolean requireThundering) {

            // 自动修正非法参数组合
            if (requireThundering && !requireRaining) {
                CraftTweakerAPI.logWarning("Thundering requires raining! Auto-correcting...");
                requireRaining = true;
            }

            final boolean finalRequireRaining = requireRaining;
            final boolean finalRequireThundering = requireThundering;

            rule.conditions.add((world, pos) -> {
                boolean isRaining = world.isRaining();
                boolean isThundering = world.isThundering();

                return (finalRequireThundering && isThundering) ||
                        (finalRequireRaining && isRaining && !finalRequireThundering) ||
                        (!finalRequireRaining && !isRaining);
            });
            return this;
        }

        @ZenMethod
        public void register() {
            RULES.compute(rule.triggerBlock, (k, v) -> {
                if (v == null) v = new ArrayList<>();
                v.add(rule);
                // 注册时排序
                v.sort(ConversionRule::compareTo);
                return v;
            });
        }
    }
}