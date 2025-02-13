package keystrokesmod.module.impl.player;

import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.Objects;

public class Disabler extends Module {

    private final int defaultSetbacks = 20;
    private final long joinDelay = 200, delay = 0, checkDisabledTime = 4000, timeout = 12000;
    private final double min_offset = 0.2;

    private long joinTime, lobbyTime, finished;
    private boolean awaitJoin, joinTick, awaitSetback, hideProgress, awaitJump, awaitGround;
    private int setbackCount, airTicks, disablerAirTicks;
    private double minSetbacks, zOffset;
    private float savedYaw, savedPitch;

    private int color = new Color(0, 187, 255, 255).getRGB();

    private String text;
    private int[] disp;
    private int width;

    public Disabler() {
        super("Disabler", Module.category.player);
    }

    private void resetVars() {
        awaitJoin = joinTick = awaitSetback = awaitJump = false;
        minSetbacks = zOffset = lobbyTime = finished = setbackCount = 0;
    }

    public void onDisable() {
        resetVars();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        long now = System.currentTimeMillis();

        if (!awaitGround && !mc.thePlayer.onGround) {
            disablerAirTicks++;
        } else {
            awaitGround = false;
            disablerAirTicks = 0;
        }

        if (awaitJoin && now >= joinTime + joinDelay) {
            if (Utils.getBedwarsStatus() == 1 || Utils.isBedwarsPractice() || Utils.getSkyWarsStatus() == 1) {
                awaitJoin = false;
                joinTick = true;
            }
        }

        if (awaitSetback) {
            color = Theme.getGradient((int) HUD.theme.getInput(), 0);
            text = "§7running disabler " + "§r" + Utils.round((now - lobbyTime) / 1000d, 1) + "s " + ((int) Utils.round(100 * (setbackCount / minSetbacks), 0)) + "%";
            width = mc.fontRendererObj.getStringWidth(text) / 2 - 2;
        } else {
            text = null;
        }

        if (finished != 0 && mc.thePlayer.onGround && now - finished > checkDisabledTime) {
            Utils.print("&7[&dR&7] &adisabler enabled");
            finished = 0;
        }

        if (awaitJump && disablerAirTicks == 5) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
            awaitJump = false;

            minSetbacks = defaultSetbacks;
            savedYaw = e.getYaw(); // pitch will be 0
            lobbyTime = now;
            awaitSetback = true;
        }

        if (joinTick) {
            joinTick = false;
            Utils.print("&7[&dR&7] running disabler...");
            if (mc.thePlayer.onGround || (mc.thePlayer.fallDistance < 0.3 && !Utils.isBedwarsPractice())) {
                awaitJump = true;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
            } else {
                minSetbacks = defaultSetbacks;
                savedYaw = e.getYaw(); // pitch will be 0
                lobbyTime = now;
                awaitSetback = true;
            }
            return;
        }

        if (awaitSetback) {
            if (setbackCount >= minSetbacks) {
                Utils.print("&7[&dR&7] &afinished in &b" + Utils.round((now - lobbyTime) / 1000d, 1) + "&as, wait a few seconds...");
                resetVars();
                finished = now;
                return;
            } else if (lobbyTime != 0 && now - lobbyTime > timeout) {
                Utils.print("&7[&dR&7] &cdisabler failed");
                resetVars();
                return;
            }
            if (now - lobbyTime > delay) {
                e.setYaw(savedYaw);
                e.setPitch(savedPitch);
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionZ = 0;

                if (mc.thePlayer.ticksExisted % 2 == 0) {
                    //e.setPosX(mc.thePlayer.posX + 0.11);
                }

                if (Utils.getSkyWarsStatus() == 1) {
                    zOffset = min_offset * 0.7;
                    if (mc.thePlayer.ticksExisted % 2 == 0) {
                        zOffset *= -1;
                    }
                    e.setPosZ(e.getPosZ() + zOffset);
                } else {
                    e.setPosZ(e.getPosZ() + (zOffset += min_offset));
                }
            }
        }

    }

    @SubscribeEvent()
    public void onMoveInput(PrePlayerInputEvent e) {
        if (awaitSetback) {
            e.setForward(0);
            e.setStrafe(0);
            mc.thePlayer.movementInput.jump = false;
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S08PacketPlayerPosLook) {
            setbackCount++;
            zOffset = 0;
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null || !awaitSetback || text == null) {
                return;
            }
        }
        float widthOffset = 0;
        color = Theme.getGradient((int) HUD.theme.getInput(), 0);
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        int[] display = {scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset, display[1] / 2 + 8, color, true);
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            long joinTime = System.currentTimeMillis();
            if (awaitSetback) {
                Utils.print("&7[&dR&7] &cdisabing disabler");
            }
            resetVars();
            awaitJoin = awaitGround = true;
        }
    }
}