package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Momentum extends Module {
    public ButtonSetting renderTimer;

    public boolean blink;
    private int wait;
    private int latestY;
    private boolean setY;

    public Momentum() {
        super("Momentum", category.movement, 0);
        this.registerSetting(renderTimer = new ButtonSetting("Render Timer", false));
    }

    public void onDisable() {
        reset();
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (mc.thePlayer.onGround && !blink) {
            reset();
            return;
        }
        if (mc.thePlayer.motionY < -0.0784000015258789) {
            if (!setY) {
                latestY = (int) mc.thePlayer.posY;
                setY = true;
            }
        }
        else {
            setY = false;
        }
        if (Utils.fallDistZ() >= 0 && Utils.fallDistZ() <= 2 && latestY - mc.thePlayer.posY >= 2) {
            blink = true;
        }
        if (blink) {
            wait++;
            if (!mc.thePlayer.onGround && wait >= 2 || ModuleUtils.groundTicks >= 3) {
                reset();
            }
        }






    }


    private void reset() {
        blink = setY = false;
        wait = latestY = 0;
    }


}