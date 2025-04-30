package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.potion.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @Redirect(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean redirectRenderGameOverlay(GuiIngame gui, Potion potion) {
        if (ModuleManager.antiDebuff != null && ModuleManager.antiDebuff.canRemoveNausea(potion)) {
            return false;
        }
        return Minecraft.getMinecraft().thePlayer.isPotionActive(potion);
    }

}
