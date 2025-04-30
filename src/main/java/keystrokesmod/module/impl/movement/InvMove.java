package keystrokesmod.module.impl.movement;

import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.event.*;
import keystrokesmod.utility.ModuleUtils;
import net.minecraft.client.gui.GuiChat;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ConcurrentLinkedQueue;

public class InvMove extends Module {
    private SliderSetting modes;


    private int ticks;
    private boolean binds, stopMoving;
    private String[] modesString = new String[] { "Vanilla", "Stop movement", "Motion", "Only menus" };

    public InvMove() {
        super("InvMove", Module.category.movement);
        this.registerSetting(modes = new SliderSetting("Modes", 1, modesString));
    }

    public void onDisable() {
        reset();
    }

    @SubscribeEvent
    public void onSendPacketAll(SendAllPacketsEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.getPacket() instanceof C0EPacketClickWindow) {
            stopMoving = true;
            ticks = 0;
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!guiCheck()) {
            reset();
            return;
        }

        if (stopMoving) {
            ticks++;
            if (ticks >= 10) {
                ticks = 0;
                stopMoving = false;
            }
        }

        if (modes.getInput() == 3 && !nonInteractGUIs()) {
            allowBinds(false);
            return;
        }
        allowBinds(true);

        if (modes.getInput() == 1) {
            if (!nonInteractGUIs()) {
                if (stopMoving) {
                    allowBinds(false);
                }
            } else {
                reset();
            }
        }
        if (modes.getInput() == 2 && !nonInteractGUIs()) {
            if (!mc.thePlayer.onGround) {
                motionSet(0.56, 0);
            }
            else {
                if (mc.thePlayer.isSprinting()) {
                    if (getSpeedLevel() == 0) {
                        motionSet(0.72, 0.03);
                    }
                    else {
                        motionSet(0.64, 0.03);
                    }
                } else {
                    motionSet(0.97, 0.03);
                }
            }
        }

        boolean foodLvlMet = (float)mc.thePlayer.getFoodStats().getFoodLevel() > 6.0F || mc.thePlayer.capabilities.allowFlying; // from mc
        if (((Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()) || ModuleManager.sprint.isEnabled()) && mc.thePlayer.movementInput.moveForward >= 0.8F && foodLvlMet && !mc.thePlayer.isSprinting())) {
            mc.thePlayer.setSprinting(true);
        }
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!guiCheck()) {
            return;
        }
        if (nonInteractGUIs()) {
            return;
        }
        if (modes.getInput() != 2) {
            return;
        }
        mc.thePlayer.movementInput.jump = false;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!guiCheck()) {
            return;
        }
        if (Keyboard.isKeyDown(208) && mc.thePlayer.rotationPitch < 90.0F) {
            mc.thePlayer.rotationPitch += 1.0F;
        }
        if (Keyboard.isKeyDown(200) && mc.thePlayer.rotationPitch > -90.0F) {
            mc.thePlayer.rotationPitch -= 1.0F;
        }
        if (Keyboard.isKeyDown(205)) {
            mc.thePlayer.rotationYaw += 1.0F;
        }
        if (Keyboard.isKeyDown(203)) {
            mc.thePlayer.rotationYaw -= 1.0F;
        }
    }

    private void reset() {
        ticks = 0;
        stopMoving = false;
        if (!binds) {
            allowBinds(true);
        }
    }

    public boolean active() {
        return ModuleManager.invmove != null && ModuleManager.invmove.isEnabled() && guiCheck() && !nonInteractGUIs() && modes.getInput() == 2;
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

    private boolean guiCheck() {
        if (mc.currentScreen == null) {
            return false;
        }
        if (mc.currentScreen instanceof GuiChat) {
            return false;
        }
        return true;
    }

    private boolean nonInteractGUIs() {
        if (mc.currentScreen instanceof ClickGui) {
            return true;
        }
        if (mc.currentScreen instanceof GuiIngameMenu) {
            return true;
        }
        return false;
    }

    private void motionSet(double val, double strafe) {
        mc.thePlayer.motionX *= (mc.thePlayer.moveStrafing == 0 ? val : val - strafe);
        mc.thePlayer.motionZ *= (mc.thePlayer.moveStrafing == 0 ? val : val - strafe);
    }

    private void allowBinds(boolean allowKeys) {
        if (allowKeys) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), Utils.jumpDown());
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
            binds = true;
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(),false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(),false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(),false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(),false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(),false);
            binds = false;
        }
    }

}