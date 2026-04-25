package com.jeremyseq.inhabitants.entities.concher.ai;

import com.jeremyseq.inhabitants.entities.concher.ConcherEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

public class ConcherAi {
    private final ConcherEntity concher;
    
    private boolean hasShell;
    private float currentWeight;
    private int blinkTimer = 0;

    private int stateTimer = 0;
    private BlockPos targetPos = null;

    public enum State {
        IDLE,
        WANDERING,
        PANIC, // for non-shell concher is fleeing, for shell concher is hiding inside its shell
        SLEEPING
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
        STAGE_3  // large-shell
    }

    public ConcherAi(ConcherEntity concher) {
        this.concher = concher;
    }

    public void registerGoals() {
        
    }

    public void aiStep() {
        this.hasShell = concher.getStage() > 0;
        this.handleBlinking();

        State currentState = getState();
        if (currentState == State.IDLE) {
            idle();
        } else if (currentState == State.WANDERING) {
            wandering();
        } else {
            setState(State.IDLE);
            stateTimer = 20;
        }
    }

    private void idle() {
        stateTimer--;

        if (stateTimer <= 0) {
            Vec3 pos = DefaultRandomPos.getPos(concher, 5, 4);

            if (pos != null) {
                targetPos = BlockPos.containing(pos);
                setState(State.WANDERING);
                
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

        if (targetPos == null ||
            concher.distanceToSqr(
                targetPos.getX(),
                targetPos.getY(),
                targetPos.getZ()
            ) < 2.0 || stateTimer <= 0) {

            setState(State.IDLE);
            stateTimer = 40 + concher.getRandom().nextInt(60);
            concher.getNavigation().stop();
            
            return;
        }
        
        double dx = targetPos.getX() - concher.getX();
        double dz = targetPos.getZ() - concher.getZ();
        float targetYaw = (float)(Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        float yawDiff = Mth.wrapDegrees(targetYaw - concher.getYRot());

        if (hasShell) {
            concher.setYRot(concher.getYRot() + yawDiff * 0.15f);
            concher.yBodyRot = concher.getYRot();
            concher.yHeadRot = concher.getYRot();
        }

        // snail walk
        boolean isPausing;
        if (!hasShell) {
            isPausing = (stateTimer % 40) >= 20;
        } else {
            isPausing = (stateTimer % 60) >= 30;
        }
        
        if (isPausing) {
            concher.getNavigation().stop();
        } else {
            if (hasShell) {
                // only move if mostly facing the target
                if (Math.abs(yawDiff) < 15.0f) {
                    concher.getNavigation().moveTo(
                        targetPos.getX(),
                        targetPos.getY(),
                        targetPos.getZ(),
                        1.0D
                    );
                } else {
                    concher.getNavigation().stop();
                }
            } else {
                // non-shell: rotate and walk normally
                concher.getNavigation().moveTo(
                    targetPos.getX(),
                    targetPos.getY(),
                    targetPos.getZ(),
                    1.0D
                );
            }
        }
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

    public State getState() {
        return State.values()[concher.getEntityData().get(ConcherEntity.AI_STATE)];
    }

    public void setState(State state) {
        concher.getEntityData().set(ConcherEntity.AI_STATE, state.ordinal());
    }

    public SleepingState getSleepingState() {
        return SleepingState.values()[concher.getEntityData().get(ConcherEntity.SLEEPING_STATE)];
    }

    public void setSleepingState(SleepingState state) {
        concher.getEntityData().set(ConcherEntity.SLEEPING_STATE, state.ordinal());
    }

    public boolean hasShell() {
        return hasShell;
    }

    public void onGrowStage(int oldStage, int newStage) {
        if (concher.level().isClientSide()) return;
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
        double[] vertSteps = {0.25D, 0.5D, 0.75D, 1.0D, 1.25D, 1.5D, 1.75D, 2.0D};
        double[] horizSteps = {0.0D, 0.5D, -0.5D, 1.0D, -1.0D};

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
                if (found) break;
            }
            if (found) break;
        }

        if (!found) {
            concher.setStage(oldStage);
            concher.refreshDimensions();
            concher.growTimer = Math.max(0, 15);
            concher.spawnInhibitParticles(serverLevel);
        }
    }

    private float getWeightOfStage(GrowthStage stage) {
        // TODO: weights of stages
        return 0f;
    }
}