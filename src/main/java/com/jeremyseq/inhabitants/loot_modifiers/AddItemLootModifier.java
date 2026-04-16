package com.jeremyseq.inhabitants.loot_modifiers;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import net.minecraftforge.common.loot.LootModifier;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.NotNull;

public class AddItemLootModifier extends LootModifier {

    private final ItemStack itemStack;

    public static final Codec<AddItemLootModifier> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst).and(
            ItemStack.CODEC.fieldOf("itemStack").forGetter(m -> m.itemStack)
    ).apply(inst, AddItemLootModifier::new));

    public AddItemLootModifier(LootItemCondition[] conditionsIn, ItemStack itemStack) {
        super(conditionsIn);
        this.itemStack = itemStack;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
        ObjectArrayList<ItemStack> generatedLoot, LootContext context
    ) {
        generatedLoot.add(itemStack.copy());
        return generatedLoot;
    }

    @Override
    public Codec<? extends LootModifier> codec() {
        return CODEC;
    }
}
