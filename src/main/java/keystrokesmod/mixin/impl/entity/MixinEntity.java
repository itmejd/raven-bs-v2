package keystrokesmod.mixin.impl.entity;

import keystrokesmod.event.ClientLookEvent;
import keystrokesmod.event.PlayerMoveEvent;
import keystrokesmod.event.StepHeightEvent;
import keystrokesmod.event.StrafeEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.Safewalk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow
    public double motionX;
    @Shadow
    public double motionZ;
    @Shadow
    public float rotationYaw;

    @ModifyVariable(method = "moveEntity", at = @At(value = "STORE", ordinal = 0), name = "flag")
    private boolean injectSafeWalk(boolean flag) {
        Entity entity = (Entity) (Object) this;
        Minecraft mc = Minecraft.getMinecraft();

        if (entity != null && entity == mc.thePlayer && entity.onGround) {
            if (Safewalk.canSafeWalk() || (ModuleManager.scaffold != null && ModuleManager.scaffold.canSafewalk())) {
                return true;
            }
        }
        return flag;
    }

    @Overwrite
    public void moveFlying(float strafe, float forward, float friction) {
        StrafeEvent strafeEvent = new StrafeEvent(strafe, forward, friction, this.rotationYaw);
        if((Object) this == Minecraft.getMinecraft().thePlayer) {
            MinecraftForge.EVENT_BUS.post(strafeEvent);
        }

        strafe = strafeEvent.getStrafe();
        forward = strafeEvent.getForward();
        friction = strafeEvent.getFriction();
        float yaw = strafeEvent.getYaw();

        float f = (strafe * strafe) + (forward * forward);

        if (f >= 1.0E-4F) {
            f = MathHelper.sqrt_float(f);
            if (f < 1.0F) {
                f = 1.0F;
            }

            f = friction / f;
            strafe *= f;
            forward *= f;
            float f1 = MathHelper.sin(yaw * (float)Math.PI / 180.0F);
            float f2 = MathHelper.cos(yaw * (float)Math.PI / 180.0F);
            this.motionX += strafe * f2 - forward * f1;
            this.motionZ += forward * f2 + strafe * f1;
        }
    }

    @Redirect(method = "moveEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;stepHeight:F", opcode = Opcodes.GETFIELD, ordinal = 0))
    private float redirectStepHeight(Entity instance) {
        StepHeightEvent stepHeightEvent = new StepHeightEvent(instance, instance.stepHeight);
        MinecraftForge.EVENT_BUS.post(stepHeightEvent);
        return stepHeightEvent.stepHeight;
    }

    @Overwrite
    protected final Vec3 getVectorForRotation(float pitch, float yaw) {

        ClientLookEvent event = new ClientLookEvent(yaw, pitch);

        MinecraftForge.EVENT_BUS.post(event);

        pitch = event.pitch;
        yaw = event.yaw;

        float f = MathHelper.cos(-yaw * ((float)Math.PI / 180F) - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * ((float)Math.PI / 180F) - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * ((float)Math.PI / 180F));
        float f3 = MathHelper.sin(-pitch * ((float)Math.PI / 180F));

        return new Vec3(f1 * f2, f3, f * f2);
    }

    @Inject(method = "moveEntity", at = @At("HEAD"))
    private void injectPlayerMoveEvent(double x, double y, double z, CallbackInfo ci) {
        if (((Object) this) instanceof EntityPlayerSP) {
            MinecraftForge.EVENT_BUS.post(new PlayerMoveEvent(x, y, z));
        }
    }
}