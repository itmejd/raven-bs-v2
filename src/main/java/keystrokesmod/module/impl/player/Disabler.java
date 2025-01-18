package keystrokesmod.module.impl.player;

import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Objects;

public class Disabler extends Module {
    public ButtonSetting motion;
    public ButtonSetting bridging;

    private final int defaultSetbacks = 20;
    private final long joinDelay = 200, delay = 0, checkDisabledTime = 4000, timeout = 12000;
    private final double min_offset = 0.2;

    private long joinTime, lobbyTime, finished;
    private boolean awaitJoin, joinTick, awaitSetback, hideProgress, awaitJump, awaitGround;
    private int setbackCount, airTicks, disablerAirTicks;
    private double minSetbacks, zOffset;
    private float savedYaw, savedPitch;
    private boolean noRotateWasEnabled;
    private Class<? extends Module> noRotate;

    private boolean hasSneaked, hasWentInAir;
    private int lastSneakTicks;
    private int lastY;

    //private String text;
    private int[] disp;
    private int width;

    public Disabler() {
        super("Disabler", Module.category.player);

        this.registerSetting(motion = new ButtonSetting("Motion", false));
        this.registerSetting(bridging = new ButtonSetting("Bridging", false));
    }

    private void resetVars() {
        if (noRotateWasEnabled) {
            Objects.requireNonNull(getModule(noRotate)).enable();
        }
        awaitJoin = joinTick = awaitSetback = noRotateWasEnabled = awaitJump = false;
        minSetbacks = zOffset = lobbyTime = finished = setbackCount = 0;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        /*long now = System.currentTimeMillis();

        if (!awaitGround && !mc.thePlayer.onGround) {
            disablerAirTicks++;
        } else {
            awaitGround = false;
            disablerAirTicks = 0;
        }

        if (awaitJoin && now >= joinTime + joinDelay) {
            ItemStack item = mc.thePlayer.inventory.getStackInSlot(8);
            if (Utils.getBedwarsStatus() == 1 || Utils.isBedwarsPractice() || Utils.skywarsQueue()) {
                if (ModuleManager.noRotate.isEnabled() && Utils.skywarsQueue()) {
                    Objects.requireNonNull(getModule(noRotate)).disable();
                    noRotateWasEnabled = true;
                }
                awaitJoin = false;
                joinTick = true;
            }
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
            Utils.print("&7[&dR&7] running disabler...");
            if (mc.thePlayer.onGround || (mc.thePlayer.fallDistance < 0.3 && !Utils.isBedwarsPractice())) {
                awaitJump = true;
                mc.thePlayer.jump();
                //client.print("Jump");
                joinTick = false;
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
                //client.print("2");
                if (Utils.skywarsQueue()) {
                    zOffset = min_offset * 0.7;
                    if (mc.thePlayer.ticksExisted % 2 == 0) {
                        zOffset *= -1;
                    }
                    e.setPosZ(e.getPosZ() + zOffset);
                } else {
                    e.setPosZ(zOffset + min_offset);
                }
            }
        }*/

        if (bridging.isToggled()) {
            if (mc.gameSettings.keyBindSneak.isKeyDown() || !Safewalk.canSafeWalk() && !ModuleManager.scaffold.isEnabled) {
                lastSneakTicks = 0;
            }
            if (mc.thePlayer.onGround) { // Switching Y levels doesnt stop flagging
                if ((int) mc.thePlayer.posY != lastY && hasWentInAir) {
                    lastSneakTicks = 0;
                    Utils.print("Dif Y");
                }
                lastY = (int) mc.thePlayer.posY;
                hasWentInAir = false;
            }
            else {
                hasWentInAir = true;
            }
            lastSneakTicks++;
        }

    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (bridging.isToggled()) {
            if (hasSneaked) {
                if (!mc.gameSettings.keyBindSneak.isKeyDown()) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
                }
                hasSneaked = false;
                lastSneakTicks = 0;
            } else if (lastSneakTicks >= 19) {
                if (!mc.gameSettings.keyBindSneak.isKeyDown() && (Safewalk.canSafeWalk() || ModuleManager.scaffold.isEnabled) && mc.thePlayer.onGround) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
                    hasSneaked = true;
                    Utils.print("Sneak packet");
                }
            }
        }
    }

    @SubscribeEvent()
    public void onMoveInput(PrePlayerInputEvent e) {
        if (awaitSetback) {
            e.setForward(0);
            e.setStrafe(0);
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S08PacketPlayerPosLook) {
            setbackCount++;
            zOffset = 0;
        }
    }

    /*void onRenderTick(float partialTicks) {
        if (awaitSetback) {
            if (hideProgress || text == null) {
                return;
            }
            render.text(text, disp[0] / 2 - width, disp[1] / 2 + 13, 1, -1, true);
        }
    }*/

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            long joinTime = System.currentTimeMillis();
            if (awaitSetback) {
                Utils.print("&7[&dR&7] &cdisabing disabler");
                resetVars();
            }
            awaitJoin = awaitGround = true;
        }
    }
}