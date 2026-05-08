package com.jeremyseq.inhabitants.debug;

import net.minecraftforge.fml.loading.FMLEnvironment;

public final class DevMode {

    public static final boolean IN_DEV = !FMLEnvironment.production;

    // --- Bogre ---
    public static boolean showBogre = true;
    public static boolean showBogreStates = true;
    public static boolean showBogrePathfinding = true;

    public static boolean bogre() {
        return IN_DEV && showBogre;
    }

    public static boolean bogreStates() {
        return bogre() && showBogreStates;
    }

    public static boolean bogrePathfinding() {
        return bogre() && showBogrePathfinding;
    }

    // --- Concher ---
    public static boolean showConcher = true;
    public static boolean showConcherStates = true;
    public static boolean showConcherPathfinding = true;

    public static boolean concher() {
        return IN_DEV && showConcher;
    }

    public static boolean concherStates() {
        return concher() && showConcherStates;
    }

    public static boolean concherPathfinding() {
        return concher() && showConcherPathfinding;
    }

}