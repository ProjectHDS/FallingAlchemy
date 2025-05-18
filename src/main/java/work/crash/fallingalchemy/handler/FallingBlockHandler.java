package work.crash.fallingalchemy.handler;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import work.crash.fallingalchemy.modsupport.FallingAlchemyTweaker;
import work.crash.fallingalchemy.item.ConsumedItem;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;

import static work.crash.fallingalchemy.Tags.MOD_ID;
import static work.crash.fallingalchemy.utils.Math.*;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class FallingBlockHandler {

    private static final List<WeakReference<EntityFallingBlock>> trackedBlocks = new ArrayList<>();
    private final Dictionary<EntityFallingBlock, BlockPos> blockPosition = new Hashtable<>();

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityFallingBlock block) {
            Block blockType = block.getBlock().getBlock();
            if (FallingAlchemyTweaker.RULES.containsKey(blockType)) {
                trackedBlocks.add(new WeakReference<>(block));
                blockPosition.put(block, block.getPosition());
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            Iterator<WeakReference<EntityFallingBlock>> iterator = trackedBlocks.iterator();

            while (iterator.hasNext()) {
                EntityFallingBlock block = iterator.next().get();
                if (block == null || block.isDead) {
                    BlockPos pos = new BlockPos(block.posX, block.posY, block.posZ);
                    if (event.world.getBlockState(pos).getBlock() == block.getBlock().getBlock()) {
                        processConversion(event.world, blockPosition.get(block), pos, block.getBlock().getBlock());
                    }
                    iterator.remove();
                }
            }
        }
    }

    private void processConversion(World world, BlockPos oriPos, BlockPos pos, Block triggerBlock) {
        List<FallingAlchemyTweaker.ConversionRule> rules = FallingAlchemyTweaker.RULES.getOrDefault(triggerBlock, Collections.emptyList());

        List<FallingAlchemyTweaker.ConversionRule> sortedRules = rules.stream().sorted(FallingAlchemyTweaker.ConversionRule::compareTo).collect(Collectors.toList());

        ruleLoop:
        for (FallingAlchemyTweaker.ConversionRule rule : sortedRules) {
            if (!rule.conditions.stream().allMatch(cond -> cond.test(world, pos))) continue;

            AxisAlignedBB area = new AxisAlignedBB(pos).grow(rule.radius);
            List<EntityItem> allItems = world.getEntitiesWithinAABB(EntityItem.class, area);

            // 计算每个消耗品的可用次数
            int multiplier = Integer.MAX_VALUE;
            for (ConsumedItem consumed : rule.consumedItems) {
                int total = 0;
                for (EntityItem item : allItems) {
                    if (consumed.matches(item.getItem())) {
                        total += item.getItem().getCount();
                    }
                }
                if (total < consumed.requiredCount) {
                    continue ruleLoop; // 不满足条件，跳过该规则
                }
                multiplier = Math.min(multiplier, total / consumed.requiredCount);
            }

            if (multiplier <= 0) continue;
            // 避免无消耗物时multiplier异常
            if (rule.consumedItems.isEmpty()) {
                multiplier = 1;
            }

            // 计算位移是否满足要求
            if (positionDistance(oriPos, pos) < rule.displacement) {
                continue;
            }

            // 扣除每个消耗品
            for (ConsumedItem consumed : rule.consumedItems) {
                int required = consumed.requiredCount * multiplier;
                List<EntityItem> matchingItems = allItems.stream().filter(item -> consumed.matches(item.getItem())).collect(Collectors.toList());

                int remaining = required;
                for (EntityItem item : matchingItems) {
                    ItemStack stack = item.getItem();
                    int taken = Math.min(remaining, stack.getCount());
                    stack.shrink(taken);
                    remaining -= taken;

                    if (stack.isEmpty()) {
                        item.setDead();
                    } else {
                        item.setItem(stack);
                    }

                    if (remaining <= 0) break;
                }
            }

            if (world.rand.nextFloat() > rule.successChance) {
                rule.playFailureSound(world, pos); // 成功率判定失败
                continue;
            }

            // 生成产物
            int finalMultiplier = multiplier;
            rule.outputs.forEach(output -> {
                ItemStack spawnedStack = output.copy();
                //
                if (rule.displacement > 0 && positionDistance(oriPos, pos) > rule.displacement && rule.additionalProducts) {
                    spawnedStack.setCount((int) ((positionDistance(oriPos, pos) - rule.displacement) + (spawnedStack.getCount() * finalMultiplier)));
                } else {
                    spawnedStack.setCount(spawnedStack.getCount() * finalMultiplier);
                }
                EntityItem newItem = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, spawnedStack);
                newItem.setDefaultPickupDelay();
                newItem.motionY = 0.1;
                world.spawnEntity(newItem);
            });
            rule.playSuccessSound(world, pos);
            // 是否消耗方块
            if (world.rand.nextFloat() >= rule.keepBlockChance) {
                world.setBlockToAir(pos);
            }

            break;
        }
    }
}