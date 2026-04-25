package com.jeremyseq.inhabitants.debug;

import net.minecraftforge.fml.loading.FMLEnvironment;

public final class DevMode {

    private static final boolean IN_DEV = !FMLEnvironment.production;

    // --- Bogre ---
    public static boolean showBogre = false;
    public static boolean showStates = false;
    public static boolean showPathfinding = false;

    public static boolean bogre()            { return IN_DEV && showBogre; }
    public static boolean bogreStates()      { return bogre() && showStates; }
    public static boolean bogrePathfinding() { return bogre() && showPathfinding; }

    // --- Concher ---
    public static boolean showConcher = true;
    public static boolean showConcherStates = true;

    public static boolean concher() { return IN_DEV && showConcher; }
    public static boolean concherStates() { return concher() && showConcherStates; }

}