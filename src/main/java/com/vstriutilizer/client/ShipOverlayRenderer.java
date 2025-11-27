package com.vstriutilizer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vstriutilizer.VSTriutilizerMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side renderer for visualizing which ships are using TriutilizerVS
 */
@OnlyIn(Dist.CLIENT)
public class ShipOverlayRenderer {
    
    // Track which ships are using parallel solver
    private static final Set<Object> parallelShips = ConcurrentHashMap.newKeySet();
    private static final Set<Object> regularShips = ConcurrentHashMap.newKeySet();
    
    /**
     * Mark a ship as using the parallel solver
     */
    public static void markShipAsParallel(Object shipId) {
        parallelShips.add(shipId);
        regularShips.remove(shipId);
    }
    
    /**
     * Mark a ship as using the regular VS solver
     */
    public static void markShipAsRegular(Object shipId) {
        regularShips.add(shipId);
        parallelShips.remove(shipId);
    }
    
    /**
     * Remove ship from tracking
     */
    public static void removeShip(Object shipId) {
        parallelShips.remove(shipId);
        regularShips.remove(shipId);
    }
    
    /**
     * Clear all tracked ships
     */
    public static void clear() {
        parallelShips.clear();
        regularShips.clear();
    }
    
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!VSTriutilizerMod.isShipOverlayEnabled()) {
            return;
        }
        
        // Only render during the translucent stage to get proper blending
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        
        PoseStack poseStack = event.getPoseStack();
        
        // Get buffer source from the context
        // Note: This requires proper VS integration to get actual ship data
        // For now, this is a framework that will work once VS integration is complete
        
        // TODO: Render ship boxes once we have VS ship data
        // renderShipBoxes(poseStack, bufferSource, parallelShips, 0.0f, 1.0f, 0.0f, 0.3f);
        // renderShipBoxes(poseStack, bufferSource, regularShips, 1.0f, 0.0f, 0.0f, 0.3f);
    }
    
    private static void renderShipBoxes(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                        Set<Object> ships, float r, float g, float b, float a) {
        if (ships.isEmpty()) {
            return;
        }
        
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        
        for (Object shipId : ships) {
            // Try to get ship bounds from VS
            AABB bounds = getShipBounds(shipId);
            if (bounds != null) {
                renderBox(poseStack, consumer, bounds, r, g, b, a);
            }
        }
    }
    
    private static AABB getShipBounds(Object shipId) {
        // This is a placeholder - actual implementation would query VS for ship bounds
        // For now, return null until we have proper VS integration
        // TODO: Integrate with VS ship data to get actual bounding boxes
        
        // Example of what this would look like:
        // if (shipId instanceof ShipData ship) {
        //     return ship.getWorldAABB();
        // }
        
        return null;
    }
    
    private static void renderBox(PoseStack poseStack, VertexConsumer consumer, 
                                   AABB box, float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();
        
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;
        
        // Bottom edges
        consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
        
        consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
        
        consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
        
        consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
        
        // Top edges
        consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
        
        consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        
        consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
        
        consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
        
        // Vertical edges
        consumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).endVertex();
        
        consumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).endVertex();
        
        consumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        
        consumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).endVertex();
    }
}
