package keystrokesmod.script.model;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovementInput;

public class Simulation {
    private final SimulatedPlayer raw;

    private Simulation(SimulatedPlayer raw) {
        this.raw = raw;
    }

    /**
     * Create a new simulation seeded from the current client player state.
     * Automatically copies the real player's MovementInput.
     */
    public static Simulation create() {
        Minecraft mc = Minecraft.getMinecraft();
        MovementInput input = mc.thePlayer.movementInput;
        SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(input);
        return new Simulation(sim);
    }

    /**
     * Set forward input (-1..1).
     */
    public void setForward(float forward) {
        raw.movementInput.moveForward = forward;
    }

    /**
     * Set strafe input (-1..1).
     */
    public void setStrafe(float strafe) {
        raw.movementInput.moveStrafe = strafe;
    }

    /**
     * Set jump flag.
     */
    public void setJump(boolean jump) {
        raw.movementInput.jump = jump;
    }

    /**
     * Set sneak flag.
     */
    public void setSneak(boolean sneak) {
        raw.movementInput.sneak = sneak;
    }

    /**
     * Set yaw (horizontal rotation) for the simulation.
     */
    public void setYaw(float yaw) {
        raw.rotationYaw = yaw;
    }

    /**
     * Advance the simulator one game tick.
     */
    public void tick() {
        raw.tick();
    }

    /**
     * @return predicted position after tick.
     */
    public Vec3 getPosition() {
        return raw.getPos();
    }

    /**
     * @return raw motion vector (dx, dy, dz) from last tick.
     */
    public Vec3 getMotion() {
        return new Vec3(raw.motionX, raw.motionY, raw.motionZ);
    }

    /**
     * @return true if on ground at end of simulation.
     */
    public boolean onGround() {
        return raw.onGround;
    }
}