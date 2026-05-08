package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.enchantments.ModEnchantments;
import com.jeremyseq.inhabitants.paintings.ModPaintings;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
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

                        pOutput.accept(ModItems.BOGRE_SPAWN_EGG.get());
                        pOutput.accept(ModItems.FISH_SNOT_CHOWDER.get());
                        pOutput.accept(ModItems.UNCANNY_POTTAGE.get());
                        pOutput.accept(ModItems.GIANT_BONE.get());
                        pOutput.accept(ModItems.WARPED_CLAM_ITEM.get());
                        pOutput.accept(ModItems.IMPALER_SPAWN_EGG.get());
                        pOutput.accept(ModItems.IMPALER_SPIKE.get());
                        pOutput.accept(ModItems.MUSIC_DISC_BOGRE.get());
                        pOutput.accept(ModItems.BAKED_BRAINS.get());
                        pOutput.accept(ModItems.MARINATED_SPIDER.get());
                        pOutput.accept(ModItems.DIMENSIONAL_SERVING.get());
                        pOutput.accept(ModItems.JAVELIN.get());
                        pOutput.accept(ModItems.SPIKE_DRILL.get());
                        pOutput.accept(ModItems.IMPALER_HEAD.get());
                        pOutput.accept(ModItems.DRIPSTONE_IMPALER_HEAD.get());
                        pOutput.accept(ModItems.NIGHTMARE_SPAWN_EGG.get());
                        pOutput.accept(ModItems.DREAD_CLOTH.get());
                        pOutput.accept(ModItems.RIFTBLADE.get());

                        //ench
                        addEnchantedBook(ModEnchantments.DIAMOND_TIP.get(), 1, pOutput);
                        addEnchantedBook(ModEnchantments.THERMAL_CAPACITY.get(), 1, pOutput);

                        //paintings
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