package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceiveAllPacketsEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AntiVoid extends Module {
    private static SliderSetting distance;
    private ButtonSetting renderTimer, disableLJ, disablePractice;
    public ConcurrentLinkedQueue<Packet> blinkedPackets = new ConcurrentLinkedQueue<>();
    private int color = new Color(0, 187, 255, 255).getRGB();
    public int blinkTicks;
    public boolean started, wait;
    public double y;
    public AntiVoid() {
        super("AntiVoid", category.player);
        this.registerSetting(distance = new SliderSetting("Distance", "", 5, 1, 10, 0.5));
        this.registerSetting(renderTimer = new ButtonSetting("Render Timer", false));
        this.registerSetting(disableLJ = new ButtonSetting("Disable with Long Jump", false));
        this.registerSetting(disablePractice = new ButtonSetting("Disable in Practice", false));
    }

    @Override
    public void onEnable() {
        blinkedPackets.clear();
    }

    public void onDisable() {
        release();
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        Packet packet = e.getPacket();
        if (!started && (!Utils.overVoid() || mc.thePlayer.onGround)) {
            y = mc.thePlayer.posY;
            wait = false;
            return;
        }
        if (!started && (Utils.isReplay() || Utils.spectatorCheck() || Utils.isBedwarsPractice() && disablePractice.isToggled())) {
            return;
        }
        if (mc.thePlayer.ticksExisted <= 10) {
            return;
        }
        if (packet.getClass().getSimpleName().startsWith("S")) {
            return;
        }
        if (ModuleManager.noFall.isBlinking) {
            return;
        }
        if (wait) {
            return;
        }
        if (packet instanceof C00PacketLoginStart || packet instanceof C00Handshake) {
            return;
        }
        if (!e.isCanceled()) {
            started = true;

            if (ModuleManager.blink.isEnabled()) {
                return;
            }

            blinkedPackets.add(packet);
            KillAura.blinkOn = true;
            e.setCanceled(true);
        }
    }

    @Override
    public String getInfo() {
        return blinkTicks + "";
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {

        if (!Utils.overVoid() || mc.thePlayer.onGround) {
            release();
        }
        if (dist() && Utils.overVoid() || disableLJ.isToggled() && LongJump.function) {
            release();
            wait = true;
        }
        else if (started) {
            ++blinkTicks;

            if (ModuleManager.blink.isEnabled()) {
                return;
            }

            if (mc.thePlayer.posY <= y - distance.getInput()) {
                posPacket();
                release();
                wait = true;
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck() || !renderTimer.isToggled() || blinkTicks == 0 || blinkTicks >= 99999 || ModuleManager.blink.isEnabled()) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                return;
            }
        }
        Utils.handleTimer(color, blinkTicks);
    }

    public void posPacket() {
        PacketUtils.sendPacketNoEvent(new C03PacketPlayer.C06PacketPlayerPosLook(mc.thePlayer.posX, -0.55, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround));
    }

    public void release() {
        if (!ModuleManager.blink.isEnabled()) {
            synchronized (blinkedPackets) {
                for (Packet packet : blinkedPackets) {
                    Raven.packetsHandler.handlePacket(packet);
                    PacketUtils.sendPacketNoEvent(packet);
                }
            }
        }
        blinkedPackets.clear();
        blinkTicks = 0;
        started = false;
        if (!ModuleManager.blink.isEnabled()) {
            KillAura.blinkOn = KillAura.blinkChecked = false;
        }
    }

    public boolean dist() {
        double minMotion = 0.1;
        int dist1 = 4;
        int dist2 = 6;
        int dist3 = 7;
        int dist4 = 9;
        // 1x1

        if (mc.thePlayer.isCollidedHorizontally) {
            return false;
        }

        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ) > dist1) {
            return true;
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ) > dist1) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ) > dist1) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ - 1) > dist1) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ + 1) > dist1) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ - 1) > dist1) {
            if (mc.thePlayer.motionX <= -minMotion && mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ + 1) > dist1) {
            if (mc.thePlayer.motionX >= minMotion && mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ + 1) > dist1) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ - 1) > dist1) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }

        // 2x2

        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ) > dist2) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ) > dist2) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ - 2) > dist2) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ + 2) > dist2) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ - 1) > dist2) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ - 2) > dist2) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ + 1) > dist2) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ + 2) > dist2) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ - 2) > dist2) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ - 2) > dist2) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ + 2) > dist2) {
            if (mc.thePlayer.motionX >= minMotion && mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ - 2) > dist2) {
            if (mc.thePlayer.motionX <= -minMotion && mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }

        // 3x3

        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionX >= minMotion && mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion && mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ + 1) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ + 2) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ - 1) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ - 2) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ - 1) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ - 2) > dist3) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ - 3) > dist3) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ + 1) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ + 2) > dist3) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ + 3) > dist3) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }

        // 4x4

        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionX >= minMotion && mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion && mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ + 3) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ + 2) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ + 1) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 3, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 2, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 1, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ - 3) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ - 2) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX + 4, (int) mc.thePlayer.posZ - 1) > dist4) {
            if (mc.thePlayer.motionX >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ + 4) > dist4) {
            if (mc.thePlayer.motionZ >= minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ + 3) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ + 2) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ + 1) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ - 3) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ - 2) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 4, (int) mc.thePlayer.posZ - 1) > dist4) {
            if (mc.thePlayer.motionX <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 3, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 2, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }
        if (Utils.distanceToGround(mc.thePlayer, (int) mc.thePlayer.posX - 1, (int) mc.thePlayer.posZ - 4) > dist4) {
            if (mc.thePlayer.motionZ <= -minMotion) {
                return true;
            }
        }





        return false;
    }

}