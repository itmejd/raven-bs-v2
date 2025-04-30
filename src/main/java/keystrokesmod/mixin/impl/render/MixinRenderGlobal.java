package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal { // credit: pablolnmak
    @Shadow
    @Final
    private Minecraft mc;

    @Unique
    private boolean shouldRender() {
        return ModuleManager.playerESP != null && ModuleManager.playerESP.isEnabled() && ModuleManager.playerESP.outline.isToggled();
    }
}