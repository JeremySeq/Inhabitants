package com.jeremyseq.inhabitants.loot_modifiers;

import com.jeremyseq.inhabitants.items.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public class AddChitinTemplateModifier extends LootModifier {

    public static final Codec<AddChitinTemplateModifier> CODEC = RecordCodecBuilder.create(inst ->
            codecStart(inst).apply(inst, AddChitinTemplateModifier::new));

    public AddChitinTemplateModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation tableId = context.getQueriedLootTableId();
        if (tableId.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "chests/pillager_outpost"))) {
            if (context.getRandom().nextFloat() < 0.25f) {
                generatedLoot.add(new ItemStack(ModItems.CHITIN_UPGRADE_SMITHING_TEMPLATE.get()));
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
