package com.jeremyseq.inhabitants.entities.bogre.utilities;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;

import java.util.UUID;

public class BogreNbtHelper {
    public static final String CAULDRON_POS_KEY = "CauldronPos";
    public static final String TRANSFORMATION_PROGRESS_KEY = "bogre_disc_progress";

    public static void save(BogreEntity bogre, CompoundTag tag) {
        if (bogre.cauldronPos != null) {
            tag.put(CAULDRON_POS_KEY, NbtUtils.writeBlockPos(bogre.cauldronPos));
        }
    }

    public static void load(BogreEntity bogre, CompoundTag tag) {
        if (tag.contains(CAULDRON_POS_KEY)) {
            bogre.cauldronPos = NbtUtils.readBlockPos(tag.getCompound(CAULDRON_POS_KEY));
        }
    }
}