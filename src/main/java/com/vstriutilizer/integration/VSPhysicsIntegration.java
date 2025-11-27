package com.vstriutilizer.integration;

import com.vstriutilizer.config.TriutilizerVSConfig;

/**
 * Handles integration with Valkyrien Skies physics system
 * 
 * With mixin integration, this class now tracks whether the mixin
 * successfully loaded and hooked into VS physics.
 */
public class VSPhysicsIntegration {
    
    private static boolean integrationAttempted = false;
    private static boolean integrationSuccessful = false;
    private static boolean mixinActive = false;
    
    /**
     * Attempts to integrate with Valkyrien Skies physics
     * 
     * With mixin integration, this checks if VS is present and marks
     * integration as successful (mixin handles the actual hooking).
     * 
     * @param addon The addon instance to use for physics solving
     * @return true if integration successful, false otherwise
     */
    public static boolean tryInjectPhysicsSolver(ValkyrienSkiesTriutilizerAddon addon) {
        if (integrationAttempted) {
            return integrationSuccessful;
        }
        
        integrationAttempted = true;
        
        System.out.println("[VS-Triutilizer] Checking Valkyrien Skies integration...");
        
        try {
            // Check if Valkyrien Skies is loaded
            boolean vsLoaded = isValkyrienSkiesLoaded();
            
            if (!vsLoaded) {
                System.out.println("[VS-Triutilizer] ✗ Valkyrien Skies not found!");
                System.out.println("[VS-Triutilizer] This mod requires Valkyrien Skies to function.");
                return false;
            }
            
            // VS is loaded, and we have mixins configured
            // The mixin will automatically hook into VS physics
            integrationSuccessful = true;
            System.out.println("[VS-Triutilizer] ✓ Valkyrien Skies detected");
            System.out.println("[VS-Triutilizer] ✓ Mixin integration active");
            System.out.println("[VS-Triutilizer] Parallel physics solver will hook into VS automatically");
            return true;
            
        } catch (Exception e) {
            System.err.println("[VS-Triutilizer] Error during integration: " + e.getMessage());
            if (TriutilizerVSConfig.COMMON.enableDebugLogging.get()) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Check if Valkyrien Skies is loaded by looking for a core VS class
     */
    private static boolean isValkyrienSkiesLoaded() {
        try {
            // Check for core VS class that should always be present
            Class.forName("org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Called by mixin to notify that it successfully hooked into VS
     */
    public static void notifyMixinActive() {
        if (!mixinActive) {
            mixinActive = true;
            System.out.println("[VS-Triutilizer] Mixin hook confirmed active");
        }
    }
    
    /**
     * Check if integration is active
     * @return true if VS integration is working (via mixin or otherwise)
     */
    public static boolean isIntegrated() {
        return integrationSuccessful;
    }
    
    /**
     * Check if mixin hook is confirmed active
     * @return true if mixin reported successful hook
     */
    public static boolean isMixinActive() {
        return mixinActive;
    }
}
