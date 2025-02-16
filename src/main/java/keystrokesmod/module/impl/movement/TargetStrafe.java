package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TargetStrafe extends Module {
    private ButtonSetting bhopOnly;

    private double angle;
    private float radius = 0.6f;

    public TargetStrafe() {
        super("TargetStrafe", category.movement);
        this.registerSetting(bhopOnly = new ButtonSetting("Require bhop", false));
    }

    public void guiUpdate() {

    }

    /*@Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }*/

    @SubscribeEvent(priority = EventPriority.LOWEST) // called last in order to apply fix
    public void onMoveInput(PrePlayerInputEvent e) {
        if (bhopOnly.isToggled() && !ModuleManager.bhop.isEnabled()) {
            return;
        }
        if (ModuleManager.scaffold.isEnabled) {
            return;
        }
        if (KillAura.target == null) {
            return;
        }
        EntityLivingBase targetPosition = KillAura.target;
        angle += 1;

        double offsetX = radius * Math.cos(angle);
        double offsetZ = radius * Math.sin(angle);
        double directionX = targetPosition.getPosition().getX() + offsetX - mc.thePlayer.posX;
        double directionZ = targetPosition.getPosition().getZ() + offsetZ - mc.thePlayer.posZ;
        double magnitude = Math.sqrt(directionX * directionX + directionZ * directionZ);
        if (magnitude > 0.01) {
            directionX /= magnitude;
            directionZ /= magnitude;
            double yawRadians = Math.toRadians(-mc.thePlayer.rotationYaw);
            double rotatedX = directionX * Math.cos(yawRadians) - directionZ * Math.sin(yawRadians);
            double rotatedZ = directionX * Math.sin(yawRadians) + directionZ * Math.cos(yawRadians);
            e.setStrafe((float) rotatedX);
            e.setForward((float) rotatedZ);
        }
    }

    public void onDisable() {

    }
}