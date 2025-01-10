package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class Safewalk extends Module {
    private SliderSetting shiftDelay;
    private SliderSetting motion;
    public static ButtonSetting shift, blocksOnly, pitchCheck, disableOnForward;
    private boolean isSneaking;
    private long lastShift = 0L;

    public Safewalk() {
        super("Safewalk", Module.category.player, 0);
        this.registerSetting(shift = new ButtonSetting("Shift", false));
        this.registerSetting(shiftDelay = new SliderSetting("Delay until next shift", 0.0, 0.0, 800.0, 10.0));
        this.registerSetting(motion = new SliderSetting("Motion", 1.0, 0.5, 1.2, 0.01));
        this.registerSetting(blocksOnly = new ButtonSetting("Blocks only", true));
        this.registerSetting(disableOnForward = new ButtonSetting("Disable on forward", false));
        this.registerSetting(pitchCheck = new ButtonSetting("Pitch check", false));
    }

    public void onDisable() {
        if (shift.isToggled() && Utils.isEdgeOfBlock()) {
            this.setSneakState(false);
        }
        isSneaking = false;
        lastShift = 0L;
    }

    public void onUpdate() {
        if (motion.getInput() != 1.0 && mc.thePlayer.onGround && Utils.isMoving() && (!pitchCheck.isToggled() || mc.thePlayer.rotationPitch >= 70.0f)) {
            mc.thePlayer.motionX *= motion.getInput();
            mc.thePlayer.motionZ *= motion.getInput();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        if (!shift.isToggled() || !Utils.nullCheck()) {
            return;
        }
        if (mc.thePlayer.onGround && Utils.isEdgeOfBlock()) {
            if (blocksOnly.isToggled()) {
                final ItemStack getHeldItem = mc.thePlayer.getHeldItem();
                if (getHeldItem == null || !(getHeldItem.getItem() instanceof ItemBlock)) {
                    this.setSneakState(false);
                    return;
                }
            }
            if (disableOnForward.isToggled() && Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
                this.setSneakState(false);
                return;
            }
            if (pitchCheck.isToggled() && mc.thePlayer.rotationPitch < 70.0f) {
                this.setSneakState(false);
                return;
            }
            this.setSneakState(true);
        } else if (this.isSneaking) {
            this.setSneakState(false);
        }
        if (this.isSneaking && mc.thePlayer.capabilities.isFlying) {
            this.setSneakState(false);
        }
    }

    @SubscribeEvent
    public void onGuiOpen(final GuiOpenEvent guiOpenEvent) {
        if (shift.isToggled() && guiOpenEvent.gui == null) {
            this.isSneaking = mc.thePlayer.isSneaking();
        }
    }

    private void setSneakState(boolean shift) {
        if (this.isSneaking) {
            if (shift) {
                return;
            }
        }
        else if (!shift) {
            return;
        }
        if (shift) {
            final long targetShiftDelay = (long)shiftDelay.getInput();
            if (targetShiftDelay > 0L) {
                if (Utils.timeBetween(this.lastShift, System.currentTimeMillis()) < targetShiftDelay) {
                    return;
                }
                this.lastShift = System.currentTimeMillis();
            }
        }
        else {
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                return;
            }
            this.lastShift = System.currentTimeMillis();
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), this.isSneaking = shift);
    }

    public static boolean canSafeWalk() {
        if (ModuleManager.safeWalk != null && ModuleManager.safeWalk.isEnabled()) {
            if (disableOnForward.isToggled() && Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
                return false;
            }
            if (pitchCheck.isToggled() && mc.thePlayer.rotationPitch < 70) {
                return false;
            }
            if (blocksOnly.isToggled() && (mc.thePlayer.getHeldItem() == null || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock))) {
                return false;
            }
            if (ModuleManager.scaffold.moduleEnabled) {
                return false;
            }
            return true;
        }
        return false;
    }
}
