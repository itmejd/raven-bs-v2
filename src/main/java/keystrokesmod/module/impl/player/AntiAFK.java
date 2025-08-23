package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AntiAFK extends Module {
    private SliderSetting afk;
    private ButtonSetting jump;
    private ButtonSetting jumpWhenCollided;
    private ButtonSetting randomClicks;
    private ButtonSetting swapItem;
    private SliderSetting spin;
    private ButtonSetting randomizeDelta;
    private ButtonSetting randomizePitch;
    private SliderSetting minDelay;
    private SliderSetting maxDelay;
    private String[] afkModes = new String[]{"None", "Wander", "Lateral shuffle", "Forward", "Backward", "Lobby"};
    private String[] spinModes = new String[]{"None", "Random", "Right", "Left"};
    private int ticks, afkTicks;
    private boolean c;
    public boolean stop = false;
    private boolean stopFlying;
    private int sfTicks, randomDelay;
    public AntiAFK() {
        super("AntiAFK", category.player);
        this.registerSetting(afk = new SliderSetting("AFK", 0, afkModes));
        this.registerSetting(jump = new ButtonSetting("Jump", false));
        this.registerSetting(jumpWhenCollided = new ButtonSetting("Jump only when collided", false));
        this.registerSetting(randomClicks = new ButtonSetting("Random clicks", false));
        this.registerSetting(swapItem = new ButtonSetting("Swap item", false));
        this.registerSetting(spin = new SliderSetting("Spin", 0, spinModes));
        this.registerSetting(randomizeDelta = new ButtonSetting("Randomize delta", true));
        this.registerSetting(randomizePitch = new ButtonSetting("Randomize pitch", true));
        this.registerSetting(minDelay = new SliderSetting("Minimum delay ticks", 10.0, 4.0, 160.0, 2.0));
        this.registerSetting(maxDelay = new SliderSetting("Maximum delay ticks", 80.0, 4.0, 160.0, 2.0));
    }

    public void onEnable() {
        this.ticks = this.h();
        this.c = Utils.getRandom().nextBoolean();
    }

    public void onUpdate() {
        if (stop) {
            return;
        }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiChat)) {
            return;
        }
        --this.ticks;
        switch ((int) afk.getInput()) {
            case 1: {
                if (this.c) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Utils.getRandom().nextBoolean());
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), Utils.getRandom().nextBoolean());
                    break;
                }
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), Utils.getRandom().nextBoolean());
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), Utils.getRandom().nextBoolean());
                break;
            }
            case 2: {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), this.c);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), !this.c);
                break;
            }
            case 3: {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                break;
            }
            case 4: {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
                break;
            }
            case 5: {
                if (Utils.isMoving() || Utils.jumpDown()) {
                    if (sfTicks > 0 && !Utils.jumpDown()) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                    }
                    afkTicks = sfTicks = 0;
                }
                else {
                    ++afkTicks;
                }
                if (afkTicks >= 1000) {
                    if (mc.thePlayer.capabilities.isFlying) {
                        stopFlying = true;
                    }
                    else if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                    afkTicks = 0;
                }
                if (stopFlying && ++sfTicks > -1) {
                    if (sfTicks == 1) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                    }
                    else if (sfTicks == 2) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                    }
                    else if (sfTicks == 4) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                    }
                    else if (sfTicks == 5) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                        sfTicks = 0;
                        stopFlying = false;
                    }
                }
                break;
            }
        }
        switch ((int) spin.getInput()) {
            case 1: {
                mc.thePlayer.rotationYaw += this.c(this.c);
                this.d();
                break;
            }
            case 2: {
                mc.thePlayer.rotationYaw += this.c(true);
                this.d();
                break;
            }
            case 3: {
                mc.thePlayer.rotationYaw += this.c(false);
                this.d();
                break;
            }
        }
        if (jump.isToggled() && mc.thePlayer.onGround && (!jumpWhenCollided.isToggled() || mc.thePlayer.isCollidedHorizontally)) {
            mc.thePlayer.jump();
        }
        if (this.ticks == 0) {
            if (swapItem.isToggled()) {
                mc.thePlayer.inventory.currentItem = Utils.randomizeInt(0, 8);
            }
            if (randomClicks.isToggled()) {
                ((IAccessorMinecraft) mc).callClickMouse();
            }
            this.ticks = this.h();
            this.c = !this.c;
        }
    }

    private double a() {
        final int n = Utils.getRandom().nextBoolean() ? 1 : -1;
        if (!randomizeDelta.isToggled()) {
            return 2 * n;
        }
        double n2 = Utils.randomizeInt(100, 500) / 100.0;
        if (n2 % 1.0 == 0.0) {
            n2 += Utils.randomizeInt(1, 10) / 10.0 * n;
        }
        return n2 * n;
    }

    public void onDisable() {
        this.b(0);
        stop = false;
    }

    private void b(final int n) {
        switch (n) {
            case 1: {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                break;
            }
            case 2: {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                break;
            }
            case 3: {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
                break;
            }
            case 4: {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
                break;
            }
        }
    }

    private int h() {
        if (minDelay.getInput() == maxDelay.getInput()) {
            return (int)minDelay.getInput();
        }
        return Utils.randomizeInt((int)minDelay.getInput(), (int) maxDelay.getInput());
    }

    private void d() {
        if (randomizePitch.isToggled()) {
            mc.thePlayer.rotationPitch = RotationUtils.clampPitch((float)(mc.thePlayer.rotationPitch + this.a()));
        }
    }

    private double c(final boolean b) {
        final int n = b ? 1 : -1;
        if (!randomizeDelta.isToggled()) {
            return 3 * n;
        }
        double n2 = Utils.randomizeInt(100, 1000) / 100.0;
        if (n2 % 1.0 == 0.0) {
            n2 += Utils.randomizeInt(1, 10) / 10.0 * n;
        }
        return n2 * n;
    }
}
