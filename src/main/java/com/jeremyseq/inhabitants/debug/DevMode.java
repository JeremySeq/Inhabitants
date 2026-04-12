package com.jeremyseq.inhabitants.debug;

import net.minecraftforge.fml.loading.FMLEnvironment;

public final class DevMode {

    // --- Core ---
    private static boolean enabled = !FMLEnvironment.production;
    public static boolean isEnabled() { return enabled; }

    // --- Bogre ---
    private static boolean showBogre = true;
    private static boolean showStates = true;
    private static boolean showPathfinding = true;

    public static boolean bogre() { return isEnabled() && isShowBogre(); }
    public static boolean bogreStates() { return bogre() && isShowStates(); }
    public static boolean bogrePathfinding() { return bogre() && isShowPathfinding(); }

    public static boolean isShowBogre() { return showBogre; }
    public static boolean isShowStates() { return showStates; }
    public static boolean isShowPathfinding() { return showPathfinding; }

    public static void setShowStates(boolean value) { showStates = value; }
    public static void setShowBogre(boolean value) { showBogre = value; }
    public static void setShowPathfinding(boolean value) { showPathfinding = value; }
    
    // --- Impaler Variables ---

    // --- Clam Variables ---


    public static void setEnabled(boolean value) { 
        if (FMLEnvironment.production) return;
        enabled = value; 
    }
}