package com.vstriutilizer.integration;

import com.triutilizer.core.compat.TriutilizerAddon;
import com.triutilizer.core.compat.CompatContext;
import com.triutilizer.core.concurrency.TaskManager;
import com.triutilizer.core.concurrency.Priority;
import com.triutilizer.core.util.MainThread;
import com.vstriutilizer.physics.ParallelRedBlackSORSolver;
import com.vstriutilizer.physics.PhysicsConstraint;
import com.vstriutilizer.physics.RigidBodyState;

import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Triutilizer addon for Valkyrien Skies
 * 
 * This addon provides multithreaded physics solving for Valkyrien Skies using a custom
 * Red-Black SOR solver that's optimized for parallel execution.
 * 
 * Key Features:
 * - Parallel constraint solving using Red-Black SOR algorithm
 * - Memory efficient (unlike Jacobi solver)
 * - Truly parallel (unlike Gauss-Seidel solver)
 * - Thread-safe integration with Minecraft
 * - Adaptive performance based on available threads
 */
public class ValkyrienSkiesTriutilizerAddon implements TriutilizerAddon {
    
    private static final String ADDON_ID = "valkyrienskies";
    
    // Physics solver instance
    private ParallelRedBlackSORSolver solver;
    
    // Statistics tracking
    private final AtomicInteger totalPhysicsSteps = new AtomicInteger(0);
    private final AtomicInteger parallelSteps = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Long> performanceMetrics = new ConcurrentHashMap<>();
    
    // Ship tracking for overlay visualization
    private final ConcurrentHashMap<Object, Boolean> shipSolverUsage = new ConcurrentHashMap<>();
    
    // Runtime config cache
    private boolean enableDetailedLogging = false;
    
    @Override
    public String id() {
        return ADDON_ID;
    }
    
    @Override
    public void onRegister(CompatContext ctx) {
        System.out.println("[VS-Triutilizer] Valkyrien Skies integration registered");
        System.out.println("[VS-Triutilizer] Using Parallel Red-Black SOR solver");
        
        // Initialize the physics solver with config values
        updateSolverFromConfig();
        
        // We don't request thread caps - we want to use all available threads
        // for physics computation
        System.out.println("[VS-Triutilizer] Physics solver initialized from config");
    }
    
    private void updateSolverFromConfig() {
        double omega = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.sorOmega.get();
        int maxIter = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.sorMaxIterations.get();
        double tol = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.sorTolerance.get();
        
        this.solver = new ParallelRedBlackSORSolver(omega, maxIter, tol);
        this.enableDetailedLogging = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.enableDebugLogging.get();
    }
    
    @Override
    public void onServerAboutToStart(MinecraftServer server, CompatContext ctx) {
        System.out.println("[VS-Triutilizer] Server starting - physics system ready");
        
        // Log available threads for physics
        TaskManager.Stats stats = TaskManager.getStats();
        System.out.println("[VS-Triutilizer] Available worker threads: " + stats.threads);
    }
    
    @Override
    public void onServerStopping(MinecraftServer server, CompatContext ctx) {
        System.out.println("[VS-Triutilizer] Server stopping - finalizing physics");
        
        // Print statistics
        printStatistics();
    }
    
    /**
     * Main physics step function - called by Valkyrien Skies to solve physics
     * 
     * This is the primary integration point. Valkyrien Skies should call this
     * method instead of its default solver.
     * 
     * @param constraints The physics constraints to solve
     * @param bodies The rigid bodies in the simulation
     * @param deltaTime Time step for integration
     * @return CompletableFuture that completes when physics step is done
     */
    public CompletableFuture<Void> solvePhysicsStep(
            List<PhysicsConstraint> constraints,
            List<RigidBodyState> bodies,
            double deltaTime) {
        return solvePhysicsStep(constraints, bodies, deltaTime, null);
    }
    
    /**
     * Physics step with ship tracking for overlay visualization
     * 
     * @param constraints The physics constraints to solve
     * @param bodies The rigid bodies in the simulation
     * @param deltaTime Time step for integration
     * @param shipId Optional ship identifier for tracking (can be null)
     * @return CompletableFuture that completes when physics step is done
     */
    public CompletableFuture<Void> solvePhysicsStep(
            List<PhysicsConstraint> constraints,
            List<RigidBodyState> bodies,
            double deltaTime,
            Object shipId) {
        
        // Check if parallel physics is enabled in config
        if (!com.vstriutilizer.config.TriutilizerVSConfig.COMMON.enableParallelPhysics.get()) {
            // Parallel physics disabled - mark ship as using regular solver
            if (shipId != null) {
                shipSolverUsage.put(shipId, false);
                notifyShipRenderer(shipId, false);
            }
            return CompletableFuture.completedFuture(null);
        }
        
        long startTime = System.nanoTime();
        totalPhysicsSteps.incrementAndGet();
        
        if (constraints.isEmpty() || bodies.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Check minimum constraint threshold
        int minConstraints = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.minConstraintsForParallel.get();
        if (constraints.size() < minConstraints) {
            // Too few constraints, use simple sequential solving
            if (shipId != null) {
                shipSolverUsage.put(shipId, false);
                notifyShipRenderer(shipId, false);
            }
            return solveSequentially(constraints, deltaTime);
        }
        
        // Mark ship as using parallel solver
        if (shipId != null) {
            shipSolverUsage.put(shipId, true);
            notifyShipRenderer(shipId, true);
        }
        
        // Update solver parameters from config
        updateSolverFromConfig();
        
        // Step 1: Solve constraints in parallel using Red-Black SOR
        return solver.solve(constraints, bodies)
            .thenCompose(result -> {
                parallelSteps.incrementAndGet();
                
                if (enableDetailedLogging) {
                    System.out.println("[VS-Triutilizer] Solver converged in " + 
                                     result.iterations + " iterations, error: " + 
                                     result.finalError);
                }
                
                // Step 2: Integrate body positions (must be on main thread)
                return TaskManager.submit(() -> {
                    MainThread.execute(() -> {
                        for (RigidBodyState body : bodies) {
                            body.integrate(deltaTime);
                        }
                    });
                    return null;
                }, Priority.HIGH).thenApply(v -> (Void) null);
            })
            .whenComplete((v, ex) -> {
                long elapsed = System.nanoTime() - startTime;
                recordPerformance("physicsStep", elapsed);
                
                if (ex != null) {
                    System.err.println("[VS-Triutilizer] Physics step failed: " + 
                                     ex.getMessage());
                    ex.printStackTrace();
                }
            });
    }
    
    /**
     * Optimized batch physics solving for multiple independent ship systems
     * 
     * @param shipSystems List of independent ship physics systems
     * @param deltaTime Time step
     * @return CompletableFuture that completes when all ships are solved
     */
    public CompletableFuture<Void> solveMultipleShips(
            List<ShipPhysicsSystem> shipSystems,
            double deltaTime) {
        
        if (shipSystems.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Process each ship in parallel since they're independent
        return TaskManager.mapParallel(shipSystems, ship -> {
            return solvePhysicsStep(ship.getConstraints(), ship.getBodies(), deltaTime)
                .thenApply(v -> ship);
                
        }).thenAccept(results -> {
            if (enableDetailedLogging) {
                System.out.println("[VS-Triutilizer] Solved " + results.size() + 
                                 " ships in parallel");
            }
        });
    }
    
    /**
     * Parallel collision detection for multiple ships
     * This can be called before the physics step
     */
    public CompletableFuture<List<PhysicsConstraint>> detectCollisionsParallel(
            List<ShipPhysicsSystem> ships) {
        
        return TaskManager.submit(() -> {
            // Broad phase: parallel AABB checks
            return performBroadPhase(ships);
            
        }, Priority.HIGH).thenCompose(pairs -> {
            // Narrow phase: parallel detailed collision detection
            return TaskManager.mapChunked(pairs, 32, pairChunk -> {
                return performNarrowPhase(pairChunk);
            }, Priority.HIGH);
            
        }).thenApply(constraintLists -> {
            // Flatten the list of lists
            return constraintLists.stream()
                .flatMap(List::stream)
                .toList();
        });
    }
    
    /**
     * Configuration methods
     */
    public void setSORParameters(double omega, int maxIterations, double tolerance) {
        this.solver = new ParallelRedBlackSORSolver(omega, maxIterations, tolerance);
        System.out.println("[VS-Triutilizer] Solver reconfigured: omega=" + omega +
                         ", maxIter=" + maxIterations + ", tolerance=" + tolerance);
    }
    
    public void setDetailedLogging(boolean enabled) {
        this.enableDetailedLogging = enabled;
        com.vstriutilizer.config.TriutilizerVSConfig.COMMON.enableDebugLogging.set(enabled);
    }
    
    /**
     * Notify the client-side renderer about which solver a ship is using
     */
    private void notifyShipRenderer(Object shipId, boolean usingParallel) {
        try {
            if (usingParallel) {
                com.vstriutilizer.client.ShipOverlayRenderer.markShipAsParallel(shipId);
            } else {
                com.vstriutilizer.client.ShipOverlayRenderer.markShipAsRegular(shipId);
            }
        } catch (NoClassDefFoundError e) {
            // Client classes not available on server - ignore
        }
    }
    
    /**
     * Get solver usage for a specific ship
     */
    public boolean isShipUsingParallelSolver(Object shipId) {
        return shipSolverUsage.getOrDefault(shipId, false);
    }
    
    /**
     * Remove ship from tracking when it's unloaded
     */
    public void removeShip(Object shipId) {
        shipSolverUsage.remove(shipId);
        try {
            com.vstriutilizer.client.ShipOverlayRenderer.removeShip(shipId);
        } catch (NoClassDefFoundError e) {
            // Client classes not available on server - ignore
        }
    }
    
    private CompletableFuture<Void> solveSequentially(List<PhysicsConstraint> constraints, double deltaTime) {
        return CompletableFuture.supplyAsync(() -> {
            int maxIter = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.sorMaxIterations.get();
            double tolerance = com.vstriutilizer.config.TriutilizerVSConfig.COMMON.sorTolerance.get();
            
            // Note: Sequential solving is a simplified fallback
            // Constraints may need access to body lists - this is a basic implementation
            for (int iter = 0; iter < maxIter; iter++) {
                double maxError = 0.0;
                
                // This is a placeholder - actual implementation depends on constraint interface
                // In real usage, constraints would be solved with body access
                
                if (maxError < tolerance) {
                    break;
                }
            }
            return null;
        }, Runnable::run);
    }
    
    /**
     * Get performance statistics
     */
    public PhysicsStatistics getStatistics() {
        TaskManager.Stats taskStats = TaskManager.getStats();
        
        return new PhysicsStatistics(
            totalPhysicsSteps.get(),
            parallelSteps.get(),
            taskStats.threads,
            taskStats.active,
            taskStats.queueSize,
            performanceMetrics
        );
    }
    
    private void printStatistics() {
        PhysicsStatistics stats = getStatistics();
        
        System.out.println("\n[VS-Triutilizer] Physics Statistics:");
        System.out.println("  Total physics steps: " + stats.totalSteps);
        System.out.println("  Parallel steps: " + stats.parallelSteps);
        System.out.println("  Worker threads: " + stats.workerThreads);
        System.out.println("  Average step time: " + 
                         getAveragePerformance("physicsStep") + " ms");
        System.out.println();
    }
    
    private void recordPerformance(String metric, long nanos) {
        performanceMetrics.merge(metric + "_total", nanos, Long::sum);
        performanceMetrics.merge(metric + "_count", 1L, Long::sum);
    }
    
    private double getAveragePerformance(String metric) {
        Long total = performanceMetrics.get(metric + "_total");
        Long count = performanceMetrics.get(metric + "_count");
        
        if (total == null || count == null || count == 0) {
            return 0.0;
        }
        
        return (total / (double) count) / 1_000_000.0; // Convert to milliseconds
    }
    
    // Placeholder implementations - to be connected to actual VS collision detection
    private List<ShipPair> performBroadPhase(List<ShipPhysicsSystem> ships) {
        // TODO: Implement actual broad phase collision detection
        return List.of();
    }
    
    private List<PhysicsConstraint> performNarrowPhase(List<ShipPair> pairs) {
        // TODO: Implement actual narrow phase collision detection
        return List.of();
    }
    
    /**
     * Helper classes
     */
    public static class ShipPhysicsSystem {
        private final List<PhysicsConstraint> constraints;
        private final List<RigidBodyState> bodies;
        
        public ShipPhysicsSystem(List<PhysicsConstraint> constraints, 
                                List<RigidBodyState> bodies) {
            this.constraints = constraints;
            this.bodies = bodies;
        }
        
        public List<PhysicsConstraint> getConstraints() { return constraints; }
        public List<RigidBodyState> getBodies() { return bodies; }
    }
    
    private static class ShipPair {
        final ShipPhysicsSystem ship1;
        final ShipPhysicsSystem ship2;
        
        ShipPair(ShipPhysicsSystem ship1, ShipPhysicsSystem ship2) {
            this.ship1 = ship1;
            this.ship2 = ship2;
        }
    }
    
    public static class PhysicsStatistics {
        public final int totalSteps;
        public final int parallelSteps;
        public final int workerThreads;
        public final int activeThreads;
        public final int queueSize;
        public final ConcurrentHashMap<String, Long> metrics;
        
        PhysicsStatistics(int totalSteps, int parallelSteps, int workerThreads,
                         int activeThreads, int queueSize,
                         ConcurrentHashMap<String, Long> metrics) {
            this.totalSteps = totalSteps;
            this.parallelSteps = parallelSteps;
            this.workerThreads = workerThreads;
            this.activeThreads = activeThreads;
            this.queueSize = queueSize;
            this.metrics = metrics;
        }
    }
}
