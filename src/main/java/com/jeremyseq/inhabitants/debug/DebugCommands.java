package com.jeremyseq.inhabitants.debug;

import com.jeremyseq.inhabitants.entities.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegistryObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public class DebugCommands {

    @FunctionalInterface
    public interface BoolConsumer {
        void accept(boolean value);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!DevMode.IN_DEV) return;
        register(event.getDispatcher());
    }

    /**
     * Registers all debug commands.
     * Add new mob commands here as new mobs are created.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                MobCommandBuilder.forMob("bogre", ModEntities.BOGRE)
                        .requires(DevMode::bogre)
                        .withStateDebug(() -> DevMode.showBogreStates, v -> DevMode.showBogreStates = v)
                        .withPathDebug(() -> DevMode.showBogrePathfinding, v -> DevMode.showBogrePathfinding = v)
                        .withSpawn()
                        .withKill()
                        .build()
        );

        dispatcher.register(registerCauldronCommands());
        dispatcher.register(registerDevCommands());
    }

    /**
     * A builder for registering a standard set of debug commands for a mob.
     * <p>
     * Each standard feature (spawn, kill, path debug, state debug) is opt-in. Extra subcommands
     * can be added via {@link #withExtra}.
     * <p>
     * Example usage:
     * <pre>{@code
     * MobCommandBuilder.forMob("bogre", ModEntities.BOGRE)
     *     .requires(DevMode::bogre)
     *     .withStateDebug(() -> DevMode.showBogreStates, v -> DevMode.showBogreStates = v)
     *     .withPathDebug(() -> DevMode.showBogrePathfinding, v -> DevMode.showBogrePathfinding = v)
     *     .withSpawn()
     *     .withKill()
     *     .build();
     * }</pre>
     */
    public static class MobCommandBuilder {
        private final String name;
        private final RegistryObject<? extends EntityType<? extends Mob>> entityType;

        private BooleanSupplier guard = () -> true;
        private BooleanSupplier pathGetter = null;
        private BoolConsumer pathSetter = null;
        private BooleanSupplier stateGetter = null;
        private BoolConsumer stateSetter = null;
        private boolean spawn = false;
        private boolean kill = false;
        private final List<ExtraCommand> extras = new ArrayList<>();

        private MobCommandBuilder(String name, RegistryObject<? extends EntityType<? extends Mob>> entityType) {
            this.name = name;
            this.entityType = entityType;
        }

        public static MobCommandBuilder forMob(String name, RegistryObject<? extends EntityType<? extends Mob>> type) {
            return new MobCommandBuilder(name, type);
        }

        public MobCommandBuilder requires(BooleanSupplier guard) {
            this.guard = guard;
            return this;
        }

        public MobCommandBuilder withPathDebug(BooleanSupplier getter, BoolConsumer setter) {
            this.pathGetter = getter;
            this.pathSetter = setter;
            return this;
        }

        public MobCommandBuilder withStateDebug(BooleanSupplier getter, BoolConsumer setter) {
            this.stateGetter = getter;
            this.stateSetter = setter;
            return this;
        }

        public MobCommandBuilder withSpawn() {
            this.spawn = true;
            return this;
        }

        public MobCommandBuilder withKill() {
            this.kill = true;
            return this;
        }

        public MobCommandBuilder withExtra(String subName, BiConsumer<CommandSourceStack, EntityType<? extends Mob>> action) {
            extras.add(new ExtraCommand(subName, action));
            return this;
        }

        public LiteralArgumentBuilder<CommandSourceStack> build() {
            final BooleanSupplier g = guard;
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(name)
                    .requires(source -> source.hasPermission(4) && g.getAsBoolean());

            if (pathGetter != null || stateGetter != null) {
                LiteralArgumentBuilder<CommandSourceStack> debug = Commands.literal("debug");

                if (stateGetter != null) {
                    final BoolConsumer setter = stateSetter;
                    debug.then(Commands.literal("state")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> {
                                boolean val = BoolArgumentType.getBool(ctx, "enabled");
                                setter.accept(val);
                                log("State debug: " + val, ctx.getSource());
                                return 1;
                            })
                        )
                    );
                }

                if (pathGetter != null) {
                    final BoolConsumer setter = pathSetter;
                    debug.then(Commands.literal("path")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> {
                                boolean val = BoolArgumentType.getBool(ctx, "enabled");
                                setter.accept(val);
                                log("Path debug: " + val, ctx.getSource());
                                return 1;
                            })
                        )
                    );
                }

                root.then(debug);
            }

            if (spawn) {
                root.then(Commands.literal("spawn")
                    .executes(ctx -> {
                        spawnMob(ctx.getSource(), entityType.get());
                        return 1;
                    })
                );
            }

            if (kill) {
                root.then(Commands.literal("kill")
                    .executes(ctx -> {
                        killMob(ctx.getSource(), entityType.get());
                        return 1;
                    })
                );
            }

            for (ExtraCommand extra : extras) {
                final ExtraCommand captured = extra;
                root.then(Commands.literal(captured.subName)
                    .executes(ctx -> {
                        captured.action.accept(ctx.getSource(), entityType.get());
                        return 1;
                    })
                );
            }

            return root;
        }

        private record ExtraCommand(String subName, BiConsumer<CommandSourceStack, EntityType<? extends Mob>> action) {}
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerCauldronCommands() {
        return Commands.literal("cauldron")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(context -> {
                                    spawnCauldron(context.getSource(), Vec3Argument.getVec3(context, "pos"));
                                    return 1;
                                })
                        )
                        .executes(context -> {
                            spawnCauldron(context.getSource(), context.getSource().getPosition());
                            return 1;
                        })
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerDevCommands() {
        return Commands.literal("dev")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("tankmode")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    applyTankMode(context.getSource(), BoolArgumentType.getBool(context, "enabled"));
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("fly")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    applyFly(context.getSource(), BoolArgumentType.getBool(context, "enabled"));
                                    return 1;
                                })
                        )
                );
    }

    private static void spawnCauldron(CommandSourceStack source, Vec3 pos) {
        Entity cauldron = ModEntities.BOGRE_CAULDRON.get().create(source.getLevel());
        if (cauldron != null) {
            double cx = Math.floor(pos.x) + 1.0D;
            double cy = Math.floor(pos.y);
            double cz = Math.floor(pos.z) + 1.0D;
            cauldron.moveTo(cx, cy, cz, 0, 0);
            source.getLevel().addFreshEntity(cauldron);
            log("Spawned Cauldron", source);
        }
    }

    private static void applyTankMode(CommandSourceStack source, boolean enabled) {
        Entity entity = source.getEntity();
        if (entity instanceof Player player) {
            if (enabled) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 72000, 4, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 72000, 4, false, false));
            } else {
                player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                player.removeEffect(MobEffects.REGENERATION);
            }
            log("Tank Mode: " + (enabled ? "ON" : "OFF"), source);
        }
    }

    private static void applyFly(CommandSourceStack source, boolean enabled) {
        Entity entity = source.getEntity();
        if (entity instanceof Player player) {
            player.getAbilities().mayfly = enabled;
            if (!enabled) player.getAbilities().flying = false;
            player.onUpdateAbilities();
            log("Flight: " + (enabled ? "ON" : "OFF"), source);
        }
    }

    private static void spawnMob(CommandSourceStack source, EntityType<? extends Mob> entityType) {
        Mob mob = entityType.create(source.getLevel());
        if (mob != null) {
            mob.moveTo(source.getPosition().x, source.getPosition().y, source.getPosition().z, source.getRotation().y, source.getRotation().x);
            source.getLevel().addFreshEntity(mob);
            log("Spawned " + entityType.getDescription().getString(), source);
        }
    }

    private static void killMob(CommandSourceStack source, EntityType<? extends Mob> entityType) {
        int count = 0;
        Vec3 pos = source.getPosition();
        AABB area = new AABB(pos.x - 50, pos.y - 50, pos.z - 50, pos.x + 50, pos.y + 50, pos.z + 50);
        for (Mob mob : source.getLevel().getEntitiesOfClass(Mob.class, area, e -> e.getType() == entityType)) {
            mob.discard();
            count++;
        }
        log("Killed " + count + " " + entityType.getDescription().getString(), source);
    }

    private static void log(String message, CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(message), true);
    }
}