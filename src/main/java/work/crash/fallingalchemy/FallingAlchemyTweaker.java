package work.crash.fallingalchemy;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
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
    public static ConversionBuilder addConversion(
            IItemStack fallingBlock,
            IItemStack consumedItem,
            double radius,
            IItemStack[] outputs, // 多产物
            @Optional int requiredCount, // 可选参数：所需消耗品数量（默认1）
            @Optional double successChance,   // 成功概率（默认1.0）
            @Optional double keepBlockChance   // 方块保留概率（默认0.0）
    ) {
        ItemStack mcFalling = CraftTweakerMC.getItemStack(fallingBlock);
        Block block = (mcFalling.getItem() instanceof ItemBlock) ?
                ((ItemBlock) mcFalling.getItem()).getBlock() : null;

        ItemStack mcConsumed = CraftTweakerMC.getItemStack(consumedItem);
        List<ItemStack> mcOutputs = Arrays.stream(outputs)
                .map(CraftTweakerMC::getItemStack)
                .collect(Collectors.toList());

        if (block == null || mcConsumed.isEmpty() || mcOutputs.isEmpty()) return null;

        float success = (float) MathHelper.clamp(successChance, 0.0, 1.0);
        float keep = (float) MathHelper.clamp(keepBlockChance, 0.0, 1.0);

        return new ConversionBuilder(new ConversionRule(block,mcConsumed.getItem(),(float)radius,requiredCount > 0 ? requiredCount : 1,
                mcOutputs,success, keep));
    }

    // 规则内部类
    public static class ConversionRule {
        final Block triggerBlock;
        final Item consumedItem;
        final float radius;
        final int requiredCount;
        final float successChance;
        final float keepBlockChance;
        List<ItemStack> outputs;
        final List<ICondition> conditions = new ArrayList<>();
        /*ParticleSettings successParticles;
        ParticleSettings failParticles;*/

        public ConversionRule(Block trigger, Item consumed, float radius, int count,List<ItemStack> output,
                              float success, float keep) {
            this.triggerBlock = trigger;
            this.consumedItem = consumed;
            this.radius = radius;
            this.outputs = output;
            this.requiredCount = count;
            this.successChance = success;
            this.keepBlockChance = keep;
        }
    }
    @ZenClass("mods.fallingalchemy.ConversionBuilder")
    public static class ConversionBuilder {
        private final ConversionRule rule;

        public ConversionBuilder(ConversionRule rule) {
            this.rule = rule;
        }

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
        @ZenMethod
        public ConversionBuilder addWeatherCondition(boolean requireRaining, boolean requireThundering) {
            // 参数合法性检查
            if (requireThundering && !requireRaining) {
                CraftTweakerAPI.logError("雷暴天气必须同时需要下雨！已自动修正 requireRaining=true");
                requireRaining = true;
            }

            final boolean finalRequireRaining = requireRaining;
            final boolean finalRequireThundering = requireThundering;

            rule.conditions.add((world, pos) -> {
                boolean isRaining = world.isRaining();
                boolean isThundering = world.isThundering();

                if (finalRequireThundering) {
                    return isThundering; // 隐含 requireRaining=true
                } else if (finalRequireRaining) {
                    return isRaining && !isThundering; // 需要下雨但不要雷暴
                } else {
                    return !isRaining; // 不需要下雨
                }
            });
            return this;
        }

        /*@ZenMethod
        public ConversionBuilder setParticles(
                String successType, int[] successColor, int successCount,
                String failType, int[] failColor, int failCount
        ) {
            rule.successParticles = new ParticleSettings(
                    EnumParticleTypes.valueOf(successType),
                    successColor,
                    successCount
            );
            rule.failParticles = new ParticleSettings(
                    EnumParticleTypes.valueOf(failType),
                    failColor,
                    failCount
            );
            return this;
        }*/

        @ZenMethod
        public void register() {
            RULES.computeIfAbsent(rule.triggerBlock, k -> new ArrayList<>()).add(rule);
        }
    }
}