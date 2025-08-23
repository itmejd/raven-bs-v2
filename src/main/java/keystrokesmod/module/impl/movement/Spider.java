package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Spider extends Module {
    public SliderSetting mode;
    private String[] modes = new String[]{"Spoof", "Motion"};

    public Spider() {
        super("Spider", category.player);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (!mc.thePlayer.isCollidedHorizontally) {
            return;
        }
        mc.thePlayer.stepHeight = 0.f;
        if (mc.thePlayer.motionY == 0) {
            e.setOnGround(true);
        }
        else {
            if (mc.thePlayer.posY < 0.f) {
                mc.thePlayer.posY = 0.f;
            }
        }
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

}