package keystrokesmod.helper;

import keystrokesmod.event.*;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.player.Scaffold;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RotationHelper {

    private static RotationHelper INSTANCE = new RotationHelper();

    private Float serverYaw = null;
    private Float serverPitch = null;

    private boolean setRotations = false; // When set to true will tell the client that it will need to apply rotation and fixes in the current tick

    public boolean forceMovementFix = false;

    private Minecraft mc = Minecraft.getMinecraft();

    public float yawOffset = 0F;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreUpdate(PreUpdateEvent e) {
        ClientRotationEvent event = new ClientRotationEvent(this.serverYaw, this.serverPitch);

        MinecraftForge.EVENT_BUS.post(event);

        if (event.yaw != null && !event.yaw.isNaN()) {
            this.serverYaw = event.yaw;
            this.setRotations = true;
        }
        if (event.pitch != null && !event.pitch.isNaN()) {
            this.serverPitch = event.pitch;
            this.setRotations = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreMotion(PreMotionEvent e) {
        if (!this.setRotations) {
            return;
        }
        if (this.serverYaw != null && !this.serverYaw.isNaN()) e.setYaw(this.serverYaw + yawOffset);
        if (this.serverPitch != null && !this.serverPitch.isNaN()) e.setPitch(this.serverPitch);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onStrafe(StrafeEvent e) {
        if (fixMovement()) {
            e.setYaw(this.serverYaw + yawOffset);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onJump(JumpEvent e) {
        if (fixMovement()) {
            e.setYaw(this.serverYaw + yawOffset);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPostInput(PostPlayerInputEvent event) {
        if (!fixMovement()) {
            return;
        }

        float sneakMultiplier = mc.thePlayer.movementInput.sneak ? 0.3F : 1F;

        float yaw = this.serverYaw + yawOffset;
        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput.moveStrafe;

        if (forward == 0 && strafe == 0) {
            return;
        }

        double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(getDirection(mc.thePlayer.rotationYaw + yawOffset, forward, strafe)));

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float pfRaw = -1F; pfRaw <= 1F; pfRaw += 1F) {
            for (float psRaw = -1F; psRaw <= 1F; psRaw += 1F) {
                if (pfRaw == 0 && psRaw == 0) {
                    continue;
                }

                float predictedForward = pfRaw * sneakMultiplier;
                float predictedStrafe = psRaw * sneakMultiplier;

                double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(getDirection(yaw, predictedForward, predictedStrafe)));
                double difference = Math.abs(angle - predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        mc.thePlayer.movementInput.moveForward = closestForward;
        mc.thePlayer.movementInput.moveStrafe = closestStrafe;
        Settings.fixedForward = closestForward;
        Settings.fixedStrafe = closestStrafe;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRunTick(GameTickEvent e) {
        this.serverYaw = this.serverPitch = null;
        this.setRotations = this.forceMovementFix = false;
    }

    private boolean fixMovement() {
        return ((Settings.movementFix != null && Settings.movementFix.isToggled()) || this.forceMovementFix) && this.setRotations;
    }

    public static double getDirection(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public static RotationHelper get() {
        return INSTANCE;
    }

    public void setRotations(float yaw, float pitch) {
        this.serverYaw = yaw;
        this.serverPitch = pitch;
        this.setRotations = true;
    }

    public void setYaw(float yaw) {
        this.serverYaw = yaw;
        this.setRotations = true;
    }

    public void setPitch(float pitch) {
        this.serverPitch = pitch;
        this.setRotations = true;
    }
}