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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public class DebugCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!DevMode.bogre()) return;
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(registerBogreCommands());
        dispatcher.register(registerCauldronCommands());
        dispatcher.register(registerDevCommands());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerBogreCommands() {
        return Commands.literal("bogre")
            .requires(source -> source.hasPermission(4))
            .then(Commands.literal("debug")
            .then(Commands.literal("state")
            .then(Commands.argument("enabled", BoolArgumentType.bool())
            .executes(context -> {
                enableStateDebug(context.getSource(),
                BoolArgumentType.getBool(context, "enabled"));
                return 1;
            })))
            
            .then(Commands.literal("path")
            .then(Commands.argument("enabled", BoolArgumentType.bool())
            .executes(context -> {
                enablePathfindingDebug(context.getSource(),
                BoolArgumentType.getBool(context, "enabled"));
                return 1;
            }))))
            
            .then(Commands.literal("spawn")
            .executes(context -> {
                spawnMob(context.getSource(), ModEntities.BOGRE.get());
                return 1;
            }))

            .then(Commands.literal("kill")
            .executes(context -> {
                killMob(context.getSource(), ModEntities.BOGRE.get());
                return 1;
            }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerCauldronCommands() {
        return Commands.literal("cauldron")
            .requires(source -> source.hasPermission(4))
            .then(Commands.literal("spawn")
            .then(Commands.argument("pos", Vec3Argument.vec3())
            .executes(context -> {
                spawnCauldron(context.getSource(), Vec3Argument.getVec3(context, "pos"));
                return 1;
            }))
            .executes(context -> {
                spawnCauldron(context.getSource(), context.getSource().getPosition());
                return 1;
            }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerDevCommands() {
        return Commands.literal("dev")
            .requires(source -> source.hasPermission(4))
            .then(Commands.literal("tankmode")
            .then(Commands.argument("enabled", BoolArgumentType.bool())
            .executes(context -> {
                applyTankMode(context.getSource(),
                BoolArgumentType.getBool(context, "enabled"));
                return 1;
            })))
            
            .then(Commands.literal("fly")
            .then(Commands.argument("enabled", BoolArgumentType.bool())
            .executes(context -> {
                applyFly(context.getSource(),
                BoolArgumentType.getBool(context, "enabled"));
                return 1;
            })));
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

    private static void enableStateDebug(CommandSourceStack source, boolean enabled) {
        DevMode.showBogreStates = enabled;
        log("Bogre state debug: " + DevMode.showBogreStates, source);
    }

    private static void enablePathfindingDebug(CommandSourceStack source, boolean enabled) {
        DevMode.showBogrePathfinding = enabled;
        log("Bogre path debug: " + DevMode.showBogrePathfinding, source);
    }

    private static void applyTankMode(CommandSourceStack source, boolean enabled) {
        Entity entity = source.getEntity();
        if (entity instanceof Player player) {
            if (enabled) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 72000, 4, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 72000, 4, false, false));
                log("Tank Mode: ON", source);
            } else {
                player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                player.removeEffect(MobEffects.REGENERATION);
                log("Tank Mode: OFF", source);
            }
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

            mob.moveTo(source.getPosition().x, source.getPosition().y, source.getPosition().z,
            source.getRotation().y, source.getRotation().x);
            
            source.getLevel().addFreshEntity(mob);
            log("Spawned " + entityType.getDescription().getString(), source);
        }
    }

    private static void killMob(CommandSourceStack source, EntityType<? extends Mob> entityType) {
        int count = 0;
        Vec3 pos = source.getPosition();
        AABB area = new AABB(
            pos.x - 50, pos.y - 50,
            pos.z - 50, pos.x + 50,
            pos.y + 50, pos.z + 50);
        
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