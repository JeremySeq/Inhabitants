package com.jeremyseq.inhabitants.damagesource;

import com.jeremyseq.inhabitants.Inhabitants;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class ModDamageTypes {
    public static final ResourceKey<DamageType> IMPALED = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(Inhabitants.MODID, "impaled"));

    public static DamageSource causeImpaledDamage(
        Level level, @Nullable Entity attacker
    ) {
        return new DamageSource(level.registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(IMPALED), attacker);
    }

    public static DamageSource causeImpaledDamage(
        Level level,
        @Nullable Entity directEntity,
        @Nullable Entity causingEntity
    ) {
        return new DamageSource(level.registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(IMPALED), directEntity, causingEntity);
    }
}
