package keystrokesmod.module.impl.movement;

import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.util.Vec3;

public class KeepSprint extends Module {
    public static SliderSetting slow;
    public static ButtonSetting disableWhileJump;
    public static ButtonSetting reduceReachHits;

    public KeepSprint() {
        super("KeepSprint", Module.category.movement, 0);
        this.registerSetting(new DescriptionSetting(new String("Default is 40% motion reduction.")));
        this.registerSetting(slow = new SliderSetting("Slow %", 40.0D, 0.0D, 40.0D, 1.0D));
        this.registerSetting(disableWhileJump = new ButtonSetting("Disable while jumping", false));
        this.registerSetting(reduceReachHits = new ButtonSetting("Only reduce reach hits", false));
    }

    public static double getKeepSprintMotion() {
        boolean vanilla = false;
        if (disableWhileJump.isToggled() && !mc.thePlayer.onGround) {
            vanilla = true;
        }
        else if (reduceReachHits.isToggled() && !mc.thePlayer.capabilities.isCreativeMode) {
            double distance = -1.0;
            final Vec3 getPositionEyes = mc.thePlayer.getPositionEyes(1.0f);
            if (ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && KillAura.target != null) {
                distance = getPositionEyes.distanceTo(KillAura.target.getPositionEyes(Utils.getTimer().renderPartialTicks));
            }
            else if (ModuleManager.reach != null && ModuleManager.reach.isEnabled()) {
                distance = getPositionEyes.distanceTo(mc.objectMouseOver.hitVec);
            }
            if (distance != -1.0 && distance <= 3.0) {
                vanilla = true;
            }
        }
        if (vanilla) {
            return 0.6;
        }
        else {
            return (100.0f - (float) slow.getInput()) / 100.0f;
        }
    }
}