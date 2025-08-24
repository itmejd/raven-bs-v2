package keystrokesmod.module.impl.player;

import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.*;
import java.util.List;

public class VelocityBuffer extends Module {
    private ButtonSetting renderTimer;
    private int color = new Color(209, 1, 1, 255).getRGB();

    private List<Map<String, Object>> packets = new ArrayList<>();
    public boolean delaying = false;
    private int timeout = 0;


    public VelocityBuffer() {
        super("VelocityBuffer", category.player);
        this.registerSetting(renderTimer = new ButtonSetting("Render Timer", false));
    }

    @Override
    public void onDisable() {
        flush();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
                S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity) e.getPacket();

                if (s12PacketEntityVelocity.getEntityID() == mc.thePlayer.getEntityId()) {
                    delaying = true;
                }
            }
        }


        Packet p = e.getPacket();
        if (!delaying || !(p instanceof S12PacketEntityVelocity || p instanceof S32PacketConfirmTransaction)) {
            return;
        }
        Map<String, Object> entry = new HashMap<>();
        entry.put("packet", e.getPacket());
        entry.put("time", Utils.time());
        synchronized (packets) {
            packets.add(entry);
        }
        e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPostMotion(PostMotionEvent e) {
        if (delaying && ++timeout >= 1000) {
            flush();
            Utils.modulePrint("&cVelocityBuffer timed out.");
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck() || timeout == 0 || !renderTimer.isToggled()) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                return;
            }
        }
        ticksTimer(timeout);
    }

    private void ticksTimer(int ticks) {
        int widthOffset = (ticks < 10) ? 4 : (ticks >= 10 && ticks < 100) ? 7 : (ticks >= 100 && ticks < 1000) ? 10 : (ticks >= 1000) ? 13 : 16;
        String text = ("" + ticks);
        int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        int[] display = {scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        float yadd = 8;
        if (BlinkHandler.blinkModule() && BlinkHandler.renderTimer() && BlinkHandler.blinkTicks != 0) {
            yadd = 18;
        }
        mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset, display[1] / 2 + yadd, color, true);
    }

    void flush() {
        while (!packets.isEmpty()) {
            synchronized (packets) {
                Map<String, Object> entry = packets.remove(0);
                PacketUtils.receivePacketNoEvent((Packet) entry.get("packet"));
            }
        }
        delaying = false;
        timeout = 0;
    }
}