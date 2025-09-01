package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.entities.ModEntities;
import com.jeremyseq.inhabitants.entities.gazer.GazerEntity;
import com.jeremyseq.inhabitants.items.armor.GazerPodArmorRenderer;
import com.jeremyseq.inhabitants.networking.GazerCameraPacketS2C;
import com.jeremyseq.inhabitants.networking.ModNetworking;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.NetworkDirection;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class GazerPodItem extends ArmorItem implements GeoItem {
    AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public GazerPodItem(Properties properties) {
        super(ArmorMaterials.LEATHER, Type.HELMET, properties);
    }

    // ===== NBT Helpers =====
    public static boolean hasGazer(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean("HasGazer");
    }

    public static void setHasGazer(ItemStack stack, boolean hasGazer) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean("HasGazer", hasGazer);
    }

    public static int getGazerId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return -1;
        return tag.getInt("GazerId") != 0 ? tag.getInt("GazerId") : -1;
    }

    public static void setGazerId(ItemStack stack, int gazerId) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("GazerId", gazerId);
    }

    // ===== Right Click in Air =====
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // called on server when releasing
        if (!level.isClientSide && hasGazer(stack) && player instanceof ServerPlayer serverPlayer) {
            // Release gazer
            GazerEntity gazerEntity = ModEntities.GAZER.get().create(level);

            assert gazerEntity != null;
            gazerEntity.podOwner = player.getUUID();
            gazerEntity.moveTo(player.getX(), player.getY() + 1, player.getZ(), player.getYRot(), player.getXRot());
            level.addFreshEntity(gazerEntity);

            setGazerId(stack, gazerEntity.getId());

            gazerEntity.exitPod(player, true);

            setHasGazer(stack, false);

            Inhabitants.LOGGER.debug("Releasing gazer with ID {}", gazerEntity.getId());

            ModNetworking.CHANNEL.sendTo(new GazerCameraPacketS2C(gazerEntity.getId(), true),
                    serverPlayer.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT);

            return InteractionResultHolder.success(stack);
        }

        return super.use(level, player, hand);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return super.onDroppedByPlayer(item, player);
    }

//    @Override
//    public void onInventoryTick(ItemStack stack, Level level, Player player, int slotIndex, int selectedIndex) {
//        if (player.isDeadOrDying() && !hasGazer(stack)) {
//            if (getGazerId(stack) != -1) {
//                // Clear gazer ID if player dies and pod is empty
//                setGazerId(stack, -1);
//            } else {
//                // If player dies and pod has a gazer, just ensure it's marked as empty
//                GazerEntity gazer = (GazerEntity) level.getEntity(getGazerId(stack));
//
//                assert gazer != null;
//                gazer.podOwner = null; // clear owner to prevent issues
//                gazer.currentState = GazerEntity.GazerState.IDLE; // set to idle state
//            }
//        }
//        super.onInventoryTick(stack, level, player, slotIndex, selectedIndex);
//    }

    // ===== Right Click Entity =====
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof GazerEntity gazer) {
            if (!hasGazer(player.getItemInHand(hand))) {
                setHasGazer(player.getItemInHand(hand), true);
                gazer.enterPod(); // calls discard()
                return InteractionResult.SUCCESS;
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    // ===== Tooltip =====
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasGazer(stack)) {
            tooltip.add(net.minecraft.network.chat.Component.literal("Contains a Gazer"));
        } else {
            tooltip.add(net.minecraft.network.chat.Component.literal("Empty Pod"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GazerPodArmorRenderer renderer;

            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> _default) {
                if (this.renderer == null) {
                    this.renderer = new GazerPodArmorRenderer();
                }
                this.renderer.prepForRender(entity, itemStack, armorSlot, _default);
                return this.renderer;
            }
        });
    }
}
