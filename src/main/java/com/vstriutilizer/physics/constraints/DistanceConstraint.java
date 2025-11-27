package com.vstriutilizer.physics.constraints;

import com.vstriutilizer.physics.PhysicsConstraint;
import com.vstriutilizer.physics.RigidBodyState;

import java.util.List;

/**
 * Example constraint: Distance constraint between two bodies
 * 
 * This constraint maintains a fixed distance between two points on two rigid bodies.
 * It's useful for ropes, hinges, and rigid connections.
 */
public class DistanceConstraint implements PhysicsConstraint {
    
    private final int bodyAIndex;
    private final int bodyBIndex;
    private final double targetDistance;
    private double previousLambda = 0.0;
    
    // Local attachment points on each body
    private final double localAX, localAY, localAZ;
    private final double localBX, localBY, localBZ;
    
    // Constraint compliance (0 = rigid, higher = softer)
    private final double compliance;
    
    public DistanceConstraint(int bodyA, int bodyB, 
                              double targetDistance,
                              double localAX, double localAY, double localAZ,
                              double localBX, double localBY, double localBZ,
                              double compliance) {
        this.bodyAIndex = bodyA;
        this.bodyBIndex = bodyB;
        this.targetDistance = targetDistance;
        this.localAX = localAX;
        this.localAY = localAY;
        this.localAZ = localAZ;
        this.localBX = localBX;
        this.localBY = localBY;
        this.localBZ = localBZ;
        this.compliance = compliance;
    }
    
    @Override
    public int[] getAffectedBodyIndices() {
        return new int[] { bodyAIndex, bodyBIndex };
    }
    
    @Override
    public double calculateError(List<RigidBodyState> bodies) {
        RigidBodyState bodyA = bodies.get(bodyAIndex);
        RigidBodyState bodyB = bodies.get(bodyBIndex);
        
        // Transform local points to world space
        double[] worldA = transformToWorld(bodyA, localAX, localAY, localAZ);
        double[] worldB = transformToWorld(bodyB, localBX, localBY, localBZ);
        
        // Calculate current distance
        double dx = worldB[0] - worldA[0];
        double dy = worldB[1] - worldA[1];
        double dz = worldB[2] - worldA[2];
        double currentDistance = Math.sqrt(dx*dx + dy*dy + dz*dz);
        
        // Error is the difference from target distance
        return currentDistance - targetDistance;
    }
    
    @Override
    public double calculateLambda(double error, List<RigidBodyState> bodies) {
        RigidBodyState bodyA = bodies.get(bodyAIndex);
        RigidBodyState bodyB = bodies.get(bodyBIndex);
        
        // Get world positions
        double[] worldA = transformToWorld(bodyA, localAX, localAY, localAZ);
        double[] worldB = transformToWorld(bodyB, localBX, localBY, localBZ);
        
        // Direction vector
        double dx = worldB[0] - worldA[0];
        double dy = worldB[1] - worldA[1];
        double dz = worldB[2] - worldA[2];
        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        
        if (len < 1e-6) return 0.0;
        
        dx /= len;
        dy /= len;
        dz /= len;
        
        // Calculate effective mass in constraint direction
        double effectiveMass = calculateEffectiveMass(bodyA, bodyB, 
                                                      worldA, worldB, 
                                                      dx, dy, dz);
        
        if (effectiveMass < 1e-6) return 0.0;
        
        // Calculate lambda with compliance
        double lambda = -error / (effectiveMass + compliance);
        
        return lambda;
    }
    
    @Override
    public void applyImpulse(double lambda, List<RigidBodyState> bodies) {
        RigidBodyState bodyA = bodies.get(bodyAIndex);
        RigidBodyState bodyB = bodies.get(bodyBIndex);
        
        // Get world positions
        double[] worldA = transformToWorld(bodyA, localAX, localAY, localAZ);
        double[] worldB = transformToWorld(bodyB, localBX, localBY, localBZ);
        
        // Direction vector
        double dx = worldB[0] - worldA[0];
        double dy = worldB[1] - worldA[1];
        double dz = worldB[2] - worldA[2];
        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        
        if (len < 1e-6) return;
        
        dx /= len;
        dy /= len;
        dz /= len;
        
        // Linear impulse
        double impX = lambda * dx;
        double impY = lambda * dy;
        double impZ = lambda * dz;
        
        // Apply to body A (negative)
        bodyA.applyLinearImpulse(-impX, -impY, -impZ);
        
        // Apply to body B (positive)
        bodyB.applyLinearImpulse(impX, impY, impZ);
        
        // Calculate angular impulses (r x impulse)
        double[] rA = {worldA[0] - bodyA.posX, worldA[1] - bodyA.posY, worldA[2] - bodyA.posZ};
        double[] rB = {worldB[0] - bodyB.posX, worldB[1] - bodyB.posY, worldB[2] - bodyB.posZ};
        
        // Cross product for angular impulse
        double angImpAX = rA[1] * impZ - rA[2] * impY;
        double angImpAY = rA[2] * impX - rA[0] * impZ;
        double angImpAZ = rA[0] * impY - rA[1] * impX;
        
        double angImpBX = rB[1] * impZ - rB[2] * impY;
        double angImpBY = rB[2] * impX - rB[0] * impZ;
        double angImpBZ = rB[0] * impY - rB[1] * impX;
        
        bodyA.applyAngularImpulse(-angImpAX, -angImpAY, -angImpAZ);
        bodyB.applyAngularImpulse(angImpBX, angImpBY, angImpBZ);
    }
    
    @Override
    public double getPreviousLambda() {
        return previousLambda;
    }
    
    @Override
    public void setPreviousLambda(double lambda) {
        this.previousLambda = lambda;
    }
    
    /**
     * Transform a local point to world space using the body's position and rotation
     */
    private double[] transformToWorld(RigidBodyState body, double lx, double ly, double lz) {
        // Apply quaternion rotation: v' = q * v * q^-1
        // Simplified for performance
        double qw = body.rotW, qx = body.rotX, qy = body.rotY, qz = body.rotZ;
        
        double tx = 2.0 * (qy * lz - qz * ly);
        double ty = 2.0 * (qz * lx - qx * lz);
        double tz = 2.0 * (qx * ly - qy * lx);
        
        double rx = lx + qw * tx + (qy * tz - qz * ty);
        double ry = ly + qw * ty + (qz * tx - qx * tz);
        double rz = lz + qw * tz + (qx * ty - qy * tx);
        
        return new double[] {
            body.posX + rx,
            body.posY + ry,
            body.posZ + rz
        };
    }
    
    /**
     * Calculate the effective mass of the constraint
     */
    private double calculateEffectiveMass(RigidBodyState bodyA, RigidBodyState bodyB,
                                         double[] worldA, double[] worldB,
                                         double nx, double ny, double nz) {
        double em = 0.0;
        
        // Linear contribution
        if (!bodyA.isStatic) em += bodyA.invMass;
        if (!bodyB.isStatic) em += bodyB.invMass;
        
        // Angular contribution
        if (!bodyA.isStatic) {
            double[] rA = {worldA[0] - bodyA.posX, worldA[1] - bodyA.posY, worldA[2] - bodyA.posZ};
            double[] rxn = {rA[1]*nz - rA[2]*ny, rA[2]*nx - rA[0]*nz, rA[0]*ny - rA[1]*nx};
            em += (rxn[0]*rxn[0]*bodyA.invInertiaXX + 
                   rxn[1]*rxn[1]*bodyA.invInertiaYY + 
                   rxn[2]*rxn[2]*bodyA.invInertiaZZ);
        }
        
        if (!bodyB.isStatic) {
            double[] rB = {worldB[0] - bodyB.posX, worldB[1] - bodyB.posY, worldB[2] - bodyB.posZ};
            double[] rxn = {rB[1]*nz - rB[2]*ny, rB[2]*nx - rB[0]*nz, rB[0]*ny - rB[1]*nx};
            em += (rxn[0]*rxn[0]*bodyB.invInertiaXX + 
                   rxn[1]*rxn[1]*bodyB.invInertiaYY + 
                   rxn[2]*rxn[2]*bodyB.invInertiaZZ);
        }
        
        return em;
    }
}
