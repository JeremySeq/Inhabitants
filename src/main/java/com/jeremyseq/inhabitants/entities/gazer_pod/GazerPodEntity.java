package com.jeremyseq.inhabitants.entities.gazer_pod;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.entities.gazer.GazerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.Random;

public class GazerPodEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final Random random = new Random();

    private static final EntityDataAccessor<Boolean> HAS_GAZER = SynchedEntityData.defineId(GazerPodEntity.class, EntityDataSerializers.BOOLEAN);

    public static AttributeSupplier setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10f).build();
    }

    public GazerPodEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void knockback(double pStrength, double pX, double pZ) {}

    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
//        if (!level().isClientSide) {
            if (this.hasGazer() && random.nextInt(100) == 0) {
                // Spawn gazer
                GazerEntity gazerEntity = ModEntities.GAZER.get().create(level());

                assert gazerEntity != null;

                gazerEntity.moveTo(getX(), getY() + 1, getZ(), getYRot(), 0);

                level().addFreshEntity(gazerEntity);

                Inhabitants.LOGGER.debug("Spawning gazer from pod at " + getX() + ", " + (getY() + 1) + ", " + getZ());

                this.setHasGazer(false);
                gazerEntity.setGazerState(GazerEntity.GazerState.IDLE);
            }
//            else if (!hasGazer() && random.nextInt(100) == 0) {
//                // Despawn gazer
//                spawnedGazer.discard();
//                spawnedGazer = null;
//            }
//        }
        Inhabitants.LOGGER.debug("GazerPod hasGazer: " + hasGazer() + " tick: " + this.tickCount);
    }


    public boolean hasGazer() {
        return this.entityData.get(HAS_GAZER);
    }
    public void setHasGazer(boolean value) {
        this.entityData.set(HAS_GAZER, value);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HAS_GAZER, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasGazer", hasGazer());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HasGazer")) {
            setHasGazer(tag.getBoolean("HasGazer"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
