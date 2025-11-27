package com.vstriutilizer.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration for TriutilizerVS
 * Located in config/triutilizer/triutilizervs.toml
 */
public class TriutilizerVSConfig {
    
    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;
    
    static {
        Pair<Common, ForgeConfigSpec> commonPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = commonPair.getLeft();
        COMMON_SPEC = commonPair.getRight();
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "triutilizer/triutilizervs.toml");
    }
    
    public static class Common {
        
        // Physics Solver Settings
        public final ForgeConfigSpec.BooleanValue enableParallelPhysics;
        public final ForgeConfigSpec.DoubleValue sorOmega;
        public final ForgeConfigSpec.IntValue sorMaxIterations;
        public final ForgeConfigSpec.DoubleValue sorTolerance;
        public final ForgeConfigSpec.IntValue minConstraintsForParallel;
        
        // Debug Settings
        public final ForgeConfigSpec.BooleanValue enableDebugLogging;
        public final ForgeConfigSpec.BooleanValue logPerformanceMetrics;
        public final ForgeConfigSpec.IntValue performanceLogInterval;
        
        // Compatibility Settings
        public final ForgeConfigSpec.BooleanValue forceOverrideVSPhysics;
        
        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment("TriutilizerVS Configuration",
                          "Settings for the Valkyrien Skies Triutilizer integration")
                   .push("triutilizervs");
            
            // Physics Solver Settings
            builder.comment("Physics Solver Settings",
                          "Configure the parallel Red-Black SOR physics solver")
                   .push("physics");
            
            enableParallelPhysics = builder
                .comment("Enable parallel physics solving",
                        "If true, uses the multithreaded Red-Black SOR solver",
                        "If false, falls back to Valkyrien Skies' default solver",
                        "Default: true")
                .define("enableParallelPhysics", true);
            
            sorOmega = builder
                .comment("SOR relaxation parameter (omega)",
                        "Controls convergence speed vs stability",
                        "Range: 1.0 to 2.0",
                        "Lower values (1.5-1.7): More stable, slower convergence",
                        "Higher values (1.8-1.9): Faster convergence, may be less stable",
                        "Default: 1.8")
                .defineInRange("sorOmega", 1.8, 1.0, 2.0);
            
            sorMaxIterations = builder
                .comment("Maximum solver iterations per step",
                        "Higher values improve accuracy but increase CPU usage",
                        "Default: 10 (good balance for most cases)",
                        "Increase to 15-20 for high-precision simulations",
                        "Decrease to 5-7 for better performance on large ships")
                .defineInRange("sorMaxIterations", 10, 1, 50);
            
            sorTolerance = builder
                .comment("Solver convergence tolerance",
                        "Lower values = more accurate but slower",
                        "Higher values = faster but less accurate",
                        "Default: 1e-4 (0.0001)")
                .defineInRange("sorTolerance", 1e-4, 1e-6, 1e-2);
            
            minConstraintsForParallel = builder
                .comment("Minimum constraints to use parallel solver",
                        "Ships with fewer constraints use sequential solver (less overhead)",
                        "Default: 50",
                        "Lower values: Use parallel more often (may have overhead)",
                        "Higher values: Use parallel less often (less overhead)")
                .defineInRange("minConstraintsForParallel", 50, 1, 1000);
            
            builder.pop();
            
            // Debug Settings
            builder.comment("Debug and Logging Settings",
                          "Configure debug output and performance monitoring")
                   .push("debug");
            
            enableDebugLogging = builder
                .comment("Enable detailed debug logging",
                        "Logs solver convergence info and physics steps",
                        "Can be toggled in-game with /triutilizervs logging",
                        "Default: false")
                .define("enableDebugLogging", false);
            
            logPerformanceMetrics = builder
                .comment("Log performance metrics periodically",
                        "Prints physics performance stats to console",
                        "Default: false")
                .define("logPerformanceMetrics", false);
            
            performanceLogInterval = builder
                .comment("Performance logging interval (in seconds)",
                        "How often to log performance metrics (if enabled)",
                        "Default: 60 seconds")
                .defineInRange("performanceLogInterval", 60, 10, 600);
            
            builder.pop();
            
            // Compatibility Settings
            builder.comment("Compatibility Settings",
                          "Control integration behavior with Valkyrien Skies")
                   .push("compatibility");
            
            forceOverrideVSPhysics = builder
                .comment("Attempt to override Valkyrien Skies physics solver",
                        "If true, attempts to replace VS physics solver on startup",
                        "If false, requires VS to manually call the addon",
                        "Note: Automatic integration is experimental and may require manual VS integration",
                        "Default: true")
                .define("forceOverrideVSPhysics", true);
            
            builder.pop();
            
            builder.pop();
        }
    }
}
