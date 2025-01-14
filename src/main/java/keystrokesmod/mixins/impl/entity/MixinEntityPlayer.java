package keystrokesmod.mixins.impl.entity;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.Reduce;
import keystrokesmod.module.impl.movement.KeepSprint;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase {
    public MixinEntityPlayer(World p_i1594_1_) {
        super(p_i1594_1_);
    }
    @Shadow
    public abstract ItemStack getHeldItem();

    @ModifyConstant(method = "attackTargetEntityWithCurrentItem", constant = @Constant(doubleValue = 0.6))
    private double multiplyMotion(final double originalValue) {
        if (ModuleManager.reduce != null && ModuleManager.reduce.isEnabled()) {
            return Reduce.getReduceMotion();
        }
        else if (ModuleManager.keepSprint != null && ModuleManager.keepSprint.isEnabled()) {
            return KeepSprint.getKeepSprintMotion();
        }
        return originalValue;
    }

    @Redirect(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"))
    public void setSprinting(final EntityPlayer entityPlayer, final boolean sprinting) {
        if (ModuleManager.keepSprint != null && ModuleManager.keepSprint.isEnabled()) {
            return;
        }
        entityPlayer.setSprinting(sprinting);
    }

    @Inject(method = "isBlocking", at = @At("RETURN"), cancellable = true)
    private void isBlocking(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && ModuleManager.killAura.blockingClient && ((Object) this) == Minecraft.getMinecraft().thePlayer) {
            cir.setReturnValue(true);
        }
        cir.setReturnValue(cir.getReturnValue());
    }

}