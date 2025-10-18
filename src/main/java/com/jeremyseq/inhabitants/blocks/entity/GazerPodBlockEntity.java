package com.jeremyseq.inhabitants.blocks.entity;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.blocks.GazerPodBlockRegistry;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.entities.gazer.GazerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class GazerPodBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private boolean hasGazer = true;
    private int tickCounter = 0;

    public GazerPodBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.GAZER_POD_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        GazerPodBlockRegistry.register(this.getBlockPos());
    }

    @Override
    public void setRemoved() {
        GazerPodBlockRegistry.unregister(this.getBlockPos());
        super.setRemoved();
    }

    public boolean hasGazer() {
        return hasGazer;
    }
    public void setHasGazer(boolean value) {
        if (this.hasGazer != value) {
            this.hasGazer = value;
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("HasGazer")) {
            this.hasGazer = tag.getBoolean("HasGazer");
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("HasGazer", hasGazer);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("HasGazer", hasGazer);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains("HasGazer")) {
            this.hasGazer = tag.getBoolean("HasGazer");
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        handleUpdateTag(pkt.getTag());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public void tick() {

        if (this.getLevel() instanceof ServerLevel serverLevel) {
            tickCounter++;

            if (this.hasGazer() && tickCounter % 400 == 0) {
                Inhabitants.LOGGER.debug("Spawning gazer from pod at {}", this.getBlockPos());

                // Spawn gazer
                GazerEntity gazerEntity = ModEntities.GAZER.get().create(serverLevel);

                assert gazerEntity != null;

                gazerEntity.moveTo(this.getBlockPos().above(1), 0, 0);

                serverLevel.addFreshEntity(gazerEntity);

                gazerEntity.exitPod(false);
                this.setHasGazer(false);

                serverLevel.playSound(
                        null,
                        this.getBlockPos(),
                        SoundEvents.BEEHIVE_EXIT,
                        SoundSource.BLOCKS,
                        1.0F, 1.0F
                );
                tickCounter = 0;
            }
        }
    }
}
