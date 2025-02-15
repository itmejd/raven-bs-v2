package keystrokesmod.module.impl.movement;

import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.event.PrePlayerInputEvent;
import net.minecraft.client.gui.GuiChat;
import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ConcurrentLinkedQueue;

public class InvMove extends Module {
    private SliderSetting modes;


    private int ticks;
    private boolean stopMoving;
    private String[] modesString = new String[] { "Vanilla", "Stop movement" };

    public InvMove() {
        super("InvMove", Module.category.movement);
        this.registerSetting(modes = new SliderSetting("Modes", 1, modesString));
    }

    public void onDisable() {
        reset();
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C0EPacketClickWindow) {
            if (modes.getInput() == 1) {
                stopMoving = true;
                ticks = 0;
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!guiCheck()) {
            reset();
            return;
        }
        allowBinds(true);
        if (modes.getInput() == 1) {
            if (!(mc.currentScreen instanceof ClickGui)) {
                if (stopMoving) {
                    ticks++;
                    allowBinds(false);
                    if (ticks >= 9) {
                        ticks = 0;
                        stopMoving = false;
                    }
                }
            } else {
                reset();
            }
        }

        boolean foodLvlMet = (float)mc.thePlayer.getFoodStats().getFoodLevel() > 6.0F || mc.thePlayer.capabilities.allowFlying; // from mc
        if (((Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()) || ModuleManager.sprint.isEnabled()) && mc.thePlayer.movementInput.moveForward >= 0.8F && foodLvlMet && !mc.thePlayer.isSprinting())) {
            mc.thePlayer.setSprinting(true);
        }
        if (Keyboard.isKeyDown(208) && mc.thePlayer.rotationPitch < 90.0F) {
            mc.thePlayer.rotationPitch += 6.0F;
        }
        if (Keyboard.isKeyDown(200) && mc.thePlayer.rotationPitch > -90.0F) {
            mc.thePlayer.rotationPitch -= 6.0F;
        }
        if (Keyboard.isKeyDown(205)) {
            mc.thePlayer.rotationYaw += 6.0F;
        }
        if (Keyboard.isKeyDown(203)) {
            mc.thePlayer.rotationYaw -= 6.0F;
        }
    }

    private void reset() {
        ticks = 0;
        stopMoving = false;
    }

    private boolean guiCheck() {
        //Utils.sendModuleMessage(this, "&7screen " + mc.currentScreen);
        if (mc.currentScreen == null) {
            return false;
        }
        if (mc.currentScreen instanceof ClickGui) {
            return true;
        }
        if (mc.currentScreen instanceof GuiChat) {
            return false;
        }
        return true;
    }

    private void allowBinds(boolean allowKeys) {
        if (allowKeys) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), Utils.jumpDown());
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(),false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(),false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(),false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(),false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(),false);
        }
    }

}
