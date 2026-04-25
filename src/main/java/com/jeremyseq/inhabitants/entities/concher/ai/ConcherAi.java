package com.jeremyseq.inhabitants.entities.concher.ai;

import com.jeremyseq.inhabitants.entities.concher.ConcherEntity;
import com.jeremyseq.inhabitants.debug.DevMode;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

public class ConcherAi {
    private final ConcherEntity concher;
    
    private boolean hasShell;
    private float currentWeight;
    private int blinkTimer = 0;

    private int stateTimer = 0;
    private int stateTicks = 0;
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
        } else {
            setState(State.IDLE);
            stateTimer = 20;
        }
    }

    // TODO: remove later
    private void spawnPathParticles() {
        if (!DevMode.concherPathfinding() || concher.level().isClientSide()) return;

        Path path = concher.getNavigation().getPath();
        if (path == null) return;

        ServerLevel level = (ServerLevel) concher.level();

        for (int i = 0; i < path.getNodeCount(); i++) {
            Node node = path.getNode(i);

            level.sendParticles(
                ParticleTypes.FLAME,
                node.x + 0.5,
                node.y + 0.2,
                node.z + 0.5,
                1,
                0, 0.02, 0, 0
            );
        }
    }

    private void idle() {
        stateTimer--;

        if (stateTimer <= 0) {
            Vec3 pos = DefaultRandomPos.getPos(concher, 5, 4);

            if (pos != null) {
                targetPos = BlockPos.containing(pos);
                setState(State.WANDERING);

                concher.getNavigation().moveTo(
                    targetPos.getX() + 0.5,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5,
                    getSpeed()
                );
                
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

        boolean reached = targetPos == null ||
            concher.getNavigation().isDone() || 
            concher.distanceToSqr(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5
            ) < 0.5;
        
        boolean timeout = stateTimer <= -800;

        if (reached || timeout) {
            setState(State.IDLE);
            stateTimer = 60 + concher.getRandom().nextInt(60);
            concher.getNavigation().stop();
            
            return;
        }
        
        Path path = concher.getNavigation().getPath();
        double lookX, lookZ;
        if (path != null && !path.isDone()) {
            Node nextNode = path.getNextNode();
            lookX = nextNode.x + 0.5;
            lookZ = nextNode.z + 0.5;
        } else {
            lookX = targetPos.getX() + 0.5;
            lookZ = targetPos.getZ() + 0.5;
        }

        double dx = lookX - concher.getX();
        double dz = lookZ - concher.getZ();
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
            isPausing = (stateTicks % 40) < 20;
        } else {
            isPausing = (stateTicks % 60) < 30;
        }
        
        if (isPausing) {
            concher.getNavigation().setSpeedModifier(0.0D);
        } else {
            concher.getNavigation().setSpeedModifier(getSpeed());
            
            if (concher.getNavigation().isDone() && targetPos != null) {
                concher.getNavigation().moveTo(
                    targetPos.getX() + 0.5,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5,
                    getSpeed()
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

    public float getSpeed() {
        return switch (concher.getStage()) {
            case 0 -> 1f;
            case 1 -> 0.8f;
            case 2 -> 0.75f;
            case 3 -> 0.7f;
            default -> 0.7f;
        };
    }
}