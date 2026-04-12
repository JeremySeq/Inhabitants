package com.jeremyseq.inhabitants.blocks.impaler_head;

import com.jeremyseq.inhabitants.blocks.entity.ModBlockEntities;

import net.minecraft.core.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.RotationSegment;

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import org.jetbrains.annotations.*;

import java.util.List;

public class ImpalerHeadBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int attackTimer = -1;
    private ResourceLocation noteBlockSound = ResourceLocation
        .fromNamespaceAndPath("inhabitants", "impaler.idle");

    public ImpalerHeadBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.IMPALER_HEAD_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> {
            if (state.getAnimatable().getBlockState().getValue(AbstractImpalerHeadBlock.POWERED)) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("attack"));
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        super.saveAdditional(pTag);
        if (this.noteBlockSound != null) {
            pTag.putString("note_block_sound", this.noteBlockSound.toString());
        }
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains("note_block_sound", 8)) {
            this.noteBlockSound = ResourceLocation
                .tryParse(pTag.getString("note_block_sound"));
        }
    }

    @Nullable
    public ResourceLocation getNoteBlockSound() {
        return this.noteBlockSound;
    }

    public void setNoteBlockSound(@Nullable ResourceLocation pNoteBlockSound) {
        this.noteBlockSound = pNoteBlockSound;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return super.getUpdateTag();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static void tick(
        Level level,
        BlockPos pos,
        BlockState state,
        ImpalerHeadBlockEntity blockEntity
    ) {
        if (state.getValue(AbstractImpalerHeadBlock.POWERED)) {
            blockEntity.attackTimer++;

            if (blockEntity.attackTimer >= 13) {
                blockEntity.attackTimer = 0;
            }

            if (blockEntity.attackTimer == 5) {
                blockEntity.performAttack();
            }
        } else {
            blockEntity.attackTimer = -1;
        }
    }

    private void performAttack() {
        if (this.level == null || this.level.isClientSide) return;

        BlockPos pos = this.getBlockPos();
        BlockState state = this.level.getBlockState(pos);
        Direction facing;
        
        if (state.hasProperty(ImpalerWallHeadBlock.FACING)) {
            facing = state.getValue(ImpalerWallHeadBlock.FACING).getOpposite();
        } else if (state.hasProperty(ImpalerHeadBlock.ROTATION)) {
            int rotation = state.getValue(ImpalerHeadBlock.ROTATION);
            facing = Direction.fromYRot(RotationSegment.convertToDegrees(rotation)).getOpposite();
        } else {
            return;
        }

        BlockPos targetPos = pos.relative(facing);
        AABB area = new AABB(targetPos).inflate(0.25D);
        
        List<LivingEntity> entities = this.level.getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity entity : entities) {
            entity.hurt(this.level.damageSources().magic(), 1.0F);
        }
    }
}
