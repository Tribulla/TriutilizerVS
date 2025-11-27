package com.vstriutilizer.physics;

import com.triutilizer.core.concurrency.TaskManager;
import com.triutilizer.core.concurrency.Priority;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.List;

/**
 * Parallel Red-Black Successive Over-Relaxation (SOR) Physics Solver
 * 
 * This solver uses the Red-Black SOR method which is specifically designed
 * for parallel execution. Unlike Gauss-Seidel (sequential) and Jacobi (high memory),
 * Red-Black SOR achieves:
 * - Parallel execution capability (unlike Gauss-Seidel)
 * - Memory efficiency (unlike Jacobi which requires full state copies)
 * - Fast convergence (better than standard Jacobi)
 * 
 * The Red-Black coloring scheme allows all "red" constraints to be solved
 * simultaneously, then all "black" constraints, without data races.
 */
public class ParallelRedBlackSORSolver {
    
    private static final double DEFAULT_OMEGA = 1.8; // SOR relaxation parameter
    private static final int DEFAULT_MAX_ITERATIONS = 10;
    private static final double DEFAULT_TOLERANCE = 1e-4;
    
    private final double omega;
    private final int maxIterations;
    private final double tolerance;
    
    public ParallelRedBlackSORSolver() {
        this(DEFAULT_OMEGA, DEFAULT_MAX_ITERATIONS, DEFAULT_TOLERANCE);
    }
    
    public ParallelRedBlackSORSolver(double omega, int maxIterations, double tolerance) {
        this.omega = omega;
        this.maxIterations = maxIterations;
        this.tolerance = tolerance;
    }
    
    /**
     * Solves a system of constraints in parallel using Red-Black SOR
     * 
     * @param constraints List of physics constraints to solve
     * @param bodies List of rigid bodies affected by constraints
     * @return CompletableFuture that completes when solving is done
     */
    public CompletableFuture<SolverResult> solve(
            List<PhysicsConstraint> constraints,
            List<RigidBodyState> bodies) {
        
        // Partition constraints into red and black sets
        return TaskManager.submit(() -> {
            List<PhysicsConstraint> redConstraints = new ArrayList<>();
            List<PhysicsConstraint> blackConstraints = new ArrayList<>();
            
            partitionConstraints(constraints, redConstraints, blackConstraints);
            
            return new ConstraintPartition(redConstraints, blackConstraints);
            
        }, Priority.HIGH).thenCompose(partition -> {
            // Perform Red-Black SOR iterations
            return performSORIterations(partition, bodies);
        });
    }
    
    /**
     * Partitions constraints into red and black sets using graph coloring.
     * Constraints that don't share bodies can be in the same color group.
     */
    private void partitionConstraints(
            List<PhysicsConstraint> constraints,
            List<PhysicsConstraint> red,
            List<PhysicsConstraint> black) {
        
        boolean[] isRed = new boolean[constraints.size()];
        
        // Simple greedy coloring algorithm
        for (int i = 0; i < constraints.size(); i++) {
            PhysicsConstraint constraint = constraints.get(i);
            boolean canBeRed = true;
            
            // Check if any already-colored red constraint conflicts
            for (int j = 0; j < i; j++) {
                if (isRed[j] && constraintsConflict(constraint, constraints.get(j))) {
                    canBeRed = false;
                    break;
                }
            }
            
            isRed[i] = canBeRed;
            if (canBeRed) {
                red.add(constraint);
            } else {
                black.add(constraint);
            }
        }
    }
    
    /**
     * Two constraints conflict if they share any rigid bodies
     */
    private boolean constraintsConflict(PhysicsConstraint c1, PhysicsConstraint c2) {
        return c1.sharesBodyWith(c2);
    }
    
    /**
     * Performs the Red-Black SOR iteration loop
     */
    private CompletableFuture<SolverResult> performSORIterations(
            ConstraintPartition partition,
            List<RigidBodyState> bodies) {
        
        return TaskManager.submit(() -> {
            double maxError = Double.MAX_VALUE;
            int iteration = 0;
            
            while (iteration < maxIterations && maxError > tolerance) {
                // Solve red constraints in parallel
                double redError = solveConstraintSetParallel(
                    partition.redConstraints, bodies, omega
                ).join();
                
                // Solve black constraints in parallel
                double blackError = solveConstraintSetParallel(
                    partition.blackConstraints, bodies, omega
                ).join();
                
                maxError = Math.max(redError, blackError);
                iteration++;
            }
            
            return new SolverResult(iteration, maxError, maxError <= tolerance);
            
        }, Priority.HIGH);
    }
    
    /**
     * Solves a set of constraints in parallel
     * Since all constraints in the set don't conflict, they can be solved simultaneously
     */
    private CompletableFuture<Double> solveConstraintSetParallel(
            List<PhysicsConstraint> constraints,
            List<RigidBodyState> bodies,
            double omega) {
        
        if (constraints.isEmpty()) {
            return CompletableFuture.completedFuture(0.0);
        }
        
        // Process constraints in parallel chunks for efficiency
        return TaskManager.mapChunked(constraints, 16, chunk -> {
            double maxChunkError = 0.0;
            
            for (PhysicsConstraint constraint : chunk) {
                double error = solveConstraint(constraint, bodies, omega);
                maxChunkError = Math.max(maxChunkError, Math.abs(error));
            }
            
            return maxChunkError;
            
        }, Priority.HIGH).thenApply(errors -> {
            // Find maximum error across all chunks
            return errors.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
        });
    }
    
    /**
     * Solves a single constraint using SOR
     * This updates the velocities/impulses of the bodies
     */
    private double solveConstraint(
            PhysicsConstraint constraint,
            List<RigidBodyState> bodies,
            double omega) {
        
        // Get the constraint error (how much it's violated)
        double error = constraint.calculateError(bodies);
        
        // Calculate the correction impulse
        double lambda = constraint.calculateLambda(error, bodies);
        
        // Apply SOR relaxation
        double relaxedLambda = omega * lambda + (1.0 - omega) * constraint.getPreviousLambda();
        constraint.setPreviousLambda(relaxedLambda);
        
        // Apply impulse to bodies (thread-safe since constraints don't overlap)
        constraint.applyImpulse(relaxedLambda, bodies);
        
        return error;
    }
    
    /**
     * Helper class to hold partitioned constraints
     */
    private static class ConstraintPartition {
        final List<PhysicsConstraint> redConstraints;
        final List<PhysicsConstraint> blackConstraints;
        
        ConstraintPartition(List<PhysicsConstraint> red, List<PhysicsConstraint> black) {
            this.redConstraints = red;
            this.blackConstraints = black;
        }
    }
    
    /**
     * Result of the solver
     */
    public static class SolverResult {
        public final int iterations;
        public final double finalError;
        public final boolean converged;
        
        public SolverResult(int iterations, double finalError, boolean converged) {
            this.iterations = iterations;
            this.finalError = finalError;
            this.converged = converged;
        }
    }
}
