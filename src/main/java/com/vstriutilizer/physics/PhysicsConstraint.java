package com.vstriutilizer.physics;

import java.util.List;

/**
 * Represents a physics constraint between rigid bodies
 * This is an abstract interface that Valkyrien Skies constraints should implement
 */
public interface PhysicsConstraint {
    
    /**
     * @return The indices of the bodies affected by this constraint
     */
    int[] getAffectedBodyIndices();
    
    /**
     * Check if this constraint shares any bodies with another constraint
     */
    default boolean sharesBodyWith(PhysicsConstraint other) {
        int[] thisIndices = getAffectedBodyIndices();
        int[] otherIndices = other.getAffectedBodyIndices();
        
        for (int i : thisIndices) {
            for (int j : otherIndices) {
                if (i == j) return true;
            }
        }
        return false;
    }
    
    /**
     * Calculate how much this constraint is currently violated
     * @return The constraint error value
     */
    double calculateError(List<RigidBodyState> bodies);
    
    /**
     * Calculate the impulse correction factor (lambda) needed to satisfy the constraint
     * @param error The current constraint error
     * @param bodies The rigid body states
     * @return The lambda value
     */
    double calculateLambda(double error, List<RigidBodyState> bodies);
    
    /**
     * Apply the calculated impulse to the affected bodies
     * @param lambda The impulse magnitude
     * @param bodies The rigid body states
     */
    void applyImpulse(double lambda, List<RigidBodyState> bodies);
    
    /**
     * Get the previous lambda value (for SOR iteration)
     */
    double getPreviousLambda();
    
    /**
     * Set the previous lambda value (for SOR iteration)
     */
    void setPreviousLambda(double lambda);
}
