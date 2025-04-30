package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Dolphin extends Module {
    public SliderSetting horSpeed;
    public SliderSetting verSpeed;
    public ButtonSetting buoyant;
    public ButtonSetting disableUsing;
    public ButtonSetting disableVerticalWhileMoving;
    public ButtonSetting forwardOnly;

    public Dolphin() {
        super("Dolphin", category.movement, 0);
        this.registerSetting(horSpeed = new SliderSetting("Horizontal speed", 1.0, 1.0, 8.0, 0.1));
        this.registerSetting(verSpeed = new SliderSetting("Vertical speed", 1.0, 1.0, 8.0, 0.1));
        this.registerSetting(buoyant = new ButtonSetting("Buoyant", false));
        this.registerSetting(disableUsing = new ButtonSetting("Disable while using", true));
        this.registerSetting(disableVerticalWhileMoving = new ButtonSetting("Disable vertical while moving", false));
        this.registerSetting(forwardOnly = new ButtonSetting("Forward only", true));
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent event) {
        if (!mc.thePlayer.isInWater() || mc.thePlayer.capabilities.isFlying) {
            return;
        }
        if (disableUsing.isToggled() && mc.thePlayer.isUsingItem()) {
            return;
        }
        if (forwardOnly.isToggled() && !Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
            return;
        }
        if (buoyant.isToggled()) {
            mc.thePlayer.motionY = 0.0;
        }
        if (Utils.isUserMoving()) {
            double horizontalSpeed = 0.078 * horSpeed.getInput();
            Utils.setSpeed(horizontalSpeed);
            if (disableVerticalWhileMoving.isToggled()) {
                return;
            }
        }
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
            mc.thePlayer.motionY = 0.02 + 0.04 * verSpeed.getInput();
        }
        else if (!mc.thePlayer.onGround && Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            mc.thePlayer.movementInput.sneak = false;
            mc.thePlayer.motionY = -0.1 - 0.03 * verSpeed.getInput();
        }
        else if (buoyant.isToggled()) {
            mc.thePlayer.motionY = 0.0;
        }
    }
}
