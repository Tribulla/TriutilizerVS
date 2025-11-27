package com.vstriutilizer;

import com.triutilizer.core.compat.TriutilizerAPI;
import com.vstriutilizer.commands.TriutilizerVSCommands;
import com.vstriutilizer.config.TriutilizerVSConfig;
import com.vstriutilizer.integration.ValkyrienSkiesTriutilizerAddon;
import com.vstriutilizer.integration.VSPhysicsIntegration;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Main mod class for Valkyrien Skies Triutilizer Integration
 * 
 * This mod adds multithreaded physics solving to Valkyrien Skies using
 * the Triutilizer multithreading framework.
 */
@Mod("triutilizervs")
public class VSTriutilizerMod {
    
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("triutilizervs");
    
    public static ValkyrienSkiesTriutilizerAddon ADDON_INSTANCE;
    private static boolean shipOverlayEnabled = false;
    private static boolean integrationFailed = false;
    
    public VSTriutilizerMod() {
        try {
            // Register config first
            TriutilizerVSConfig.register();
            LOGGER.info("Config registered at config/triutilizer/triutilizervs.toml");
            
            // Create addon instance (register later in setup)
            ADDON_INSTANCE = new ValkyrienSkiesTriutilizerAddon();
            
            // Register event handlers
            MinecraftForge.EVENT_BUS.register(this);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onSetup);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
            
            LOGGER.info("TriutilizerVS initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize TriutilizerVS", e);
            throw new RuntimeException("TriutilizerVS initialization failed", e);
        }
    }
    
    private void onClientSetup(FMLClientSetupEvent event) {
        try {
            // Register client-side rendering if we're on the client
            if (FMLEnvironment.dist == Dist.CLIENT) {
                MinecraftForge.EVENT_BUS.register(com.vstriutilizer.client.ShipOverlayRenderer.class);
                MinecraftForge.EVENT_BUS.register(com.vstriutilizer.client.MainMenuEventHandler.class);
                LOGGER.info("Client-side handlers registered");
            }
        } catch (Exception e) {
            LOGGER.error("Error during client setup", e);
            // Don't crash on client setup failure
        }
    }
    
    private void onSetup(FMLCommonSetupEvent event) {
        try {
            // Register the addon with Triutilizer during setup phase
            LOGGER.info("Registering addon with Triutilizer...");
            com.triutilizer.core.compat.TriutilizerAPI.registerAddon(ADDON_INSTANCE);
            LOGGER.info("Addon registered successfully");
            
            // Try to integrate with VS physics if configured
            if (TriutilizerVSConfig.COMMON.forceOverrideVSPhysics.get()) {
                event.enqueueWork(() -> {
                    try {
                        boolean integrated = VSPhysicsIntegration.tryInjectPhysicsSolver(ADDON_INSTANCE);
                        if (!integrated) {
                            integrationFailed = true;
                            LOGGER.warn("VS integration not active - parallel solver available but requires manual integration");
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error during VS integration attempt", e);
                        integrationFailed = true;
                    }
                });
            } else {
                LOGGER.info("Automatic VS integration disabled in config");
            }
        } catch (Exception e) {
            LOGGER.error("Error during setup phase", e);
            throw new RuntimeException("TriutilizerVS setup failed", e);
        }
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        try {
            TriutilizerVSCommands.register(event.getDispatcher());
            LOGGER.info("Debug commands registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register commands", e);
        }
    }
    
    /**
     * Get the addon instance for integration with Valkyrien Skies
     */
    public static ValkyrienSkiesTriutilizerAddon getAddon() {
        return ADDON_INSTANCE;
    }
    
    /**
     * Toggle ship overlay visualization
     * @return new overlay state (true = enabled, false = disabled)
     */
    public static boolean toggleShipOverlay() {
        shipOverlayEnabled = !shipOverlayEnabled;
        System.out.println("[VS-Triutilizer] Ship overlay " + (shipOverlayEnabled ? "enabled" : "disabled"));
        return shipOverlayEnabled;
    }
    
    /**
     * Check if ship overlay is currently enabled
     */
    public static boolean isShipOverlayEnabled() {
        return shipOverlayEnabled;
    }
    
    /**
     * Check if integration warning should be shown
     * @return true if integration failed and should show warning
     */
    public static boolean shouldShowIntegrationWarning() {
        return integrationFailed && TriutilizerVSConfig.COMMON.forceOverrideVSPhysics.get();
    }
    
    /**
     * Check if parallel physics is enabled in config
     * @return true if parallel physics should be used
     */
    public static boolean isParallelPhysicsEnabled() {
        return TriutilizerVSConfig.COMMON.enableParallelPhysics.get();
    }
}
