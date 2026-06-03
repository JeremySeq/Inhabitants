package com.jeremyseq.inhabitants.compat;

import com.jeremyseq.inhabitants.entities.ModEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.fml.ModList;

public class ImpalerForlornHollowsBiomeModifier implements BiomeModifier {
    @Override
    public void modify(Holder<Biome> holder, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) {
            return;
        }

        if (!ModList.get().isLoaded("alexscaves")) {
            return;
        }

        ResourceLocation id = holder.unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);

        if (id != null && id.equals(ResourceLocation.fromNamespaceAndPath("alexscaves", "forlorn_hollows"))) {

            builder.getMobSpawnSettings().addSpawn(
                    MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(
                            ModEntities.IMPALER.get(),
                            30,
                            1,
                            1
                    )
            );
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return ModBiomeModifiers.IMPALER_FORLORN_HOLLOWS.get();
    }
}
