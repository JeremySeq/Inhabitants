package com.jeremyseq.inhabitants.entities.concher.ai;

import com.jeremyseq.inhabitants.entities.concher.ConcherEntity;
import com.jeremyseq.inhabitants.debug.DevMode;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.damagesource.DamageSource;

public class ConcherAi {
    private final ConcherEntity concher;

    private boolean hasShell;
    private int blinkTimer = 0;

    private int stateTimer = 0;
    private int stateTicks = 0;
    private BlockPos targetPos = null;

    private double dangerDistance = 10.0d;

    // [speed, waterSpeed, rotationSpeed, rotationWaterSpeed]
    private static final float[][] SPEEDS = {
            { 1.0f, 2.0f, 0.4f, 0.35f }, // Stage 0
            { 0.8f, 1.6f, 0.3f, 0.25f }, // Stage 1
            { 0.75f, 1.55f, 0.2f, 0.15f }, // Stage 2
            { 0.70f, 1.5f, 0.15f, 0.1f } // Stage 3
    };

    private static final double[] speedModifiers = { 2.0, 1.0, 0.9, 0.8 };

    public enum State {
        IDLE,
        WANDERING,
        PANIC,
        SLEEPING,
        PAUSING,
        TEMPTING
    }

    public enum PanicPhase {
        HIDING_START,
        HIDING,
        HIDING_END
    }

    public enum SleepingState {
        GOING_TO_SLEEP,
        SLEEPING,
        WAKING_UP
    }

    public enum GrowthStage {
        STAGE_0, // no-shell
        STAGE_1, // small-shell
        STAGE_2, // medium-shell
        STAGE_3 // large-shell
    }

    public ConcherAi(ConcherEntity concher) {
        this.concher = concher;
    }

    public void registerGoals() {
        concher.goalSelector.addGoal(3, new TemptGoal(concher, 1.0D, Ingredient.of(Items.KELP), false) {
            @Override
            public boolean canUse() {
                return !concher.isSheddingActive() && super.canUse();
            }

            @Override
            public void start() {
                super.start();
                setState(State.TEMPTING);
            }

            @Override
            public void stop() {
                super.stop();
                if (getState() == State.TEMPTING) {
                    setState(State.IDLE);
                }
            }
        });
    }

    public void aiStep() {
        if (concher.isSheddingActive()) {
            if (!concher.getNavigation().isDone()) {
                concher.getNavigation().stop();
            }
            return;
        }

        stateTicks++;
        if (!concher.level().isClientSide()) {
            spawnPathParticles();
        }

        this.hasShell = concher.getStage() > 0;
        this.handleBlinking();

        State currentState = getState();
        if (currentState == State.IDLE) {
            idle();
        } else if (currentState == State.WANDERING) {
            wandering();
        } else if (currentState == State.TEMPTING) {
            if (concher.getStage() > 0) {
                setState(State.IDLE);
            }
        } else if (currentState == State.PANIC) {
            panic();
        } else {
            setState(State.IDLE);
            stateTimer = 20;
        }
    }

    // TODO: remove later
    private void spawnPathParticles() {
        if (!DevMode.concherPathfinding() || concher.level().isClientSide())
            return;

        Path path = concher.getNavigation().getPath();
        if (path == null)
            return;

        ServerLevel level = (ServerLevel) concher.level();

        for (int i = 0; i < path.getNodeCount(); i++) {
            Vec3 pos = path.getEntityPosAtNode(concher, i);

            level.sendParticles(
                    ParticleTypes.GLOW,
                    pos.x,
                    pos.y + 0.2d,
                    pos.z,
                    1,
                    0, 0.02, 0, 0);
        }
    }

    public void onHurt(DamageSource source) {
        if (concher.isSheddingActive())
            return;

        if (getState() != State.PANIC) {
            setState(State.PANIC);
            concher.getNavigation().stop();

            if (concher.getStage() > 0) {
                setPanicPhase(PanicPhase.HIDING_START);
                stateTimer = 15;
            } else {
                stateTimer = 100;
            }
        }
    }

    private void panic() {
        if (concher.getStage() == 0) {
            flee();
        } else {
            hide();
        }
    }

    private void flee() {
        stateTimer--;

        if (concher.getNavigation().isDone()) {
            Player player = concher.level().getNearestPlayer(concher, dangerDistance);

            if (player != null) {
                Vec3 fleePos = DefaultRandomPos.getPosAway(concher, 10, 7, player.position());

                if (fleePos != null) {
                    float fleeSpeed = getSpeed() * 1.5f;
                    moveTo(BlockPos.containing(fleePos), fleeSpeed);
                }
            } else if (stateTimer <= 0) {
                setState(State.IDLE);
                concher.getNavigation().stop();
                return;
            }
        }
    }

    private void hide() {
        stateTimer--;
        PanicPhase phase = getPanicPhase();

        if (phase == PanicPhase.HIDING_START) {
            if (stateTimer <= 0) {
                setPanicPhase(PanicPhase.HIDING);
                stateTimer = 100;
            }
        } else if (phase == PanicPhase.HIDING) {
            Player player = concher.level().getNearestPlayer(concher, dangerDistance);

            if (stateTimer <= 0 && player == null) {
                setPanicPhase(PanicPhase.HIDING_END);
                stateTimer = 20;
            }
        } else if (phase == PanicPhase.HIDING_END) {
            if (stateTimer <= 0) {
                setState(State.IDLE);
            }
        }
    }

    private void idle() {
        stateTimer--;

        if (stateTimer <= 0) {
            Vec3 pos = ConcherPathfinding.findValidWanderTarget(concher);

            if (pos != null) {
                targetPos = BlockPos.containing(pos);
                setState(State.WANDERING);
                moveTo(targetPos);

                if (!hasShell) {
                    stateTimer = 200;
                } else {
                    stateTimer = 180;
                }

            } else {
                stateTimer = 40 + concher.getRandom().nextInt(40);
            }
        }
    }

    private void wandering() {
        stateTimer--;

        if (targetPos == null) {
            setState(State.IDLE);
            return;
        }

        boolean reached = concher.getNavigation().isDone() ||
                concher.distanceToSqr(
                        targetPos.getX(),
                        targetPos.getY(),
                        targetPos.getZ()) < 0.1;

        boolean timeout = stateTimer <= -800;

        if (reached || timeout) {
            setState(State.IDLE);
            stateTimer = 60 + concher.getRandom().nextInt(60);
            concher.getNavigation().stop();

            return;
        }

        if (concher.getNavigation().isDone()) {
            moveTo(targetPos);
        }
    }

    private void moveTo(BlockPos pos) {
        moveTo(pos, getSpeed());
    }

    private void moveTo(BlockPos pos, float speed) {
        if (pos == null)
            return;
        concher.getNavigation().moveTo(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                speed);
    }

    private void handleBlinking() {
        this.blinkTimer--;

        if (this.blinkTimer <= 0) {
            this.blinkTimer = concher.getRandom().nextInt(100) + 50;
            concher.getEntityData().set(ConcherEntity.BLINKING, true);
            // Inhabitants.LOGGER.info("[ConcherAi]: Concher is blinking");

        } else if (concher.getEntityData().get(ConcherEntity.BLINKING) &&
                this.blinkTimer % 10 == 0) {

            concher.getEntityData().set(ConcherEntity.BLINKING, false);
        }
    }

    public boolean isTargeting() {
        return !concher.getNavigation().isDone();
    }

    public State getState() {
        return State.values()[concher.getEntityData().get(ConcherEntity.AI_STATE)];
    }

    public void setState(State state) {
        concher.getEntityData().set(ConcherEntity.AI_STATE, state.ordinal());
        stateTicks = 0;
    }

    public SleepingState getSleepingState() {
        return SleepingState.values()[concher.getEntityData().get(ConcherEntity.SLEEPING_STATE)];
    }

    public void setSleepingState(SleepingState state) {
        concher.getEntityData().set(ConcherEntity.SLEEPING_STATE, state.ordinal());
    }

    public PanicPhase getPanicPhase() {
        return PanicPhase.values()[concher.getPanicPhase()];
    }

    public void setPanicPhase(PanicPhase phase) {
        concher.setPanicPhase(phase.ordinal());
    }

    public void onGrowStage(int oldStage, int newStage) {
        if (concher.level().isClientSide())
            return;
        ServerLevel serverLevel = (ServerLevel) concher.level();

        AABB newBox = concher.makeBoundingBox();

        if (serverLevel.noCollision(concher, newBox)) {
            concher.spawnGrowthParticles(serverLevel);
            return;
        }

        double baseX = concher.getX();
        double baseY = concher.getY();
        double baseZ = concher.getZ();
        boolean found = false;
        double[] vertSteps = { 0.25D, 0.5D, 0.75D, 1.0D, 1.25D, 1.5D, 1.75D, 2.0D };
        double[] horizSteps = { 0.0D, 0.5D, -0.5D, 1.0D, -1.0D };

        for (double vy : vertSteps) {
            for (double dx : horizSteps) {
                for (double dz : horizSteps) {
                    AABB moved = newBox.move(dx, vy, dz);
                    if (serverLevel.noCollision(concher, moved)) {
                        concher.setPos(baseX + dx, baseY + vy, baseZ + dz);
                        concher.refreshDimensions();
                        concher.spawnGrowthParticles(serverLevel);
                        found = true;
                        break;
                    }
                }
                if (found)
                    break;
            }
            if (found)
                break;
        }

        if (!found) {
            concher.setStage(oldStage);
            concher.refreshDimensions();
            concher.growTimer = Math.max(0, 15);
            concher.spawnInhibitParticles(serverLevel);
        }
    }

    public float getSpeed() {
        int stage = Math.max(0, Math.min(3, concher.getStage()));
        return concher.isInWater() ? SPEEDS[stage][1] : SPEEDS[stage][0];
    }

    public float getRotationSpeed() {
        int stage = Math.max(0, Math.min(3, concher.getStage()));
        return concher.isInWater() ? SPEEDS[stage][3] : SPEEDS[stage][2];
    }

    public double getAnimationSpeedModifier() {
        int stage = Math.max(0, Math.min(3, concher.getStage()));
        double speed = speedModifiers[stage];
        if (getState() == State.PANIC && stage == 0) {
            speed *= 1.5;
        }
        return speed;
    }

    public int getPreWalkTicks() {
        return concher.getStage() == 0 ? (int) (20 / getAnimationSpeedModifier())
                : (int) (15 / getAnimationSpeedModifier());
    }

    public int getWalkTicks() {
        return concher.getStage() == 0 ? (int) (20 / getAnimationSpeedModifier())
                : (int) (21 / getAnimationSpeedModifier());
    }
}