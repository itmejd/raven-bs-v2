package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FastFall extends Module {
    public SliderSetting mode;
    private ButtonSetting disableAdventure;
    private ButtonSetting ignoreVoid;
    private ButtonSetting disableNoFall;
    private String[] modes = new String[]{"Accelerate", "Timer"};

    private double initialY;
    private boolean isFalling;

    private int fallTicks;
    private int motion;

    private SliderSetting ticks;

    public FastFall() {
        super("FastFall", category.player);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(disableAdventure = new ButtonSetting("Disable adventure", false));
        this.registerSetting(ignoreVoid = new ButtonSetting("Ignore void", true));
        this.registerSetting(disableNoFall = new ButtonSetting("Disable while NoFalling", true));
        this.registerSetting(ticks = new SliderSetting("Intervals", 2, 1, 10, 1));
    }

    public void onDisable() {
        Utils.resetTimer();
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (reset()) {
            if (isFalling) Utils.resetTimer();
            initialY = mc.thePlayer.posY;
            isFalling = false;
            fallTicks = motion = 0;
            return;
        }
        else if ((double) mc.thePlayer.fallDistance >= 2) {
           isFalling = true;
        }

        double predictedY = mc.thePlayer.posY + mc.thePlayer.motionY;
        double distanceFallen = initialY - predictedY;

        if (isFalling && mode.getInput() == 0) {
            ++fallTicks;
            Utils.resetTimer();
            if (fallTicks >= ticks.getInput()) {
                mc.thePlayer.motionY -= ((double) motion / 95);
                fallTicks = 0;
                motion++;
            }
        }

        if (isFalling && mode.getInput() == 1) {
            ++fallTicks;
            Utils.resetTimer();
            if (fallTicks >= ticks.getInput()) {
                Utils.getTimer().timerSpeed = 1.5F;
                fallTicks = 0;
            }
        }
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    private boolean isVoid() {
        return Utils.overVoid(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }

    private boolean reset() {
        if (disableAdventure.isToggled() && mc.playerController.getCurrentGameType().isAdventure()) {
            return true;
        }
        if (ignoreVoid.isToggled() && isVoid()) {
            return true;
        }
        if (Utils.isReplay()) {
            return true;
        }
        if (mc.thePlayer.onGround) {
            return true;
        }
        if (mc.thePlayer.motionY > -0.0784) {
            return true;
        }
        if (mc.thePlayer.capabilities.isCreativeMode) {
            return true;
        }
        if (mc.thePlayer.capabilities.isFlying) {
            return true;
        }
        if (ModuleManager.scaffold.isEnabled) {
            return true;
        }
        if (ModuleManager.noFall.isFalling) {
            return true;
        }
        return false;
    }

}