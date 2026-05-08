package com.jeremyseq.inhabitants.debug;

import net.minecraftforge.fml.loading.FMLEnvironment;

public final class DevMode {

    private static final boolean IN_DEV = !FMLEnvironment.production;

    // --- Bogre ---
    public static boolean showBogre = true;
    public static boolean showStates = true;
    public static boolean showPathfinding = true;

    public static boolean bogre()            { return IN_DEV && showBogre; }
    public static boolean bogreStates()      { return bogre() && showStates; }
    public static boolean bogrePathfinding() { return bogre() && showPathfinding; }

    // --- Impaler ---

    // --- Clam ---

}