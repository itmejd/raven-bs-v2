package keystrokesmod.module.impl.player;

import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
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
    private SliderSetting disablerTicks;
    private SliderSetting activationDelay;

    int tickCounter = 0;
    boolean waitingForGround = false;
    int airTicks = 0;
    boolean applyingMotion = false;
    int stateTickCounter = 0;
    boolean warningDisplayed = false;
    int sprintToggleTick = 0;
    boolean shouldRun = false;
    long lobbyTime = 0;
    long finished = 0;
    long activationDelayMillis;
    final long checkDisabledTime = 4000;
    private int color = new Color(0, 187, 255, 255).getRGB();
    private float barWidth = 60;
    private float barHeight = 4;
    private float filledWidth;
    private float barX;
    private float barY;
    private boolean shouldRender;
    private double firstY;
    private boolean reset;
    private float savedYaw, savedPitch;
    private boolean worldJoin;

    public boolean disablerLoaded, running;

    public Disabler() {
        super("Disabler", Module.category.player);
        this.registerSetting(disablerTicks = new SliderSetting("Ticks", "", 150, 110, 150, 5));
        this.registerSetting(activationDelay = new SliderSetting("Activation delay", " seconds", 0, 0, 4, 0.5));
    }

    public void onEnable() {
        if (!disablerLoaded) {
            resetState();
        }
    }

    public void onDisable() {
        shouldRun = false;
        running = false;
    }

    private void resetState() {
        shouldRun = true;
        tickCounter = 0;
        airTicks = 0;
        applyingMotion = false;
        waitingForGround = true;
        stateTickCounter = 0;
        warningDisplayed = false;
        running = false;
        sprintToggleTick = 0;
        lobbyTime = Utils.time();
        finished = 0;
        shouldRender = false;
        reset = false;
        worldJoin = false;
        activationDelayMillis = (long)(activationDelay.getInput() * 1000);
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            resetState();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        if (Utils.getLobbyStatus() == 1 || Utils.hypixelStatus() != 1) {
            return;
        }
        long now = System.currentTimeMillis();
        if (finished != 0 && mc.thePlayer.onGround && now - finished > checkDisabledTime) {
            Utils.print("&7[&dR&7] &adisabler enabled");
            finished = 0;
            filledWidth = 0;
            disablerLoaded = true;
        }
        if (!shouldRun) {
            return;
        }

        if ((now - lobbyTime) < activationDelayMillis) {
            return;
        }

        if (waitingForGround) {
            savedYaw = e.getYaw();
            savedPitch = e.getPitch();
            if (!running) {
                //mc.thePlayer.motionY = 0;
            }
            running = true;
            /*if (mc.thePlayer.ticksExisted <= 3) {
                mc.thePlayer.motionY = 0.42f;
                waitingForGround = false;
                worldJoin = true;
            }
            else */if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.42f;
                waitingForGround = false;
                worldJoin = false;
            }
            e.setRotations(savedYaw, savedPitch);
            return;
        }
        e.setRotations(savedYaw, savedPitch);

        airTicks = mc.thePlayer.onGround ? 0 : airTicks + 1;
        if (airTicks >= 10 || worldJoin) {
            if (!applyingMotion) {
                applyingMotion = true;
                firstY = mc.thePlayer.posY;
            }

            if (tickCounter < disablerTicks.getInput()) {
                shouldRender = true;

                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionZ = 0;

                if (mc.thePlayer.posY != firstY) {
                    if (!reset) {
                        resetState();
                        activationDelayMillis = 5000;
                        reset = true;
                        Utils.print("&7[&dR&7] &adisabler reset, wait 5s");
                    }
                    else {
                        shouldRun = false;
                        applyingMotion = false;
                        running = false;
                        Utils.print("&7[&dR&7] &cfailed to reset disabler, re-enable to try again");
                    }
                }

                if (mc.thePlayer.ticksExisted % 2 == 0) {
                    e.setPosZ(e.getPosZ() + 0.075);
                    e.setPosX(e.getPosX() + 0.075);
                    e.setPosY(e.getPosY() + 0.075);
                    //e.setYaw(e.getYaw() + 8);
                }

                tickCounter++;
            } else if (!warningDisplayed) {
                double totalTimeSeconds = (now - lobbyTime) / 1000.0;
                warningDisplayed = true;
                finished = now;
                shouldRender = false;
                shouldRun = false;
                applyingMotion = false;
                running = false;
            }
        }

        filledWidth = (float)(barWidth * tickCounter / disablerTicks.getInput());
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        int[] disp = {scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        barX = disp[0] / 2 - barWidth / 2;
        barY = disp[1] / 2 + 12;
    }

    @SubscribeEvent()
    public void onMoveInput(PrePlayerInputEvent e) {
        if (!running) {
            return;
        }
        e.setForward(0);
        e.setStrafe(0);
        mc.thePlayer.movementInput.jump = false;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null || !shouldRun || !shouldRender) {
                return;
            }
        }
        color = Theme.getGradient((int) HUD.theme.getInput(), 0);
        RenderUtils.drawRoundedRectangle(barX, barY, barX + barWidth, barY + barHeight, 3, 0xFF555555);
        RenderUtils.drawRoundedRectangle(barX, barY, barX + filledWidth, barY + barHeight, 3, color);
    }
}