package keystrokesmod.module.impl.player;

import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Reflection;
import keystrokesmod.utility.Utils;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class FastPlace extends Module {
    public SliderSetting tickDelay;
    public ButtonSetting blocksOnly, pitchCheck;

    public FastPlace() {
        super("FastPlace", Module.category.player, 0);
        this.registerSetting(tickDelay = new SliderSetting("Tick delay", 1.0, 1.0, 3.0, 1.0));
        this.registerSetting(blocksOnly = new ButtonSetting("Blocks only", true));
        this.registerSetting(pitchCheck = new ButtonSetting("Pitch check", false));
        this.closetModule = true;
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent e) {
        if (e.phase == Phase.END) {
            if (ModuleManager.scaffold.stopFastPlace()) {
                return;
            }
            if (Utils.nullCheck() && mc.inGameHasFocus) {
                if (blocksOnly.isToggled()) {
                    ItemStack item = mc.thePlayer.getHeldItem();
                    if (item == null || !(item.getItem() instanceof ItemBlock)) {
                        return;
                    }
                }
                if (pitchCheck.isToggled() && mc.thePlayer.rotationPitch < 70.0f) {
                    return;
                }

                int c = (int) tickDelay.getInput();
                if (c == 0) {
                    ((IAccessorMinecraft) mc).setRightClickDelayTimer(0);
                } else {
                    if (c == 4) {
                        return;
                    }

                    int d = ((IAccessorMinecraft) mc).getRightClickDelayTimer();
                    if (d == 4) {
                        ((IAccessorMinecraft) mc).setRightClickDelayTimer(c);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck() || ModuleManager.scaffold.stopFastPlace()) {
            return;
        }
        if (pitchCheck.isToggled() && mc.thePlayer.rotationPitch < 70.0f) {
            return;
        }
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) e.getPacket();
            if (p.getPlacedBlockDirection() != 255 || p.getStack() == null || !(p.getStack().getItem() instanceof ItemBlock)) {
                return;
            }
            if (Math.random() < 0.7) {
                e.setCanceled(true);
            }
        }
    }
}
