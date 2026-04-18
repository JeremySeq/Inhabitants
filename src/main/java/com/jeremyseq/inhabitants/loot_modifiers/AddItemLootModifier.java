package com.jeremyseq.inhabitants.loot_modifiers;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.TagParser;

import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class AddItemLootModifier extends LootModifier {

    private final ItemStack itemStack;

    public static final Codec<ItemStack> itemStackCodec = RecordCodecBuilder.create(inst -> inst.group(
            ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(ItemStack::getItem),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ItemStack::getCount),
            Codec.STRING.optionalFieldOf("tag").forGetter(s -> s.hasTag() ? Optional.of(s.getTag().toString()) : Optional.empty())
    ).apply(inst, AddItemLootModifier::createStack));

    private static ItemStack createStack(Item item, int count, Optional<String> tag) {
        ItemStack stack = new ItemStack(item, count);

        tag.ifPresent(s -> {
            try {
                stack.setTag(TagParser.parseTag(s));
            } catch (Exception e) {
                Inhabitants.LOGGER.error("Failed to parse loot modifier NBT: {}", s);
            }
        });
        return stack;
    }

    public static final Codec<AddItemLootModifier> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst).and(
            itemStackCodec.fieldOf("itemStack").forGetter(m -> m.itemStack)
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
