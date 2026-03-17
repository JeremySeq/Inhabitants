package com.jeremyseq.inhabitants.items;

import com.jeremyseq.inhabitants.Inhabitants;
import com.jeremyseq.inhabitants.paintings.ModPaintings;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
                        pOutput.accept(ModItems.MONSTER_MEAL.get());
                        pOutput.accept(ModItems.BANEFUL_POTATO.get());
                        pOutput.accept(ModItems.SPIDER_SOUP.get());
                        pOutput.accept(ModItems.DIMENSIONAL_SNACK.get());
                        pOutput.accept(ModItems.GIANT_BONE.get());
                        pOutput.accept(ModItems.MUSIC_DISC_BOGRE.get());
                        pOutput.accept(ModItems.WARPED_CLAM_ITEM.get());
                        pOutput.accept(ModItems.IMPALER_SPAWN_EGG.get());
                        pOutput.accept(ModItems.IMPALER_SPIKE.get());
                        pOutput.accept(paintingStack(ModPaintings.ENDERMANS_LAST_DAY));
                        pOutput.accept(paintingStack(ModPaintings.MY_PRECIOUS));
                    })
                    .build());

    private static ItemStack paintingStack(RegistryObject<PaintingVariant> variant) {
        ItemStack stack = new ItemStack(Items.PAINTING);
        CompoundTag entityTag = new CompoundTag();
        assert variant.getId() != null;
        entityTag.putString("variant", variant.getId().toString());
        stack.getOrCreateTag().put("EntityTag", entityTag);
        return stack;
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}