package work.crash.fallingalchemy;

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
import work.crash.fallingalchemy.condition.ICondition;

import java.util.*;
import java.util.stream.Collectors;

import static work.crash.fallingalchemy.Reference.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class FallingBlockHandler {

    private static final List<EntityFallingBlock> trackedBlocks = new ArrayList<>();

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityFallingBlock) {
            EntityFallingBlock block = (EntityFallingBlock) event.getEntity();
            Block blockType = block.getBlock().getBlock();
            if (FallingAlchemyTweaker.RULES.containsKey(blockType)) {
                trackedBlocks.add(block);
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            var iterator = trackedBlocks.iterator();

            while (iterator.hasNext()) {
                EntityFallingBlock block = iterator.next();
                if (block.isDead) {
                    BlockPos pos = new BlockPos(block.posX, block.posY, block.posZ);
                    if (event.world.getBlockState(pos).getBlock() == block.getBlock().getBlock()) {
                        processConversion(event.world, pos, block.getBlock().getBlock());
                    }
                    iterator.remove();
                }
            }
        }
    }

    private static void processConversion(World world, BlockPos pos, Block triggerBlock) {
        List<FallingAlchemyTweaker.ConversionRule> rules = FallingAlchemyTweaker.RULES.getOrDefault(triggerBlock, Collections.emptyList());
        rules: for (FallingAlchemyTweaker.ConversionRule rule : rules) {
            // 条件检查
            for (ICondition condition : rule.conditions) {
                if (!condition.test(world, pos)) {
                    continue rules;
                }
            }

            // 成功率判定
            boolean success = world.rand.nextFloat() <= rule.successChance;
            //spawnParticles(world, pos, success ? rule.successParticles : rule.failParticles);

            if (!success) {
                continue;
            }

            // 消耗品检测
            AxisAlignedBB area = new AxisAlignedBB(pos).grow(rule.radius);
            List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, area)
                    .stream()
                    .filter(e -> e.getItem().getItem() == rule.consumedItem)
                    .collect(Collectors.toList());

            int total = items.stream().mapToInt(e -> e.getItem().getCount()).sum();
            if (total < rule.requiredCount) {
                continue;
            }

            // 执行转换逻辑
            int remaining = rule.requiredCount;
            for (EntityItem item : items) {
                ItemStack stack = item.getItem();
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;

                if (stack.isEmpty()) {
                    item.setDead();
                } else {
                    item.setItem(stack); // 更新剩余数量
                }

                if (remaining <= 0) break;
            }

            // 生成所有产物
            rule.outputs.forEach(output -> {
                // 数量计算（考虑成功率）
                int multiplier = (total / rule.requiredCount) * (world.rand.nextFloat() <= rule.successChance ? 1 : 0);
                ItemStack spawnedStack = output.copy();
                spawnedStack.setCount(spawnedStack.getCount() * multiplier);

                EntityItem newItem = new EntityItem(
                        world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                        spawnedStack
                );
                newItem.setDefaultPickupDelay();
                world.spawnEntity(newItem);
            });

            // 方块保留判定
            if (world.rand.nextFloat() >= rule.keepBlockChance) {
                world.setBlockToAir(pos);
            }
            break;
        }
    }

    /*private static void spawnParticles(World world, BlockPos pos, ParticleSettings settings) {
        if (settings == null) return;

        for (int i = 0; i < settings.count; i++) {
            double xOffset = world.rand.nextGaussian() * 0.2;
            double yOffset = world.rand.nextGaussian() * 0.2;
            double zOffset = world.rand.nextGaussian() * 0.2;

            double x = pos.getX() + 0.5 + xOffset;
            double y = pos.getY() + 1 + yOffset;
            double z = pos.getZ() + 0.5 + zOffset;

            switch (settings.type.getParticleID()) {
                case 2: // REDSTONE
                    float r = settings.color[0] / 255.0f;
                    float g = settings.color[1] / 255.0f;
                    float b = settings.color[2] / 255.0f;
                    world.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, r, g, b);
                    break;
                default:
                    world.spawnParticle(settings.type, x, y, z, 0, 0, 0);
                    break;
            }
        }
    }*/
}
