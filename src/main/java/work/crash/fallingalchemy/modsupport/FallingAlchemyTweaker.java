package work.crash.fallingalchemy.modsupport;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import work.crash.fallingalchemy.condition.ICondition;
import work.crash.fallingalchemy.item.ConsumedItem;

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
            @Optional int requiredCount,
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
            @Optional int priority,
            @Optional double displacement,
            @Optional boolean additionalProducts,
            @Optional String successSound,
            @Optional float successVolume,
            @Optional float successPitch,
            @Optional String failureSound,
            @Optional float failureVolume,
            @Optional float failurePitch
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

        SoundEvent successSd = parseSound(successSound);
        SoundEvent failureSd = parseSound(failureSound);
        float sVol = MathHelper.clamp(successVolume, 0.1f, 2.0f);
        float sPitch = MathHelper.clamp(successPitch, 0.5f, 2.0f);
        float fVol = MathHelper.clamp(failureVolume, 0.1f, 2.0f);
        float fPitch = MathHelper.clamp(failurePitch, 0.5f, 2.0f);

        ConversionRule rule = new ConversionRule(
                block,
                Arrays.asList(consumedItems),
                (float) radius,
                (float) displacement,
                additionalProducts,
                mcOutputs,
                success,
                keep,
                priority,
                successSd, sVol, sPitch,
                failureSd, fVol, fPitch
        );

        return new ConversionBuilder(rule);
    }

    private static SoundEvent parseSound(String soundName) {
        if (soundName == null || soundName.isEmpty()) return null;
        ResourceLocation res = new ResourceLocation(soundName);
        return SoundEvent.REGISTRY.getObject(res);
    }

    public static class ConditionInfo {
        public final ICondition condition;
        public final String type;
        public final String description;

        public ConditionInfo(ICondition condition, String type, String description) {
            this.condition = condition;
            this.type = type;
            this.description = description;
        }
    }

    public static class ConversionRule implements Comparable<ConversionRule> {
        public final Block triggerBlock;
        public final float radius;
        public final List<ConsumedItem> consumedItems;
        public final float displacement;
        public final boolean additionalProducts;
        public final float successChance;
        public final float keepBlockChance;
        public final List<ItemStack> outputs;
        public final List<ICondition> conditions = new ArrayList<>();
        public final List<ConditionInfo> conditionInfos = new ArrayList<>();
        final int priority;
        SoundEvent successSound;
        float successVolume;
        float successPitch;
        SoundEvent failureSound;
        float failureVolume;
        float failurePitch;

        public ConversionRule(Block trigger, List<ConsumedItem> consumedItems, float radius,
                              float displacement, boolean additionalProducts, List<ItemStack> output, float success, float keep, int priority,
                              SoundEvent successSound, float successVolume, float successPitch,
                              SoundEvent failureSound, float failureVolume, float failurePitch) {
            this.triggerBlock = trigger;
            this.consumedItems = consumedItems;
            this.radius = radius;
            this.outputs = output;
            this.displacement = displacement;
            this.additionalProducts = additionalProducts;
            this.successChance = success;
            this.keepBlockChance = keep;
            this.priority = priority;
            this.successSound = successSound;
            this.successVolume = successVolume;
            this.successPitch = successPitch;
            this.failureSound = failureSound;
            this.failureVolume = failureVolume;
            this.failurePitch = failurePitch;
        }

        public void playSuccessSound(World world, BlockPos pos) {
            if (successSound != null) {
                world.playSound(null, pos, successSound, SoundCategory.BLOCKS,
                        successVolume, successPitch);
            }
        }

        public void playFailureSound(World world, BlockPos pos) {
            if (failureSound != null) {
                world.playSound(null, pos, failureSound, SoundCategory.BLOCKS,
                        failureVolume, failurePitch);
            }
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

        @ZenMethod
        public ConversionBuilder addBiomeCondition(String biomeId) {
            ICondition condition = (world, pos) ->
                    world.getBiome(pos).getRegistryName()
                            .equals(new ResourceLocation(biomeId));
            rule.conditions.add(condition);
            rule.conditionInfos.add(new ConditionInfo(condition, "biome", biomeId));
            return this;
        }

        @ZenMethod
        public ConversionBuilder setSuccessSound(String soundName,
                                                 @Optional float volume,
                                                 @Optional float pitch) {
            this.rule.successSound = parseSound(soundName);
            this.rule.successVolume = MathHelper.clamp(volume, 0.1f, 2.0f);
            this.rule.successPitch = MathHelper.clamp(pitch, 0.5f, 2.0f);
            return this;
        }

        @ZenMethod
        public ConversionBuilder setFailureSound(String soundName,
                                                 @Optional float volume,
                                                 @Optional float pitch) {
            this.rule.failureSound = parseSound(soundName);
            this.rule.failureVolume = MathHelper.clamp(volume, 0.1f, 2.0f);
            this.rule.failurePitch = MathHelper.clamp(pitch, 0.5f, 2.0f);
            return this;
        }

        @ZenMethod
        public ConversionBuilder addTimeCondition(int min, @Optional int max) {
            int finalMax = max >= 0 ? max : min;
            ICondition condition = (world, pos) -> {
                long time = world.getWorldTime() % 24000;
                return time >= min && time <= finalMax;
            };
            rule.conditions.add(condition);

            String timeKey;
            if (min >= 0 && finalMax <= 6000) {
                timeKey = "morning";
            } else if (min >= 6000 && finalMax <= 12000) {
                timeKey = "day";
            } else if (min >= 12000 && finalMax <= 18000) {
                timeKey = "evening";
            } else if (min >= 18000 || finalMax <= 6000) {
                timeKey = "night";
            } else {
                timeKey = "custom:" + min + "-" + finalMax;
            }
            rule.conditionInfos.add(new ConditionInfo(condition, "time", timeKey));
            return this;
        }

        @ZenMethod
        public ConversionBuilder addMoonPhaseCondition(int moonPhase) {
            ICondition condition = ((world, pos) -> moonPhase == world.getMoonPhase());
            rule.conditions.add(condition);

            rule.conditionInfos.add(new ConditionInfo(condition, "moon", String.valueOf(moonPhase)));
            return this;
        }

        @ZenMethod
        public ConversionBuilder addHeightCondition(int min, @Optional int max) {
            int finalMax = max >= 0 ? max : min;
            ICondition condition = (world, pos) -> {
                int height = pos.getY();
                return height >= min && height <= finalMax;
            };
            rule.conditions.add(condition);
            rule.conditionInfos.add(new ConditionInfo(condition, "height", min + "-" + finalMax));
            return this;
        }

        @ZenMethod
        public ConversionBuilder addWeatherCondition(
                @Optional boolean requireRaining,
                @Optional boolean requireThundering) {

            if (requireThundering && !requireRaining) {
                CraftTweakerAPI.logWarning("Thundering requires raining! Auto-correcting...");
                requireRaining = true;
            }

            final boolean finalRequireRaining = requireRaining;
            final boolean finalRequireThundering = requireThundering;

            ICondition condition = (world, pos) -> {
                boolean isRaining = world.isRaining();
                boolean isThundering = world.isThundering();

                return (finalRequireThundering && isThundering) ||
                        (finalRequireRaining && isRaining && !finalRequireThundering) ||
                        (!finalRequireRaining && !isRaining);
            };
            rule.conditions.add(condition);

            String weatherKey;
            if (finalRequireThundering) {
                weatherKey = "thunder";
            } else if (finalRequireRaining) {
                weatherKey = "rain";
            } else {
                weatherKey = "clear";
            }
            rule.conditionInfos.add(new ConditionInfo(condition, "weather", weatherKey));
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