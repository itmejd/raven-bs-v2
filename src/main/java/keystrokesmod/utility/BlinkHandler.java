package keystrokesmod.utility;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.movement.NoSlow;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

public class BlinkHandler {
    private static Minecraft mc = null;

    public BlinkHandler(Minecraft mc) {
        this.mc = mc;
    }

    private static ConcurrentLinkedQueue<Packet> blinkedPackets = new ConcurrentLinkedQueue<>();
    static boolean active;
    public static int blinkTicks;
    private int color = new Color(0, 187, 255, 255).getRGB();
    private static Vec3 bPos;
    public static boolean released;

    public static int a1;
    public static boolean blinkModule() {
        a1 = 0;
        if (ModuleManager.antiVoid != null && ModuleManager.antiVoid.isEnabled() && ModuleManager.antiVoid.blink) {
            a1++;
        }
        if (ModuleManager.blink != null && ModuleManager.blink.isEnabled() && ModuleManager.blink.blink) {
            a1++;
        }
        if (ModuleManager.noFall != null && ModuleManager.noFall.isEnabled() && ModuleManager.noFall.mode.getInput() == 5 && ModuleManager.noFall.blink) {
            a1++;
        }
        if (ModuleManager.noSlow != null && ModuleManager.noSlow.isEnabled() && NoSlow.mode.getInput() == 5 && ModuleManager.noSlow.blink) {
            a1++;
        }
        if (ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && ModuleManager.killAura.blink) {
            a1++;
        }
        if (ModuleManager.scaffold != null && ModuleManager.scaffold.isEnabled && ModuleManager.scaffold.blink) {
            a1++;
        }
        if (ModuleManager.lagRange != null && ModuleManager.lagRange.isEnabled() && ModuleManager.lagRange.blink) {
            a1++;
        }
        return a1 > 0;
    }

    public static boolean renderTimer() {
        if (ModuleManager.antiVoid != null && ModuleManager.antiVoid.isEnabled() && ModuleManager.antiVoid.blink && ModuleManager.antiVoid.renderTimer.isToggled()) {
            return true;
        }
        if (ModuleManager.blink != null && ModuleManager.blink.isEnabled() && ModuleManager.blink.blink && ModuleManager.blink.renderTimer.isToggled()) {
            return true;
        }
        if (ModuleManager.noFall != null && ModuleManager.noFall.isEnabled() && ModuleManager.noFall.blink && ModuleManager.noFall.mode.getInput() == 5 && ModuleManager.noFall.renderTimer.isToggled()) {
            return true;
        }
        if (ModuleManager.noSlow != null && ModuleManager.noSlow.isEnabled() && ModuleManager.noSlow.blink && NoSlow.mode.getInput() == 5 && ModuleManager.noSlow.renderTimer.isToggled()) {
            return true;
        }
        return false;
    }

    private boolean renderBox() {
        if (ModuleManager.blink != null && ModuleManager.blink.isEnabled() && ModuleManager.blink.blink && ModuleManager.blink.initialPosition.isToggled()) {
            return true;
        }
        if (ModuleManager.lagRange != null && ModuleManager.lagRange.isEnabled() && ModuleManager.lagRange.blink && (ModuleManager.lagRange.initialPosition.isToggled() && ModuleManager.lagRange.function || ModuleManager.lagRange.initialPosition2.isToggled() && (ModuleManager.lagRange.functionJumpPot || ModuleManager.lagRange.functionArrows))) {
            return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreUpdate(PreUpdateEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!renderBox()) {
            bPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        }

        if (active) {
            blinkTicks++;
        }

        if (!active || blinkModule()) {
            return;
        }
        releaseAll();
    }

    public static void releaseAll() {
        if (blinkedPackets.isEmpty()) {
            return;
        }
        if (ModuleManager.antiVoid.setPos) {
            PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, -0.55, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround));
            ModuleManager.antiVoid.setPos = false;
        }
        synchronized (blinkedPackets) {
            for (Packet packet : blinkedPackets) {
                Raven.packetsHandler.handlePacket(packet);
                PacketUtils.sendPacketNoEvent(packet);
            }
        }
        blinkedPackets.clear();
        blinkTicks = 0;
        released = true;
        bPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        if (!blinkModule()) {
            active = false;
        }
    }

    public static boolean exceptions() {
        if (blinkModule() && a1 == 2 && (ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && ModuleManager.killAura.blink && ModuleManager.lagRange != null && ModuleManager.lagRange.isEnabled() && ModuleManager.lagRange.blink)) {
            return true;
        }
        return false;
    }

    public static void release() {
        if (blinkModule() && a1 > 1 && !exceptions()) {
            return;
        }
        releaseAll();
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        released = false;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck() || mc.isSingleplayer()) {
            return;
        }
        Packet p = e.getPacket();
        if (blinkModule()) {
            active = true;
        }
        if (active) {
            if (p instanceof C00PacketLoginStart || p instanceof FMLProxyPacket || p instanceof C15PacketClientSettings || p instanceof C17PacketCustomPayload || p instanceof C00Handshake || p instanceof C01PacketPing || p instanceof C00PacketServerQuery || p instanceof C00PacketKeepAlive) {
                return;
            }
            if (!(p instanceof C03PacketPlayer || p instanceof C01PacketChatMessage || p instanceof C13PacketPlayerAbilities || p instanceof C0EPacketClickWindow || p instanceof C0DPacketCloseWindow || p instanceof C0FPacketConfirmTransaction || p instanceof C0BPacketEntityAction || p instanceof C08PacketPlayerBlockPlacement || p instanceof C09PacketHeldItemChange || p instanceof C07PacketPlayerDigging || p instanceof C02PacketUseEntity || p instanceof C0APacketAnimation)) {
                Utils.print(e.getPacket());
            }
            blinkedPackets.add(e.getPacket());
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck() || blinkTicks == 0 || !renderTimer() || !blinkModule()) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                return;
            }
        }
        ticksTimer(blinkTicks);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || bPos == null || !renderBox() || !blinkModule() || mc.gameSettings.thirdPersonView == 0) {
            return;
        }
        drawBox(bPos);
    }

    private void ticksTimer(int ticks) {
        color = Theme.getGradient((int) HUD.theme.getInput(), 0);
        int widthOffset = (ticks < 10) ? 4 : (ticks >= 10 && ticks < 100) ? 7 : (ticks >= 100 && ticks < 1000) ? 10 : (ticks >= 1000) ? 13 : 16;
        String text = ("" + ticks);
        int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        int[] display = {scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset, display[1] / 2 + 8, color, true);
    }

    private void secondsTimer(int ticks) {
        color = Theme.getGradient((int) HUD.theme.getInput(), 0);
        ticks = ticks / 20;
        int widthOffset = (ticks < 10) ? 4 : (ticks >= 10 && ticks < 100) ? 7 : (ticks >= 100 && ticks < 1000) ? 10 : (ticks >= 1000) ? 13 : 16;
        String text = (ticks + "s");
        int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        int[] display = {scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset, display[1] / 2 + 8, color, true);
    }

    private void drawBox(Vec3 pos) {
        GlStateManager.pushMatrix();
        color = Theme.getGradient((int) HUD.theme.getInput(), 0);
        double x = pos.xCoord - mc.getRenderManager().viewerPosX;
        double y = pos.yCoord - mc.getRenderManager().viewerPosY;
        double z = pos.zCoord - mc.getRenderManager().viewerPosZ;
        AxisAlignedBB bbox = mc.thePlayer.getEntityBoundingBox().expand(0.1D, 0.1, 0.1);
        AxisAlignedBB axis = new AxisAlignedBB(bbox.minX - mc.thePlayer.posX + x, bbox.minY - mc.thePlayer.posY + y, bbox.minZ - mc.thePlayer.posZ + z, bbox.maxX - mc.thePlayer.posX + x, bbox.maxY - mc.thePlayer.posY + y, bbox.maxZ - mc.thePlayer.posZ + z);
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.0F);
        GL11.glColor4f(r, g, b, a);
        RenderUtils.drawBoundingBox(axis, r, g, b);
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
        GlStateManager.popMatrix();
    }
}