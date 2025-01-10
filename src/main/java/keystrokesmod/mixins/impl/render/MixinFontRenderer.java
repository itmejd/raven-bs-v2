package keystrokesmod.mixins.impl.render;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.other.NameHider;
import keystrokesmod.module.impl.render.AntiShuffle;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FontRenderer.class)
public class MixinFontRenderer {
    @ModifyVariable(method = "renderString", at = @At("HEAD"), require = 1, ordinal = 0, argsOnly = true)
    private String renderString(String string) {
        if (string == null)
            return null;
        if ((ModuleManager.nameHider != null) && ModuleManager.nameHider.isEnabled()) {
            string = NameHider.getFakeName(string);
        }
        if ((ModuleManager.antiShuffle != null) && ModuleManager.antiShuffle.isEnabled()) {
            string = AntiShuffle.removeObfuscation(string);
        }

        return string;
    }

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), require = 1, ordinal = 0, argsOnly = true)
    private String getStringWidth(String string) {
        if (string == null)
            return null;
        if ((ModuleManager.nameHider != null) && ModuleManager.nameHider.isEnabled()) {
            string = NameHider.getFakeName(string);
        }
        if ((ModuleManager.antiShuffle != null) && ModuleManager.antiShuffle.isEnabled()) {
            string = AntiShuffle.removeObfuscation(string);
        }

        return string;
    }
}
