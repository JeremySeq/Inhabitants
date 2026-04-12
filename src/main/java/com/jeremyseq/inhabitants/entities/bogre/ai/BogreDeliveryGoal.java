package com.jeremyseq.inhabitants.entities.bogre.ai;

import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.entities.bogre.skill.CarvingSkill;
import com.jeremyseq.inhabitants.entities.bogre.skill.CookingSkill;
import com.jeremyseq.inhabitants.entities.bogre.utilities.BogreDetectionHelper;
import com.jeremyseq.inhabitants.recipe.IBogreRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

public class BogreDeliveryGoal {
    public static final float DELIVER_DISTANCE = 3.5f;

    private BogreDeliveryGoal() {
    }

    public static void handleDelivery(BogreEntity bogre) {
        BogreAi.DeliveryState state = bogre.getAi().getDeliveryState();

        if (state == BogreAi.DeliveryState.SEARCHING) {
            Player owner = getOwnerPlayer(bogre);

            if (owner != null && owner.isAlive()) {
                bogre.getAi().setDeliveryState(BogreAi.DeliveryState.DELIVERING_TO_PLAYER);
                handlePlayerDelivery(bogre, owner);
            } else {
                handleChestDelivery(bogre);
            }
        } else if (state == BogreAi.DeliveryState.DELIVERING_TO_PLAYER) {
            Player owner = getOwnerPlayer(bogre);
            if (owner != null && owner.isAlive()) {
                handlePlayerDelivery(bogre, owner);
            } else {
                bogre.getAi().setDeliveryState(BogreAi.DeliveryState.SEARCHING);
                handleChestDelivery(bogre);
            }
        } else {
            handleChestDelivery(bogre);
        }
    }

    @Nullable
    private static Player getOwnerPlayer(BogreEntity bogre) {
        UUID ownerUUID = bogre.getAi().getResultOwnerUUID();
        if (ownerUUID != null) {
            Player owner = bogre.level().getPlayerByUUID(ownerUUID);
            if (owner != null && owner.isAlive() && !owner.isSpectator() && bogre.distanceTo(owner) <= 30.0) {
                return owner;
            }
        }
        return null;
    }

    private static void handlePlayerDelivery(BogreEntity bogre, Player player) {
        bogre.getEntityData().set(BogreEntity.TARGET_POS, BlockPos.ZERO);

        double distance = bogre.distanceToSqr(player);
        float stopDistance = 3.5f;

        IBogreRecipe activeRecipe = bogre.getAi().getActiveRecipe();
        if (activeRecipe != null) {
            if (activeRecipe.getBogreRecipeType() == IBogreRecipe.Type.COOKING) {
                stopDistance = CookingSkill.dropResultOffset;
            } else if (activeRecipe.getBogreRecipeType() == IBogreRecipe.Type.CARVING) {
                stopDistance = CarvingSkill.dropResultOffset;
            }
        }

        if (distance > stopDistance * stopDistance) {
            if (bogre.getNavigation().isDone() || bogre.tickCount % 10 == 0) {
                bogre.getNavigation().moveTo(player, 1.0D);
            }
            bogre.resetCookingTicks();
            return;
        }

        bogre.getNavigation().stop();
        bogre.getLookControl().setLookAt(player, 30.0F, 30.0F);

        exitItemDelivering(bogre);
    }

    private static void handleChestDelivery(BogreEntity bogre) {
        BogreAi.DeliveryState state = bogre.getAi().getDeliveryState();
        BlockPos chestPos = bogre.getEntityData().get(BogreEntity.TARGET_POS);

        if (state == BogreAi.DeliveryState.SEARCHING) {
            Optional<BlockPos> foundChest = BogreDetectionHelper.findChestWithSpace(bogre, 15, 5);

            if (foundChest.isPresent()) {
                bogre.getEntityData().set(BogreEntity.TARGET_POS, foundChest.get());
                bogre.getAi().setDeliveryState(BogreAi.DeliveryState.MOVING_TO_CHEST);
                bogre.resetCookingTicks();
            } else {
                exitItemDelivering(bogre);
            }
            return;
        }

        if (chestPos.equals(BlockPos.ZERO) || !isValidChest(bogre, chestPos)) {
            bogre.getEntityData().set(BogreEntity.TARGET_POS, BlockPos.ZERO);
            bogre.getAi().setDeliveryState(BogreAi.DeliveryState.SEARCHING);
            bogre.resetCookingTicks();
            return;
        }

        if (state == BogreAi.DeliveryState.MOVING_TO_CHEST) {
            double distSq = bogre.distanceToSqr(Vec3.atCenterOf(chestPos));
            if (distSq > DELIVER_DISTANCE * DELIVER_DISTANCE) {
                if (bogre.getNavigation().isDone() || bogre.tickCount % 10 == 0) {
                    bogre.getNavigation().moveTo(chestPos.getX(), chestPos.getY(), chestPos.getZ(), 1.0D);
                }
                bogre.resetCookingTicks();
            } else {
                bogre.getNavigation().stop();
                bogre.getAi().setDeliveryState(BogreAi.DeliveryState.OPENING_CHEST);
                bogre.resetCookingTicks();
            }
            return;
        }

        bogre.getLookControl().setLookAt(Vec3.atCenterOf(chestPos));
        bogre.resetStuckTicks();

        if (state == BogreAi.DeliveryState.OPENING_CHEST) {
            if (bogre.getCookingTicks() == 0) {
                BogreAi.playAnimation(bogre, "grab");
                toggleChest(bogre, true, true);
            } else {
                toggleChest(bogre, true, false);
            }
            if (bogre.getCookingTicks() >= CookingSkill.chestOpeningDuration) {
                bogre.getAi().setDeliveryState(BogreAi.DeliveryState.DEPOSITING);
                bogre.resetCookingTicks();
            } else {
                bogre.incrementCookingTicks();
            }
            
            return;
        } else if (state == BogreAi.DeliveryState.DEPOSITING) {
            toggleChest(bogre, true, false);
            if (bogre.getCookingTicks() == 0) {
                BlockEntity blockEntity = bogre.level().getBlockEntity(chestPos);
                if (blockEntity == null) {
                    bogre.getEntityData().set(BogreEntity.TARGET_POS, BlockPos.ZERO);
                    bogre.getAi().setDeliveryState(BogreAi.DeliveryState.SEARCHING);
                    bogre.resetCookingTicks();
                    return;
                }

                LazyOptional<IItemHandler> cap = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER);
                IItemHandler handler = cap.orElse(null);
                if (handler == null) {
                    bogre.getEntityData().set(BogreEntity.TARGET_POS, BlockPos.ZERO);
                    bogre.getAi().setDeliveryState(BogreAi.DeliveryState.SEARCHING);
                    bogre.resetCookingTicks();
                    return;
                }

                ItemStack original = bogre.getItemHeld().copy();
                ItemStack held = original.copy();

                int emptySlot = findFirstEmptySlot(handler);
                if (!held.isEmpty() && emptySlot >= 0) {
                    held = handler.insertItem(emptySlot, held, false);
                }

                bogre.setItemHeld(held, false);

                if (held.getCount() == original.getCount()) {
                    bogre.getAi().incrementDeliveryFailures();
                    bogre.getEntityData().set(BogreEntity.TARGET_POS, BlockPos.ZERO);
                    bogre.getAi().setDeliveryState(BogreAi.DeliveryState.SEARCHING);
                    bogre.resetCookingTicks();
                    return;
                }

                BogreAi.playAnimation(bogre, "grab");
                bogre.level().playSound(null, bogre.getX(), bogre.getY(), bogre.getZ(),
                        ModSoundEvents.BOGRE_DEPOSIT_IN_CHEST.get(), SoundSource.NEUTRAL, 0.25F, 1.0F);
            }
            if (bogre.getCookingTicks() >= CookingSkill.chestDepositingDuration) {
                bogre.getAi().setDeliveryState(BogreAi.DeliveryState.CLOSING_CHEST);
                bogre.resetCookingTicks();
            } else {
                bogre.incrementCookingTicks();
            }
            return;
        } else if (state == BogreAi.DeliveryState.CLOSING_CHEST) {
            if (bogre.getCookingTicks() == 0) {
                BogreAi.playAnimation(bogre, "grab");
                toggleChest(bogre, false, true);
            }

            if (bogre.getCookingTicks() >= CookingSkill.chestClosingDuration) {
                bogre.getEntityData().set(BogreEntity.TARGET_POS, BlockPos.ZERO);
                exitItemDelivering(bogre);
            } else {
                bogre.incrementCookingTicks();
            }
        }
    }

    public static void exitItemDelivering(BogreEntity bogre) {
        if (!bogre.getItemHeld().isEmpty()) {
            bogre.throwHeldItem();
        }
        finishSkilling(bogre);
    }

    public static boolean isValidChest(BogreEntity bogre, BlockPos pos) {
        BlockState state = bogre.level().getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return false;
        }

        ItemStack held = bogre.getItemHeld();
        if (held.isEmpty()) {
            return true;
        }

        return BogreDetectionHelper.hasAccessibleEmptyChestSlot(bogre.level(), pos, state);
    }

    private static void finishSkilling(BogreEntity bogre) {
        bogre.setAIState(BogreAi.State.NEUTRAL);
        bogre.setCraftingState(BogreAi.SkillingState.NONE);
        bogre.getAi().setDeliveryState(BogreAi.DeliveryState.SEARCHING);
        bogre.getAi().setActiveRecipe(null);
        bogre.getAi().setResultOwnerUUID(null);
        bogre.getAi().setPathSet(false);
        bogre.getAi().resetStuckTicks();
        bogre.setItemHeld(ItemStack.EMPTY, false);
        bogre.getAi().setDeliveryFailures(0);
    }

    private static void toggleChest(BogreEntity bogre, boolean open, boolean playSound) {
        BlockPos chestPos = bogre.getEntityData().get(BogreEntity.TARGET_POS);
        if (chestPos.equals(BlockPos.ZERO)) {
            return;
        }

        BlockState state = bogre.level().getBlockState(chestPos);
        Block block = state.getBlock();
        bogre.level().blockEvent(chestPos, block, 1, open ? 1 : 0);
        bogre.level().sendBlockUpdated(chestPos, state, state, Block.UPDATE_CLIENTS);

        if (state.hasProperty(ChestBlock.TYPE)) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                BlockPos pairPos = chestPos.relative(ChestBlock.getConnectedDirection(state));
                BlockState pairState = bogre.level().getBlockState(pairPos);
                if (pairState.is(block)) {
                    bogre.level().blockEvent(pairPos, pairState.getBlock(), 1, open ? 1 : 0);
                    bogre.level().sendBlockUpdated(pairPos, pairState, pairState, Block.UPDATE_CLIENTS);
                }
            }
        }

        if (playSound) {
            bogre.level().playSound(null, chestPos, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE,
                    SoundSource.BLOCKS, 0.5F, bogre.level().random.nextFloat() * 0.1F + 0.9F);
        }
    }

    private static int findFirstEmptySlot(IItemHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

}
