package com.vstriutilizer.client;

import com.vstriutilizer.VSTriutilizerMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handles showing the integration warning on the main menu
 */
@OnlyIn(Dist.CLIENT)
public class MainMenuEventHandler {
    
    private static boolean warningShown = false;
    
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        // Only show on title screen and only once per session
        if (event.getScreen() instanceof TitleScreen && !warningShown) {
            warningShown = true;
            
            // Check if integration failed and warning hasn't been dismissed
            if (VSTriutilizerMod.shouldShowIntegrationWarning() && !IntegrationWarningTracker.isDismissed()) {
                // Schedule the warning screen to show after a short delay
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.execute(() -> {
                    // Small delay to let the main menu fully load
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Show the warning screen
                    minecraft.execute(() -> {
                        if (minecraft.screen instanceof TitleScreen) {
                            minecraft.setScreen(new IntegrationWarningScreen(minecraft.screen));
                        }
                    });
                });
            }
        }
    }
    
    /**
     * Reset warning state (for testing or when joining servers)
     */
    public static void resetWarningState() {
        warningShown = false;
    }
}
