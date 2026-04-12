package com.jeremyseq.inhabitants.events;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.bogre.skill.BogreSkills;
import com.jeremyseq.inhabitants.effects.ModEffects;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;

import java.util.*;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Inhabitants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    private static final Map<UUID, Boolean> SNEAK_STATES = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // immaterial sneak
            if (ModEffects.IMMATERIAL.isPresent() && 
                event.player.hasEffect(ModEffects.IMMATERIAL.get()) &&
                !event.player.level().isClientSide) {
                HandleImmaterialSneak(event.player);
            }
        }
    }

    private static void HandleImmaterialSneak(Player player) {
        boolean isSneaking = player.isCrouching();
        boolean wasSneaking = SNEAK_STATES.getOrDefault(player.getUUID(), false);
        
        if (isSneaking && !wasSneaking) {
            // Only descend if we are actually phasing inside a solid block
            if (isInsideBlock(player)) {
                double nudgeX = player.getX();
                double nudgeY = player.getY() - 0.4;
                double nudgeZ = player.getZ();

                BlockPos targetPos = BlockPos.containing(nudgeX, nudgeY, nudgeZ);
                if (!player.level().getBlockState(targetPos).is(Blocks.BEDROCK)) {
                    player.teleportTo(
                        nudgeX, 
                        nudgeY, 
                        nudgeZ
                    );
                    player.setDeltaMovement(
                        player.getDeltaMovement().x, 
                        -1.0, 
                        player.getDeltaMovement().z
                    );
                    
                    player.hurtMarked = true;
                }
            }
        }

        SNEAK_STATES.put(player.getUUID(), isSneaking);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SNEAK_STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BogreSkills.CARVING.onBlockBroken(event.getPlayer().level(), event.getPos());
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() != null &&
            ModEffects.IMMATERIAL.isPresent() &&
            event.getEntity().hasEffect(ModEffects.IMMATERIAL.get())) {
            
            if (event.getSource().is(DamageTypes.IN_WALL)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.CARTOGRAPHER) {

            TagKey<Structure> swampLairTag = TagKey.create(Registries.STRUCTURE,
                ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "swamp_lair"));

            List<VillagerTrades.ItemListing> trades = event.getTrades().get(3);

            if (trades != null) {
                trades.add(new SwampLairMapTrade(
                    13, // em cost
                    swampLairTag,
                    "item.inhabitants.swamp_lair_map",
                    MapDecoration.Type.RED_X,
                    12, // max uses
                    10 // xp
                ));
            }
        }
    }

    private static class SwampLairMapTrade implements VillagerTrades.ItemListing {
        private final int emeraldCost;
        private final TagKey<Structure> destination;
        private final String displayName;
        private final MapDecoration.Type destinationType;
        private final int maxUses;
        private final int villagerXp;

        public SwampLairMapTrade(
            int emeraldCost, 
            TagKey<Structure> destination, 
            String displayName, 
            MapDecoration.Type destinationType, 
            int maxUses, 
            int villagerXp
        ) {
            this.emeraldCost = emeraldCost;
            this.destination = destination;
            this.displayName = displayName;
            this.destinationType = destinationType;
            this.maxUses = maxUses;
            this.villagerXp = villagerXp;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity entity, RandomSource random) {
            if (!(entity.level() instanceof ServerLevel serverlevel)) {
                return null;
            } else {
                BlockPos blockpos = serverlevel.findNearestMapStructure(
                    this.destination, entity.blockPosition(), 100, true);
                
                if (blockpos != null) {
                    ItemStack itemstack = MapItem.create(serverlevel,
                        blockpos.getX(), blockpos.getZ(), (byte)2, true, true);

                    MapItem.renderBiomePreviewMap(serverlevel, itemstack);
                    MapItemSavedData.addTargetDecoration(itemstack, blockpos, "+", this.destinationType);
                    itemstack.setHoverName(Component.translatable(this.displayName));
                    
                    return new MerchantOffer(
                        new ItemStack(Items.EMERALD, this.emeraldCost), 
                        new ItemStack(Items.COMPASS), 
                        itemstack, 
                        this.maxUses, 
                        this.villagerXp, 
                        0.2F
                    );
                } else {
                    return null;
                }
            }
        }
    }

    private static boolean isInsideBlock(Player player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        
        for (double dy : new double[]{0.1D, 0.8D, 1.62D}) {
            BlockPos pos = BlockPos.containing(px, py + dy, pz);

            if (!player.level().getBlockState(pos)
            .getCollisionShape(player.level(),
            pos,
            CollisionContext.empty()).isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
