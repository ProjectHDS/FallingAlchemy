package work.crash.fallingalchemy.handler;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import work.crash.fallingalchemy.event.FallingConversionEvent;
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
                if (blockPosition.get(block) == null) {
                    trackedBlocks.add(new WeakReference<>(block));
                    blockPosition.put(block, block.getPosition());
                }
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
                    if (block != null) {
                        BlockPos pos = new BlockPos(block.posX, block.posY, block.posZ);
                        IBlockState state = event.world.getBlockState(pos);
                        BlockPos originPos = blockPosition.remove(block);
                        if (originPos != null && state.getBlock() == block.getBlock().getBlock()) {
                            processConversion(event.world, originPos, pos, state);
                        }
                    }
                    iterator.remove();
                }
            }
        }
    }

    private void processConversion(World world, BlockPos oriPos, BlockPos pos, IBlockState state) {
        Block triggerBlock = state.getBlock();
        List<FallingAlchemyTweaker.ConversionRule> rules = FallingAlchemyTweaker.RULES.getOrDefault(triggerBlock, Collections.emptyList());

        if (rules.isEmpty()) return;

        List<FallingAlchemyTweaker.ConversionRule> sortedRules = rules.stream().sorted(FallingAlchemyTweaker.ConversionRule::compareTo).collect(Collectors.toList());

        ruleLoop:
        for (FallingAlchemyTweaker.ConversionRule rule : sortedRules) {
            if (!rule.conditions.stream().allMatch(cond -> cond.test(world, pos))) {
                continue;
            }

            AxisAlignedBB area = new AxisAlignedBB(pos).grow(rule.radius);
            List<EntityItem> allItems = world.getEntitiesWithinAABB(EntityItem.class, area);

            if (rule.rescueItems) {
                for (EntityItem entityItem : allItems) {
                    if (entityItem.isDead) {
                        entityItem.isDead = false;
                    }
                }
            }

            int multiplier = Integer.MAX_VALUE;
            for (ConsumedItem consumed : rule.consumedItems) {
                int total = 0;
                for (EntityItem item : allItems) {
                    if (consumed.matches(item.getItem())) {
                        total += item.getItem().getCount();
                    }
                }

                if (total < consumed.requiredCount) {
                    continue ruleLoop;
                }
                multiplier = Math.min(multiplier, total / consumed.requiredCount);
            }

            if (multiplier <= 0) continue;
            if (rule.consumedItems.isEmpty()) {
                multiplier = 1;
            }

            if (rule.onlyOne) {
                multiplier = 1;
            }

            if (positionDistance(oriPos, pos) < rule.displacement) {
                continue;
            }

            List<ItemStack> preparedOutputs = new ArrayList<>();
            int finalMultiplier = multiplier;
            rule.outputs.forEach(output -> {
                ItemStack spawnedStack = output.copy();
                if (rule.displacement > 0 && positionDistance(oriPos, pos) > rule.displacement && rule.additionalProducts) {
                    spawnedStack.setCount((int) ((positionDistance(oriPos, pos) - rule.displacement) + (spawnedStack.getCount() * finalMultiplier)));
                } else {
                    spawnedStack.setCount(spawnedStack.getCount() * finalMultiplier);
                }
                preparedOutputs.add(spawnedStack);
            });

            FallingConversionEvent.Pre preEvent = new FallingConversionEvent.Pre(world, pos, state, preparedOutputs, rule);
            if (MinecraftForge.EVENT_BUS.post(preEvent)) {
                continue;
            }

            for (ConsumedItem consumed : rule.consumedItems) {
                int required = consumed.requiredCount * multiplier;
                List<EntityItem> matchingItems = allItems.stream().filter(item -> consumed.matches(item.getItem())).collect(Collectors.toList());

                int remainingToConsume = required;
                for (EntityItem item : matchingItems) {
                    ItemStack stack = item.getItem();
                    int originalCount = stack.getCount();
                    boolean wasDead = item.isDead;

                    int taken = Math.min(remainingToConsume, originalCount);
                    stack.shrink(taken);
                    remainingToConsume -= taken;

                    if (stack.isEmpty()) {
                        item.setDead();
                    } else {
                        item.setItem(stack);
                        if (rule.rescueItems && wasDead) {
                            EntityItem newEntity = new EntityItem(world, item.posX, item.posY, item.posZ, stack.copy());
                            newEntity.motionX = item.motionX;
                            newEntity.motionY = item.motionY;
                            newEntity.motionZ = item.motionZ;
                            newEntity.setDefaultPickupDelay();
                            newEntity.setNoDespawn();
                            world.spawnEntity(newEntity);
                        }
                    }

                    if (remainingToConsume <= 0) break;
                }
            }

            if (world.rand.nextFloat() > rule.successChance) {
                rule.playFailureSound(world, pos);
                continue;
            }

            preEvent.getOutputs().forEach(stack -> {
                EntityItem newItem = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, stack);
                newItem.setDefaultPickupDelay();
                newItem.motionY = 0.1;
                if (rule.rescueItems) {
                    newItem.setNoDespawn();
                }
                world.spawnEntity(newItem);
            });

            rule.playSuccessSound(world, pos);
            MinecraftForge.EVENT_BUS.post(new FallingConversionEvent.Post(world, pos, state, preEvent.getOutputs(), rule));

            if (world.rand.nextFloat() >= rule.keepBlockChance) {
                world.setBlockToAir(pos);
            }

            break;
        }
    }
}