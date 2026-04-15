package com.jeremyseq.inhabitants.entities.bogre.bogre_cauldron;

import com.jeremyseq.inhabitants.blocks.ModBlocks;
import com.jeremyseq.inhabitants.items.ModItems;
import com.jeremyseq.inhabitants.gui.cauldron.CauldronMenu;
import com.jeremyseq.inhabitants.recipe.BogreRecipeManager;
import com.jeremyseq.inhabitants.recipe.CookingRecipe;
import com.jeremyseq.inhabitants.entities.bogre.ai.BogreAi;
import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;
import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.entities.bogre.utilities.BogreDetectionHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import org.jetbrains.annotations.*;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

import java.util.*;

public class BogreCauldronEntity extends Entity implements GeoEntity, MenuProvider {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private boolean placedBlock = false;
    private float health;

    private static final EntityDataAccessor<Boolean> COOKING =
    SynchedEntityData.defineId(BogreCauldronEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Integer> COOKING_PROGRESS =
    SynchedEntityData.defineId(BogreCauldronEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Boolean> HAS_HEAT_SOURCE =
    SynchedEntityData.defineId(BogreCauldronEntity.class, EntityDataSerializers.BOOLEAN);

    // 5 slots: 4 for ingredients, 1 for the container (bowl)/output
    private final ItemStackHandler itemHandler = new ItemStackHandler(5) {
        @Override
        public int getSlotLimit(int slot) {
            return 16;
        }
    };
    private final LazyOptional<IItemHandler> optionalItemHandler =
        LazyOptional.of(() -> itemHandler);

    private static final TagKey<Block> CAULDRON_HEAT_SOURCES =
        TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "cauldron_heat_sources"));

    public BogreCauldronEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.health = 5f;
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide) {
            if (level().getBlockState(this.blockPosition()).is(ModBlocks.INVISIBLE_CAULDRON_BLOCK.get())) {
                level().removeBlock(this.blockPosition(), false);
            }
        }
    }

    @Override
    public @NotNull InteractionResult interact(Player pPlayer, @NotNull InteractionHand pHand) {
        // allows creative player to rotate the cauldron (just for devs)
        if (pPlayer.isCreative() && pPlayer.getItemInHand(pHand).getItem() == ModItems.GIANT_BONE.get()) {
            this.setYRot(this.rotate(Rotation.CLOCKWISE_90));
            return InteractionResult.SUCCESS;
        }

        if (!this.level().isClientSide && pPlayer instanceof ServerPlayer serverPlayer) {
            this.level().playSound(null, this.blockPosition(), ModSoundEvents.CAULDRON_GUI_OPEN.get(),
            SoundSource.BLOCKS, 1.0f, 1.0f);
            
            // open Cauldron Gui
            NetworkHooks.openScreen(serverPlayer, this, buf ->
            buf.writeBlockPos(this.blockPosition()));
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (this.level() instanceof ServerLevel serverLevel) {
            this.health -= amount;

            // play hit sound
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.0F);

            // show crit particles
            serverLevel.sendParticles(ParticleTypes.CRIT, getX() + 0.5, getY() + 0.5, getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.1);

            if (this.health <= 0) {
                // play break sound
                serverLevel.playSound(null, this.blockPosition(), SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

                // show block breaking particles (simulating stone break)
                serverLevel.levelEvent(2001, this.blockPosition(), Block.getId(Blocks.STONE.defaultBlockState()));

                this.remove(RemovalReason.KILLED);
            }
        }
        return true;
    }

    /**
     * Un-waterlogs blocks below cauldron and relights campfire if necessary.
     * This is needed because sometimes when the bogre lair is generated, water floods the slabs and extinguishes the campfire.
     */
    private void fixBlocksUnderCauldron() {
        if (level().isClientSide) return;

        BlockPos center = this.blockPosition().below();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState state = level().getBlockState(pos);

                if (state.hasProperty(BlockStateProperties.WATERLOGGED) &&
                state.getValue(BlockStateProperties.WATERLOGGED)) {
                    level().setBlock(pos,
                            state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
                }
            }
        }

        BlockState state = level().getBlockState(center);

        if (state.getBlock() instanceof CampfireBlock && !state.getValue(CampfireBlock.LIT)) {
            level().setBlock(center, state.setValue(CampfireBlock.LIT, true), 3);
        }
    }

    private void snapToBlockCenter() {
        BlockPos pos = this.blockPosition();
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY();
        double cz = pos.getZ() + 0.5D;
        this.setPos(cx, cy, cz);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount == 1 && !level().isClientSide) {
            snapToBlockCenter();
            fixBlocksUnderCauldron();
        }

        if (!placedBlock && !level().isClientSide) {
            level().setBlock(this.blockPosition(),
                    ModBlocks.INVISIBLE_CAULDRON_BLOCK.get().defaultBlockState(),
                    3);
            placedBlock = true;
        }

        if (!level().isClientSide) {
            checkHeatSourceServer();
            tickCookingLogic();
        }

        if (level().isClientSide) {
            double x = getX();
            double y = getY();
            double z = getZ();

            if (level().random.nextFloat() < 0.05f) {
                // Pick one of the four sides of the square
                int side = level().random.nextInt(4);
                double px = x;
                double pz = z;
                double dx = 0;
                double dz = 0;

                // Offset along side
                double offset = -0.9 + level().random.nextDouble() * 1.8;

                switch (side) {
                    case 0 -> { px += offset; pz -= 1.0; dz = -1; } // North
                    case 1 -> { px += offset; pz += 1.0; dz = 1;  } // South
                    case 2 -> { px -= 1.0; pz += offset; dx = -1; } // West
                    case 3 -> { px += 1.0; pz += offset; dx = 1;  } // East
                }

                double py = y + 0.05;

                // Add randomness to motion away from the center
                double vx = dx * (0.05 + level().random.nextDouble() * 0.03);
                double vz = dz * (0.05 + level().random.nextDouble() * 0.03);
                // Pop upward motion
                double vy = 0.1 + level().random.nextDouble() * 0.05;

                // Particle: same as lava pop
                level().addParticle(ParticleTypes.LAVA, px, py, pz, vx, vy, vz);
            }
            
            if (this.hasHeatSource()) {
                double px = x - 1 + level().random.nextDouble() * 2.0;
                double pz = z - 1 + level().random.nextDouble() * 2.0;
                double py = y + 1.5;

                if (this.entityData.get(COOKING)) {
                    // more bubbles when cooking + effect particles
                    if (level().random.nextFloat() < 0.5f) {
                        level().addParticle(ParticleTypes.BUBBLE_POP, px, py - 0.5, pz, 0, 0.05, 0);
                        level().addParticle(ParticleTypes.EFFECT, px, py, pz, 0, 0.05, 0);
                    }
                } else {
                    // infrequent bubbles when not cooking
                    if (level().random.nextFloat() < 0.5f) {
                        level().addParticle(ParticleTypes.BUBBLE_POP, px, py - 0.5, pz, 0, 0.05, 0);
                    }
                }
                level().playLocalSound(px, py, pz,
                    SoundEvents.FIRE_AMBIENT,
                    SoundSource.BLOCKS,
                    0.4f, 0.8f + level().random.nextFloat() * 0.4f, false);
            }

        }
    }

    public boolean isCooking() {
        return this.entityData.get(COOKING);
    }

    public void setCooking(boolean cooking) {
        this.entityData.set(COOKING, cooking);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(COOKING, false);
        entityData.define(COOKING_PROGRESS, 0);
        entityData.define(HAS_HEAT_SOURCE, false);
    }

    private void checkHeatSourceServer() {
        if (this.level() != null && this.tickCount % 10 == 0) {
            BlockState belowState = this.level().getBlockState(this.blockPosition().below());
            entityData.set(HAS_HEAT_SOURCE, belowState.is(CAULDRON_HEAT_SOURCES));
        }
    }

    private void tickCookingLogic() {
        if (!canStartCooking()) {
            entityData.set(COOKING_PROGRESS, 0);
            if (!isBeingCookedByBogre()) setCooking(false);
        }
    }

    private boolean isBeingCookedByBogre() {
        return entityData.get(COOKING);
    }

    public boolean hasHeatSource() {
        return entityData.get(HAS_HEAT_SOURCE);
    }

    public boolean isReadyToCook() {
        if (!hasHeatSource()) return false;
        
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) items.add(stack.getItem());
        }
        
        Optional<CookingRecipe> recipeOpt = BogreRecipeManager.getCookingRecipe(items);
        if (recipeOpt.isEmpty()) return false;

        ItemStack containerSlot = itemHandler.getStackInSlot(4);

        return hasContainerItem(containerSlot, recipeOpt);
    }

    public int getItemCount() {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public void setCookingProgress(int progress) {
        entityData.set(COOKING_PROGRESS, progress);
    }

    private boolean canStartCooking() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) items.add(stack.getItem());
        }
        
        Optional<CookingRecipe> recipeOpt = BogreRecipeManager.getCookingRecipe(items);
        if (recipeOpt.isEmpty()) return false;

        ItemStack container = itemHandler.getStackInSlot(4);
        return hasContainerItem(container, recipeOpt) && hasHeatSource();
    }

    public void notifyTheft(Player player) {
        if (this.level().isClientSide) return;
        
        this.setCooking(false);
        this.getEntityData().set(COOKING_PROGRESS, 0);
        
        triggerBogreAnger(player);
    }

    public void notifyInvalidIngredient(Player player) {
        if (this.level().isClientSide) return;
        
        // when invalid ingredient is added, stop cooking and reset progress
        this.setCooking(false);
        this.getEntityData().set(COOKING_PROGRESS, 0);

        triggerBogreAnger(player);
    }

    private void triggerBogreAnger(Player player) {
        Optional<BogreEntity> closest = BogreDetectionHelper.findClosestBogre(
                this.level(),
                this.position(),
                10,
                bogre -> {
                    if (bogre.getAIState() != BogreAi.State.SKILLING) return false;
                    if (bogre.cauldronPos == null || !bogre.cauldronPos.equals(this.blockPosition())) return false;
                    
                    BogreAi.SkillingState state = bogre.getCraftingState();
                    return state == BogreAi.SkillingState.COOKING || state == BogreAi.SkillingState.MOVING_TO_TARGET;
                }
        );

        if (closest.isPresent()) {
            BogreEntity bogre = closest.get();
            BogreAi.SkillingState prevState = bogre.getCraftingState();

            if (bogre.getAIState() == BogreAi.State.SKILLING) {
                bogre.getAi().interruptSkilling();
            }

            if (prevState == BogreAi.SkillingState.COOKING && !player.isCreative()) {
                var attackGoal = bogre.getAttackGoal();
                if (attackGoal != null) {
                    attackGoal.getAttackedByPlayers().add(player.getUUID());
                    bogre.setTarget(player);
                    attackGoal.enterRoaring(player);
                } else {
                    bogre.setTarget(player);
                    bogre.setAIState(BogreAi.State.AGGRESSIVE);
                }
            }
        }
    }

    public ItemStack finishCooking() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) items.add(stack.getItem());
        }
        
        Optional<CookingRecipe> recipeOpt = BogreRecipeManager.getCookingRecipe(items);
        ItemStack result = ItemStack.EMPTY;
        
        if (recipeOpt.isPresent()) {
            CookingRecipe recipe = recipeOpt.get();
            ItemStack container = itemHandler.getStackInSlot(4);
            
            if (!container.isEmpty() && container.is(recipe.container())) {
                result = recipe.result().copy();

                // consume ingredients 0-3
                for (int i = 0; i < 4; i++) {
                    itemHandler.getStackInSlot(i).shrink(1);
                }
                // consume container 4
                itemHandler.getStackInSlot(4).shrink(1);
                
                level().playSound(null, blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                SoundSource.BLOCKS, 1.0F, 0.8F);
            }
        }

        entityData.set(COOKING_PROGRESS, 0);
        return result;
    }

    public int getCookingProgress() {
        return entityData.get(COOKING_PROGRESS);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains("Inventory")) {
            CompoundTag invTag = compoundTag.getCompound("Inventory");
            int savedSize = invTag.getInt("Size");
            
            if (savedSize != 5) {
                ItemStackHandler legacyHandler = new ItemStackHandler(savedSize);
                legacyHandler.deserializeNBT(invTag);
                for (int i = 0; i < Math.min(savedSize, 5); i++) {
                    itemHandler.setStackInSlot(i, legacyHandler.getStackInSlot(i));
                }
            } else {
                itemHandler.deserializeNBT(invTag);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        compoundTag.put("Inventory", itemHandler.serializeNBT());
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap,
        @Nullable Direction side) {

        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return optionalItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        optionalItemHandler.invalidate();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.empty();
    }

    @Nullable
    @Override
    public AbstractContainerMenu
        createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new CauldronMenu(id, inventory, this.itemHandler, this.blockPosition());
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<BogreCauldronEntity> animationState) {
        animationState.getController().setAnimation(RawAnimation.begin().then("animation", Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private boolean hasContainerItem(ItemStack container, Optional<CookingRecipe> recipeOpt) {
        return recipeOpt.isPresent() &&
            !container.isEmpty() &&
            container.is(recipeOpt.get().container());
    }
}