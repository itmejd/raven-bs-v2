package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceiveAllPacketsEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.ModuleUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.*;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import static keystrokesmod.module.impl.render.HUD.theme;

public class Blink extends Module {
    private static SliderSetting maximumBlinkTicks;
    private ButtonSetting initialPosition;
    private ButtonSetting renderTimer;
    private ButtonSetting disableOnBreak, disableOnAttack;
    private ConcurrentLinkedQueue<Packet> blinkedPackets = new ConcurrentLinkedQueue<>();
    private Vec3 pos;
    //final private int color = Theme.getGradient((int) theme.getInput(), 255);
    private int color = new Color(0, 187, 255, 255).getRGB();
    private int blinkTicks;
    public boolean started;

    public Blink() {
        super("Blink", category.player);
        this.registerSetting(maximumBlinkTicks = new SliderSetting("Max Blink Ticks", "", 0, 0, 40, 1));
        this.registerSetting(new DescriptionSetting("0 = no max"));
        this.registerSetting(initialPosition = new ButtonSetting("Show initial position", true));
        this.registerSetting(renderTimer = new ButtonSetting("Render Timer", false));
        this.registerSetting(disableOnBreak = new ButtonSetting("Disable on Break", false));
        this.registerSetting(disableOnAttack = new ButtonSetting("Disable on Attack", false));
    }

    @Override
    public void onEnable() {
        blinkedPackets.clear();
        pos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }

    public void onDisable() {
        synchronized (blinkedPackets) {
            for (Packet packet : blinkedPackets) {
                Raven.packetsHandler.handlePacket(packet);
                PacketUtils.sendPacketNoEvent(packet);
            }
        }
        blinkedPackets.clear();
        pos = null;
        blinkTicks = 0;
        started = false;
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            this.disable();
            return;
        }
        if (ModuleManager.antiVoid.started) {
            disable();
            return;
        }
        if (ModuleManager.killAura.isTargeting || ModuleManager.killAura.justUnTargeted) {
            return;
        }
        if (disableOnBreak.isToggled() && (Utils.usingBedAura() || ModuleUtils.isBreaking)) {
            disable();
            return;
        }
        Packet packet = e.getPacket();
        if (packet.getClass().getSimpleName().startsWith("S")) {
            return;
        }
        if (packet instanceof C00PacketLoginStart || packet instanceof C00Handshake) {
            return;
        }
        if (disableOnAttack.isToggled() && packet instanceof C02PacketUseEntity || blinkTicks >= 99999) {
            blinkTicks = 99999;
            return;
        }
        if (!e.isCanceled()) {
            started = true;
            blinkedPackets.add(packet);
            e.setCanceled(true);
        }
    }

    @Override
    public String getInfo() {
        return blinkTicks + "";
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (started) ++blinkTicks;

        if (maximumBlinkTicks.getInput() != 0) {
            if (blinkTicks >= maximumBlinkTicks.getInput()) {
                disable();
            }
        }
        if (blinkTicks >= 99999) {
            disable();
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck() || !renderTimer.isToggled() || blinkTicks == 0 || blinkTicks >= 99999) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                return;
            }
        }
        color = Theme.getGradient((int) HUD.theme.getInput(), 0);
        int widthOffset = (blinkTicks < 10) ? 4 : (blinkTicks >= 10 && blinkTicks < 100) ? 7 : (blinkTicks >= 100 && blinkTicks < 1000) ? 10 : (blinkTicks >= 1000) ? 13 : 16;
        String text = ("" + blinkTicks);
        int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        int[] display = {scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset, display[1] / 2 + 8, color, true);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || pos == null || !initialPosition.isToggled()) {
            return;
        }
        drawBox(pos);
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

