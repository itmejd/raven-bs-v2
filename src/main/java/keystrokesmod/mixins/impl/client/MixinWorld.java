package keystrokesmod.mixins.impl.client;

import keystrokesmod.module.ModuleManager;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(World.class)
public class MixinWorld {

    @Inject(method = "getThunderStrength", at = @At("RETURN"), cancellable = true)
    public void setThunderStrength(CallbackInfoReturnable<Float> clr) {
        if (ModuleManager.weather != null && ModuleManager.weather.isEnabled() && ModuleManager.weather.lightning.getInput() > 0) {
            clr.setReturnValue((float) ModuleManager.weather.lightning.getInput());
        }
    }

    @Inject(method = "getRainStrength", at = @At("RETURN"), cancellable = true)
    public void setPrecipitationStrength(CallbackInfoReturnable<Float> clr) {
        if (ModuleManager.weather != null && ModuleManager.weather.isEnabled() && ModuleManager.weather.rain.isToggled()) {
            clr.setReturnValue(1F);
        }
    }
}