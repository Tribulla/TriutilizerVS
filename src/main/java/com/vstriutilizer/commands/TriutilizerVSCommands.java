package com.vstriutilizer.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.triutilizer.core.concurrency.TaskManager;
import com.vstriutilizer.VSTriutilizerMod;
import com.vstriutilizer.integration.ValkyrienSkiesTriutilizerAddon;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Debug commands for TriutilizerVS
 */
public class TriutilizerVSCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("triutilizervs")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("status")
                .executes(TriutilizerVSCommands::showStatus))
            .then(Commands.literal("stats")
                .executes(TriutilizerVSCommands::showStats))
            .then(Commands.literal("solver")
                .executes(TriutilizerVSCommands::checkSolver))
            .then(Commands.literal("overlay")
                .executes(TriutilizerVSCommands::toggleOverlay))
            .then(Commands.literal("test")
                .executes(TriutilizerVSCommands::runTest))
            .then(Commands.literal("logging")
                .then(Commands.literal("enable")
                    .executes(context -> setLogging(context, true)))
                .then(Commands.literal("disable")
                    .executes(context -> setLogging(context, false))))
        );
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ValkyrienSkiesTriutilizerAddon addon = VSTriutilizerMod.getAddon();
            
            if (addon == null) {
                source.sendFailure(Component.literal("§c[TriutilizerVS] Addon not initialized!"));
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("§a[TriutilizerVS] Mod Status: §fLOADED"), false);
            source.sendSuccess(() -> Component.literal("§7Addon ID: §f" + addon.id()), false);
            
            // Get TaskManager stats
            TaskManager.Stats taskStats = TaskManager.getStats();
            source.sendSuccess(() -> Component.literal("§7Worker Threads: §f" + taskStats.threads), false);
            source.sendSuccess(() -> Component.literal("§7Active Tasks: §f" + taskStats.active), false);
            source.sendSuccess(() -> Component.literal("§7Queue Size: §f" + taskStats.queueSize), false);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[TriutilizerVS] Error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    private static int showStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ValkyrienSkiesTriutilizerAddon addon = VSTriutilizerMod.getAddon();
            
            if (addon == null) {
                source.sendFailure(Component.literal("§c[TriutilizerVS] Addon not initialized!"));
                return 0;
            }
            
            ValkyrienSkiesTriutilizerAddon.PhysicsStatistics stats = addon.getStatistics();
            
            source.sendSuccess(() -> Component.literal("§a[TriutilizerVS] Physics Statistics:"), false);
            source.sendSuccess(() -> Component.literal("§7Total Physics Steps: §f" + stats.totalSteps), false);
            source.sendSuccess(() -> Component.literal("§7Parallel Steps: §f" + stats.parallelSteps), false);
            source.sendSuccess(() -> Component.literal("§7Worker Threads: §f" + stats.workerThreads), false);
            source.sendSuccess(() -> Component.literal("§7Active Threads: §f" + stats.activeThreads), false);
            source.sendSuccess(() -> Component.literal("§7Task Queue: §f" + stats.queueSize), false);
            
            // Calculate parallel efficiency
            if (stats.totalSteps > 0) {
                double efficiency = (stats.parallelSteps * 100.0) / stats.totalSteps;
                source.sendSuccess(() -> Component.literal("§7Parallel Efficiency: §f" + 
                    String.format("%.1f%%", efficiency)), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[TriutilizerVS] Error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    private static int checkSolver(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ValkyrienSkiesTriutilizerAddon addon = VSTriutilizerMod.getAddon();
            
            if (addon == null) {
                source.sendFailure(Component.literal("§c[TriutilizerVS] Addon not initialized!"));
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("§a[TriutilizerVS] Physics Solver Information:"), false);
            
            // Check if parallel physics is enabled
            boolean parallelEnabled = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.enableParallelPhysics.get();
            if (parallelEnabled) {
                source.sendSuccess(() -> Component.literal("§7Parallel Physics: §aENABLED"), false);
                source.sendSuccess(() -> Component.literal("§7Solver Type: §fRed-Black SOR (Parallel)"), false);
            } else {
                source.sendSuccess(() -> Component.literal("§7Parallel Physics: §cDISABLED"), false);
                source.sendSuccess(() -> Component.literal("§7Solver Type: §fValkyrien Skies Default"), false);
            }
            
            // Show solver parameters
            double omega = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.sorOmega.get();
            int maxIter = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.sorMaxIterations.get();
            double tolerance = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.sorTolerance.get();
            int minConstraints = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.minConstraintsForParallel.get();
            
            source.sendSuccess(() -> Component.literal("§7SOR Omega: §f" + omega), false);
            source.sendSuccess(() -> Component.literal("§7Max Iterations: §f" + maxIter), false);
            source.sendSuccess(() -> Component.literal("§7Tolerance: §f" + tolerance), false);
            source.sendSuccess(() -> Component.literal("§7Min Constraints: §f" + minConstraints), false);
            
            // Check VS integration status
            boolean integrated = com.vstriutilizer.integration.VSPhysicsIntegration.isIntegrated();
            boolean mixinActive = com.vstriutilizer.integration.VSPhysicsIntegration.isMixinActive();
            
            if (integrated && mixinActive) {
                source.sendSuccess(() -> Component.literal("§7VS Integration: §aACTIVE (Mixin)"), false);
                source.sendSuccess(() -> Component.literal("§7Mixin hook confirmed and working"), false);
            } else if (integrated) {
                source.sendSuccess(() -> Component.literal("§7VS Integration: §aACTIVE (Pending)"), false);
                source.sendSuccess(() -> Component.literal("§7Mixin configured, waiting for ship physics tick"), false);
            } else {
                source.sendSuccess(() -> Component.literal("§7VS Integration: §cFAILED"), false);
                source.sendSuccess(() -> Component.literal("§7Valkyrien Skies not detected"), false);
            }
            
            // Show usage statistics
            ValkyrienSkiesTriutilizerAddon.PhysicsStatistics stats = addon.getStatistics();
            if (stats.totalSteps > 0) {
                source.sendSuccess(() -> Component.literal("§7Total Solves: §f" + stats.totalSteps), false);
                double parallelPercent = (stats.parallelSteps * 100.0) / stats.totalSteps;
                source.sendSuccess(() -> Component.literal("§7Parallel Usage: §f" + String.format("%.1f%%", parallelPercent)), false);
            } else {
                source.sendSuccess(() -> Component.literal("§7No physics steps executed yet"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[TriutilizerVS] Error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    private static int toggleOverlay(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // Toggle the overlay state
            boolean newState = com.vstriutilizer.VSTriutilizerMod.toggleShipOverlay();
            
            if (newState) {
                source.sendSuccess(() -> Component.literal("§a[TriutilizerVS] Ship overlay ENABLED"), false);
                source.sendSuccess(() -> Component.literal("§7Ships using parallel solver will be highlighted"), false);
                source.sendSuccess(() -> Component.literal("§7Green = Using TriutilizerVS parallel solver"), false);
                source.sendSuccess(() -> Component.literal("§7Red = Using VS default solver"), false);
            } else {
                source.sendSuccess(() -> Component.literal("§a[TriutilizerVS] Ship overlay DISABLED"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[TriutilizerVS] Error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    private static int runTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ValkyrienSkiesTriutilizerAddon addon = VSTriutilizerMod.getAddon();
            
            if (addon == null) {
                source.sendFailure(Component.literal("§c[TriutilizerVS] Addon not initialized!"));
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("§a[TriutilizerVS] Running test computation..."), false);
            
            // Test the TaskManager with a simple parallel task
            TaskManager.submit(() -> {
                int result = 0;
                for (int i = 0; i < 1000000; i++) {
                    result += i;
                }
                return result;
            }).thenAccept(result -> {
                source.sendSuccess(() -> Component.literal("§a[TriutilizerVS] Test completed!"), false);
                source.sendSuccess(() -> Component.literal("§7Result: §f" + result), false);
                source.sendSuccess(() -> Component.literal("§7TaskManager is functioning correctly."), false);
            }).exceptionally(ex -> {
                source.sendFailure(Component.literal("§c[TriutilizerVS] Test failed: " + ex.getMessage()));
                ex.printStackTrace();
                return null;
            });
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[TriutilizerVS] Error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    private static int setLogging(CommandContext<CommandSourceStack> context, boolean enabled) {
        CommandSourceStack source = context.getSource();
        
        try {
            ValkyrienSkiesTriutilizerAddon addon = VSTriutilizerMod.getAddon();
            
            if (addon == null) {
                source.sendFailure(Component.literal("§c[TriutilizerVS] Addon not initialized!"));
                return 0;
            }
            
            // Update both config and runtime logging
            addon.setDetailedLogging(enabled);
            
            if (enabled) {
                source.sendSuccess(() -> Component.literal("§a[TriutilizerVS] Detailed logging ENABLED"), false);
                source.sendSuccess(() -> Component.literal("§7Config updated: config/triutilizer/triutilizervs.toml"), false);
            } else {
                source.sendSuccess(() -> Component.literal("§a[TriutilizerVS] Detailed logging DISABLED"), false);
                source.sendSuccess(() -> Component.literal("§7Config updated: config/triutilizer/triutilizervs.toml"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[TriutilizerVS] Error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}
