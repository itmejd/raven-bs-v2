package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.impl.accessor.IAccessorEntityLivingBase;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class DelayRemover extends Module {
    public ButtonSetting oldReg, removeJumpTicks, whileMoving;

    public DelayRemover() {
        super("Delay Remover", category.player, 0);
        this.registerSetting(oldReg = new ButtonSetting("1.7 hitreg", true));
        this.registerSetting(removeJumpTicks = new ButtonSetting("Remove jump ticks", false));
        this.registerSetting(whileMoving = new ButtonSetting(" ^ only while moving", false));
        this.closetModule = true;
    }

    public void guiUpdate() {
        this.whileMoving.setVisible(removeJumpTicks.isToggled(), this);
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !mc.inGameHasFocus || !Utils.nullCheck()) {
            return;
        }
        if (oldReg.isToggled()) {
            ((IAccessorMinecraft) mc).setLeftClickCounter(0);
        }
        if (removeJumpTicks.isToggled()) {
            if (whileMoving.isToggled() && mc.thePlayer.motionX == 0.0D && mc.thePlayer.motionZ == 0.0D) {
                return;
            }
            ((IAccessorEntityLivingBase) mc.thePlayer).setJumpTicks(0);
        }
    }
}
