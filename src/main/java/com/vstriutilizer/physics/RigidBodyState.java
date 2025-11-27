package com.vstriutilizer.physics;

/**
 * Represents the state of a rigid body in the physics simulation
 * This is a simplified representation - adapt to match Valkyrien Skies' actual body structure
 */
public class RigidBodyState {
    
    // Linear motion
    public volatile double posX, posY, posZ;
    public volatile double velX, velY, velZ;
    public volatile double forceX, forceY, forceZ;
    public volatile double mass;
    public volatile double invMass;
    
    // Angular motion (quaternion or euler angles)
    public volatile double rotW, rotX, rotY, rotZ; // quaternion
    public volatile double angVelX, angVelY, angVelZ;
    public volatile double torqueX, torqueY, torqueZ;
    
    // Inertia tensor (simplified as diagonal for now)
    public volatile double inertiaXX, inertiaYY, inertiaZZ;
    public volatile double invInertiaXX, invInertiaYY, invInertiaZZ;
    
    // Metadata
    public int bodyId;
    public boolean isStatic;
    
    public RigidBodyState(int bodyId) {
        this.bodyId = bodyId;
        this.mass = 1.0;
        this.invMass = 1.0;
        this.isStatic = false;
        
        // Default identity rotation
        this.rotW = 1.0;
        this.rotX = 0.0;
        this.rotY = 0.0;
        this.rotZ = 0.0;
        
        // Default inertia
        this.inertiaXX = this.inertiaYY = this.inertiaZZ = 1.0;
        this.invInertiaXX = this.invInertiaYY = this.invInertiaZZ = 1.0;
    }
    
    /**
     * Apply an impulse to this body
     * Thread-safe for multiple constraints applying impulses
     */
    public synchronized void applyLinearImpulse(double impX, double impY, double impZ) {
        if (!isStatic) {
            velX += impX * invMass;
            velY += impY * invMass;
            velZ += impZ * invMass;
        }
    }
    
    /**
     * Apply an angular impulse to this body
     * Thread-safe for multiple constraints applying impulses
     */
    public synchronized void applyAngularImpulse(double impX, double impY, double impZ) {
        if (!isStatic) {
            angVelX += impX * invInertiaXX;
            angVelY += impY * invInertiaYY;
            angVelZ += impZ * invInertiaZZ;
        }
    }
    
    /**
     * Integrate the body's velocity to update position
     * This should be called on the main thread after physics solving
     */
    public void integrate(double deltaTime) {
        if (isStatic) return;
        
        // Update position
        posX += velX * deltaTime;
        posY += velY * deltaTime;
        posZ += velZ * deltaTime;
        
        // Update rotation (simplified quaternion integration)
        double halfDt = deltaTime * 0.5;
        double qw = rotW + halfDt * (-rotX * angVelX - rotY * angVelY - rotZ * angVelZ);
        double qx = rotX + halfDt * (rotW * angVelX + rotY * angVelZ - rotZ * angVelY);
        double qy = rotY + halfDt * (rotW * angVelY + rotZ * angVelX - rotX * angVelZ);
        double qz = rotZ + halfDt * (rotW * angVelZ + rotX * angVelY - rotY * angVelX);
        
        // Normalize quaternion
        double norm = Math.sqrt(qw*qw + qx*qx + qy*qy + qz*qz);
        rotW = qw / norm;
        rotX = qx / norm;
        rotY = qy / norm;
        rotZ = qz / norm;
        
        // Apply forces and torques
        velX += forceX * invMass * deltaTime;
        velY += forceY * invMass * deltaTime;
        velZ += forceZ * invMass * deltaTime;
        
        angVelX += torqueX * invInertiaXX * deltaTime;
        angVelY += torqueY * invInertiaYY * deltaTime;
        angVelZ += torqueZ * invInertiaZZ * deltaTime;
        
        // Clear forces for next frame
        forceX = forceY = forceZ = 0.0;
        torqueX = torqueY = torqueZ = 0.0;
    }
    
    /**
     * Create a copy of this state for reading in worker threads
     */
    public RigidBodyState snapshot() {
        RigidBodyState copy = new RigidBodyState(this.bodyId);
        copy.posX = this.posX;
        copy.posY = this.posY;
        copy.posZ = this.posZ;
        copy.velX = this.velX;
        copy.velY = this.velY;
        copy.velZ = this.velZ;
        copy.mass = this.mass;
        copy.invMass = this.invMass;
        copy.rotW = this.rotW;
        copy.rotX = this.rotX;
        copy.rotY = this.rotY;
        copy.rotZ = this.rotZ;
        copy.angVelX = this.angVelX;
        copy.angVelY = this.angVelY;
        copy.angVelZ = this.angVelZ;
        copy.inertiaXX = this.inertiaXX;
        copy.inertiaYY = this.inertiaYY;
        copy.inertiaZZ = this.inertiaZZ;
        copy.invInertiaXX = this.invInertiaXX;
        copy.invInertiaYY = this.invInertiaYY;
        copy.invInertiaZZ = this.invInertiaZZ;
        copy.isStatic = this.isStatic;
        return copy;
    }
}
