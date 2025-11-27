package com.vstriutilizer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Warning screen shown when VS integration fails
 */
@OnlyIn(Dist.CLIENT)
public class IntegrationWarningScreen extends Screen {
    
    private final Screen previousScreen;
    private static final int PADDING = 20;
    
    public IntegrationWarningScreen(Screen previousScreen) {
        super(Component.literal("TriutilizerVS Integration Warning"));
        this.previousScreen = previousScreen;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Add "Continue" button
        this.addRenderableWidget(Button.builder(
            Component.literal("Continue to Main Menu"),
            button -> this.minecraft.setScreen(previousScreen)
        ).bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
        
        // Add "Don't Show Again" button
        this.addRenderableWidget(Button.builder(
            Component.literal("Don't Show Again"),
            button -> {
                IntegrationWarningTracker.setDismissed(true);
                this.minecraft.setScreen(previousScreen);
            }
        ).bounds(this.width / 2 - 100, this.height - 65, 200, 20).build());
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render dark background
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        
        // Title
        graphics.drawCenteredString(this.font, "⚠ TriutilizerVS Integration Warning", 
                                    this.width / 2, 30, 0xFFFFAA00);
        
        int y = 60;
        int lineHeight = 12;
        
        // Warning message
        drawWrappedText(graphics, "Automatic integration with Valkyrien Skies failed.", y, 0xFFFFFFFF);
        y += lineHeight * 2;
        
        drawWrappedText(graphics, "The parallel physics solver is available but VS must call it manually.", y, 0xFFCCCCCC);
        y += lineHeight * 2;
        
        // Instructions header
        graphics.drawCenteredString(this.font, "Integration Requires Mixins or VS Modifications:", this.width / 2, y, 0xFFFFFF00);
        y += lineHeight * 2;
        
        // Option 1
        drawWrappedText(graphics, "Option 1: Use Mixins (Recommended for modpack creators)", y, 0xFFAAFFAA);
        y += lineHeight;
        drawWrappedText(graphics, "  • Create mixin to hook into VS physics tick", y, 0xFFCCCCCC);
        y += lineHeight;
        drawWrappedText(graphics, "  • Call VSTriutilizerMod.getAddon().solvePhysicsStep()", y, 0xFFCCCCCC);
        y += lineHeight;
        drawWrappedText(graphics, "  • See VS_INTEGRATION_GUIDE.md for mixin examples", y, 0xFFCCCCCC);
        y += lineHeight * 2;
        
        // Option 2
        drawWrappedText(graphics, "Option 2: Wait for official VS integration", y, 0xFFAAFFAA);
        y += lineHeight;
        drawWrappedText(graphics, "  • VS currently doesn't support custom physics solvers", y, 0xFFCCCCCC);
        y += lineHeight;
        drawWrappedText(graphics, "  • This mod will auto-integrate when VS adds support", y, 0xFFCCCCCC);
        y += lineHeight * 2;
        
        // Option 3
        drawWrappedText(graphics, "Option 3: Disable this warning", y, 0xFFAAFFAA);
        y += lineHeight;
        drawWrappedText(graphics, "  • Click 'Don't Show Again' below", y, 0xFFCCCCCC);
        y += lineHeight;
        drawWrappedText(graphics, "  • Or set forceOverrideVSPhysics = false in config", y, 0xFFCCCCCC);
        y += lineHeight;
        drawWrappedText(graphics, "  • Other mod features (commands, stats) still work", y, 0xFFCCCCCC);
        y += lineHeight * 2;
        
        // Command info
        graphics.drawCenteredString(this.font, "Check solver status with: /triutilizervs solver", 
                                    this.width / 2, y, 0xFFAAAAAA);
        y += lineHeight * 2;
        
        // Note
        drawWrappedText(graphics, "Note: VS doesn't currently provide a custom solver API. Integration", 
                       y, 0xFFFF8888);
        y += lineHeight;
        drawWrappedText(graphics, "requires mixins or VS source modifications. All other features work normally.", 
                       y, 0xFFFF8888);
        
        // Render buttons
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void drawWrappedText(GuiGraphics graphics, String text, int y, int color) {
        graphics.drawCenteredString(this.font, text, this.width / 2, y, color);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(previousScreen);
    }
}
