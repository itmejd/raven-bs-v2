package keystrokesmod.mixin.impl.client;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.Safewalk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameSettings.class)
public class MixinGameSettings {

    @Overwrite
    public static boolean isKeyDown(KeyBinding key) {
        if (key == Minecraft.getMinecraft().gameSettings.keyBindSneak && ModuleManager.safeWalk != null) {
            Safewalk safewalk = ModuleManager.safeWalk;
            if (safewalk.isEnabled() && safewalk.isSneaking) {
                return true;
            }
        }
        return key.getKeyCode() == 0 ? false : (key.getKeyCode() < 0 ? Mouse.isButtonDown(key.getKeyCode() + 100) : Keyboard.isKeyDown(key.getKeyCode()));
    }
}