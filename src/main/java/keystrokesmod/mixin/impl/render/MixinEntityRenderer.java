package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.ModuleManager;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Redirect(method = "hurtCameraEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V"))
    public void injectNoHurtCam(float angle, float x, float y, float z) {
        if (ModuleManager.noHurtCam != null && ModuleManager.noHurtCam.isEnabled()) {
            angle = (float) (angle / 14 * ModuleManager.noHurtCam.multiplier.getInput());
        }
        GlStateManager.rotate(angle, x, y, z);
    }

    @Redirect(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"))
    public double injectNoCameraClip(Vec3 raytrace, Vec3 original) {
        if (ModuleManager.noCameraClip != null && ModuleManager.noCameraClip.isEnabled()) {
            return ModuleManager.extendCamera != null && ModuleManager.extendCamera.isEnabled() ? ModuleManager.extendCamera.distance.getInput() : 4;
        }
        return raytrace.distanceTo(original);
    }

    @Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean redirectSetupFog(EntityLivingBase entity, Potion potion) {
        if (ModuleManager.antiDebuff != null && ModuleManager.antiDebuff.canRemoveBlindness(potion)) {
            return false;
        }
        return entity.isPotionActive(potion);
    }

    @Redirect(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean redirectFogColor(EntityLivingBase entity, Potion potion) {
        if (ModuleManager.antiDebuff != null && ModuleManager.antiDebuff.canRemoveBlindness(potion)) {
            return false;
        }
        return entity.isPotionActive(potion);
    }

    @Redirect(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean redirectSetupCameraTransform(EntityLivingBase entity, Potion potion) {
        if (ModuleManager.antiDebuff != null && ModuleManager.antiDebuff.canRemoveNausea(potion)) {
            return false;
        }
        return entity.isPotionActive(potion);
    }
}