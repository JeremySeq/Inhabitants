package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.enchantments.ModEnchantments;
import com.jeremyseq.inhabitants.entities.warped_clam.WarpedClamEntity;
import com.jeremyseq.inhabitants.paintings.ModPaintings;

import com.jeremyseq.inhabitants.potions.ModPotions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.*;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Inhabitants.MODID);

    public static final RegistryObject<CreativeModeTab> INHABITANTS_TAB = CREATIVE_MODE_TABS.register("inhabitants_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.CREATIVE_TAB.get()))
                    .title(Component.translatable("creativetab.inhabitants_tab"))
                    .displayItems((pParameters, pOutput) -> {

                        // spawn eggs
                        pOutput.accept(ModItems.BOGRE_SPAWN_EGG.get());
                        pOutput.accept(ModItems.IMPALER_SPAWN_EGG.get());

                        ItemStack warped_clam_ender = new ItemStack(ModItems.WARPED_CLAM_ITEM.get());
                        warped_clam_ender.getOrCreateTag().putInt("variant", WarpedClamEntity.Variant.ENDER.ordinal());
                        warped_clam_ender.getOrCreateTag().putBoolean("has_pearl", true);
                        pOutput.accept(warped_clam_ender);

                        ItemStack warped_clam_camouflage = new ItemStack(ModItems.WARPED_CLAM_ITEM.get());
                        warped_clam_camouflage.getOrCreateTag().putInt("variant", WarpedClamEntity.Variant.CAMOUFLAGE.ordinal());
                        warped_clam_camouflage.getOrCreateTag().putBoolean("has_pearl", true);
                        pOutput.accept(warped_clam_camouflage);

                        ItemStack warped_clam_amaranth = new ItemStack(ModItems.WARPED_CLAM_ITEM.get());
                        warped_clam_amaranth.getOrCreateTag().putInt("variant", WarpedClamEntity.Variant.AMARANTH.ordinal());
                        warped_clam_amaranth.getOrCreateTag().putBoolean("has_pearl", true);
                        pOutput.accept(warped_clam_amaranth);

                        ItemStack warped_clam_voidblue = new ItemStack(ModItems.WARPED_CLAM_ITEM.get());
                        warped_clam_voidblue.getOrCreateTag().putInt("variant", WarpedClamEntity.Variant.VOID_BLUE.ordinal());
                        warped_clam_voidblue.getOrCreateTag().putBoolean("has_pearl", true);
                        pOutput.accept(warped_clam_voidblue);

                        // mob drops / materials
                        pOutput.accept(ModItems.IMPALER_SPIKE.get());

                        // weapons / tools
                        pOutput.accept(ModItems.GIANT_BONE.get());
                        pOutput.accept(ModItems.JAVELIN.get());
                        pOutput.accept(ModItems.SPIKE_DRILL.get());
                        pOutput.accept(ModItems.CONCUSSION_ARROW.get());

                        // food
                        pOutput.accept(ModItems.FISH_SNOT_CHOWDER.get());
                        pOutput.accept(ModItems.UNCANNY_POTTAGE.get());
                        pOutput.accept(ModItems.BAKED_BRAINS.get());
                        pOutput.accept(ModItems.MARINATED_SPIDER.get());
                        pOutput.accept(ModItems.DIMENSIONAL_SERVING.get());

                        // decorative
                        pOutput.accept(ModItems.IMPALER_HEAD.get());
                        pOutput.accept(ModItems.IMPALER_HEAD_DRIPSTONE.get());
                        pOutput.accept(ModItems.IMPALER_HEAD_ALBINO.get());
                        pOutput.accept(ModItems.IMPALER_HEAD_FORLORN_HOLLOWS.get());

                        // misc special items
                        pOutput.accept(ModItems.MUSIC_DISC_BOGRE.get());

                        // potions
                        pOutput.accept(PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.CONCUSSION_POTION.get()));
                        pOutput.accept(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), ModPotions.CONCUSSION_POTION.get()));
                        pOutput.accept(PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), ModPotions.CONCUSSION_POTION.get()));

                        // enchantments
                        addEnchantedBook(ModEnchantments.DIAMOND_TIP.get(), 1, pOutput);
                        addEnchantedBook(ModEnchantments.THERMAL_CAPACITY.get(), 1, pOutput);

                        // paintings
                        addPainting(new ItemStack(Items.PAINTING), ModPaintings.MY_PRECIOUS.getId().toString(), pOutput);
                        addPainting(new ItemStack(Items.PAINTING), ModPaintings.ENDERMANS_LAST_DAY.getId().toString(), pOutput);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private static void addPainting(ItemStack stack, String variant, CreativeModeTab.Output output) {
        stack.getOrCreateTagElement("EntityTag").putString("variant", variant);
        output.accept(stack);
    }

    private static void addEnchantedBook(Enchantment enchantment, int level, CreativeModeTab.Output output) {
        output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)));
    }
}