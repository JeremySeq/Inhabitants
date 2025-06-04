package com.jeremyseq.inhabitants.entities.bogre;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class BogreEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public static float FORGET_RANGE = 35f;
    public static float ROAR_RANGE = 24f;
    public static float HOSTILE_RANGE = 18f;

    private Player hostilePlayer = null; // the player the Bogre is currently hostile towards

    private static final int ROAR_TICKS = 100; // how long a roar animation lasts (server)
    private boolean roaring = false;
    private int roaringTick = 0;
    private final List<Player> warnedPlayers = new ArrayList<>();

    public BogreEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier setAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0F)
                .add(Attributes.ATTACK_DAMAGE, 30F)
                .add(Attributes.ATTACK_SPEED, 1.0f)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5F)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0F)
                .add(Attributes.MOVEMENT_SPEED, .3f).build();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
        return PlayState.CONTINUE;
    }

    @Override
    public void customServerAiStep() {

        if (hostilePlayer != null) {
            // TODO: attack the hostile player, if they leave FORGET_RANGE, forget them
        }

        // TODO: search for players within HOSTILE_RANGE, if found, set hostilePlayer to that player

        // search for players within ROAR_RANGE
        if (!roaring) {
            List<Player> withinRoarRange = this.level().getEntitiesOfClass(Player.class,
                    getBoundingBox().inflate(ROAR_RANGE), Predicate.not(Player::isSpectator));

            // sort players by distance to the Bogre, closest to farthest
            withinRoarRange.sort((p1, p2) -> Float.compare(p1.distanceTo(this), p2.distanceTo(this)));
            for (Player player : withinRoarRange) {
                if (!warnedPlayers.contains(player) && player.distanceTo(this) <= ROAR_RANGE && this.hasLineOfSight(player)) {
                    player.sendSystemMessage(Component.literal("The Bogre roars at you!"));
                    roaring = true; // TODO: set roaring to false on server after some ticks
                    warnedPlayers.add(player);
                    break;
                }
            }
        } else {
            roaringTick++;
            if (roaringTick >= ROAR_TICKS) {
                roaring = false;
                roaringTick = 0;
            }
        }

        // prune warnedPlayers list
        warnedPlayers.removeIf(player -> player.distanceTo(this) > FORGET_RANGE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
