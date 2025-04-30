package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.Safewalk;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockSnow;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Speed extends Module {
    public SliderSetting speed;
    public static SliderSetting multiplier;
    private ButtonSetting onlyForward;
    private ButtonSetting onlyStrafe;

    private String[] speedOptions = new String[] { "Vanilla", "Float" };

    private boolean canFloat, requireJump;

    public Speed() {
        super("Speed", category.movement, 0);
        this.registerSetting(speed = new SliderSetting("Speed", 0, speedOptions));
        this.registerSetting(multiplier = new SliderSetting("Multiplier", "x", 1.2D, 1.0D, 1.5D, 0.01D));
        this.registerSetting(onlyForward = new ButtonSetting("Only forward", false));
        this.registerSetting(onlyStrafe = new ButtonSetting("Only strafe", false));
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        double horizontalSpeed = Utils.getHorizontalSpeed();
        if (horizontalSpeed == 0.0D) {
            return;
        }
        if (!mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying) {
            return;
        }
        if (mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime && mc.thePlayer.maxHurtTime > 0) {
            return;
        }
        if (Utils.jumpDown()) {
            return;
        }
        if (!settingsMet()) {
            return;
        }
        if (speed.getInput() == 0) {
            double val = multiplier.getInput() - (multiplier.getInput() - 1.0D) * 0.5D;
            Utils.setSpeed(horizontalSpeed * val, true);
        }
        else if (speed.getInput() == 1) {
            if (ModuleUtils.groundTicks <= 8 || floatConditions()) {
                canFloat = true;
            }
            if (!floatConditions()) {
                canFloat = false;
            }
            if (!mc.thePlayer.onGround) {
                requireJump = false;
            }
            if (canFloat && floatConditions() && !requireJump) {
                e.setPosY(e.getPosY() + ModuleUtils.offsetValue);
                if (Utils.isMoving()) {
                    Utils.setSpeed(getFloatSpeed(getSpeedLevel()));
                }
            }
        }
    }

    public boolean settingsMet() {
        if (onlyForward.isToggled() && !Utils.isBindDown(mc.gameSettings.keyBindForward)) {
            return false;
        }
        if (onlyStrafe.isToggled() && mc.thePlayer.moveStrafing == 0.0f) {
            return false;
        }
        return true;
    }

    private boolean floatConditions() {
        int edgeY = (int) Math.round((mc.thePlayer.posY % 1.0D) * 100.0D);
        if (ModuleUtils.stillTicks > 20) {
            requireJump = true;
            return false;
        }
        if (!(mc.thePlayer.posY % 1 == 0) && edgeY >= 10 && !allowedBlocks()) {
            requireJump = true;
            return false;
        }
        if (Safewalk.canSafeWalk()) {
            requireJump = true;
            return false;
        }
        if (ModuleManager.scaffold.isEnabled || ModuleManager.bhop.isEnabled()) {
            requireJump = true;
            return false;
        }
        if (!mc.thePlayer.onGround) {
            return false;
        }
        if (Utils.jumpDown()) {
            return false;
        }
        if (ModuleManager.LongJump.function) {
            return false;
        }
        if (Utils.isBindDown(mc.gameSettings.keyBindSneak)) {
            return false;
        }
        return true;
    }

    private boolean allowedBlocks() {
        Block block = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
        if (block instanceof BlockSnow) {
            return true;
        }
        if (block instanceof BlockCarpet) {
            return true;
        }
        return false;
    }

    private double[] floatSpeedLevels = {0.2, 0.22, 0.28, 0.29, 0.3};

    private double getFloatSpeed(int speedLevel) {
        double min = 0;
        if (mc.thePlayer.moveStrafing != 0 && mc.thePlayer.moveForward != 0) min = 0.003;
        if (speedLevel >= 0) {
            return floatSpeedLevels[speedLevel] - min;
        }
        return floatSpeedLevels[0] - min;
    }

    private int getSpeedLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if (potionEffect.getEffectName().equals("potion.moveSpeed")) {
                return potionEffect.getAmplifier() + 1;
            }
            return 0;
        }
        return 0;
    }
}
