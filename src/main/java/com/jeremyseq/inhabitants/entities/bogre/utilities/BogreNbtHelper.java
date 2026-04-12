package com.jeremyseq.inhabitants.entities.bogre.utilities;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import java.util.UUID;

public class BogreNbtHelper {
    public static final String CAULDRON_POS_KEY = "CauldronPos";
    public static final String RESULT_OWNER_UUID_KEY = "OwnerUUID";

    public static void save(BogreEntity bogre, CompoundTag tag) {
        if (bogre.cauldronPos != null) {
            tag.put(CAULDRON_POS_KEY, NbtUtils.writeBlockPos(bogre.cauldronPos));
        }
        UUID ownerUUID = bogre.getAi().getResultOwnerUUID();
        if (ownerUUID != null) {
            tag.putUUID(RESULT_OWNER_UUID_KEY, ownerUUID);
        }
    }

    public static void load(BogreEntity bogre, CompoundTag tag) {
        if (tag.contains(CAULDRON_POS_KEY)) {
            bogre.cauldronPos = NbtUtils.readBlockPos(tag.getCompound(CAULDRON_POS_KEY));
        }
        
        if (tag.hasUUID(RESULT_OWNER_UUID_KEY)) {
            bogre.getAi().setResultOwnerUUID(tag.getUUID(RESULT_OWNER_UUID_KEY));
        }
    }
}