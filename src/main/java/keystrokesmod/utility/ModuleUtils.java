package keystrokesmod.utility;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.render.HUD;
import net.minecraft.client.Minecraft;
import keystrokesmod.module.ModuleManager;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;

public class ModuleUtils {
    private final Minecraft mc;

    public ModuleUtils(Minecraft mc) {
        this.mc = mc;
    }

    public static boolean isBreaking;
    public static boolean threwFireball;
    private int isBreakingTick;
    public static long MAX_EXPLOSION_DIST_SQ = 10;
    private long FIREBALL_TIMEOUT = 500L, fireballTime = 0;
    public static int inAirTicks;
    public static int fadeEdge;
    public static int lastFaceDifference;
    private int lastFace;

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C07PacketPlayerDigging) {
            isBreaking = true;
        }

        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && Utils.holdingFireball()) {
            if (Utils.keybinds.isMouseDown(1)) {
                fireballTime = System.currentTimeMillis();
                threwFireball = true;
            }
        }

        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && Utils.scaffoldDiagonal(false)) {
            if (((C08PacketPlayerBlockPlacement) e.getPacket()).getPlacedBlockDirection() != 1) {
                int currentFace = ((C08PacketPlayerBlockPlacement) e.getPacket()).getPlacedBlockDirection();

                if (currentFace == lastFace) {
                    lastFaceDifference++;
                }
                else {
                   lastFaceDifference = 0;
                }

                lastFace = currentFace;
            }
        }

    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {

        if (LongJump.slotReset && ++LongJump.slotResetTicks >= 2) {
            LongJump.stopKillAura = false;
            LongJump.stopScaffold = false;
            LongJump.slotResetTicks = 0;
            LongJump.slotReset = false;
        }

        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT) {
            threwFireball = false;
            fireballTime = 0;
            ModuleManager.velocity.disableVelo = false;
        }

        if (isBreaking && ++isBreakingTick >= 1) {
            isBreaking = false;
            isBreakingTick = 0;
        }

        if (ModuleManager.killAura.justUnTargeted) {
            if (++ModuleManager.killAura.unTargetTicks >= 2) {
                ModuleManager.killAura.unTargetTicks = 0;
                ModuleManager.killAura.justUnTargeted = false;
            }
        }


    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {

        inAirTicks = mc.thePlayer.onGround ? 0 : ++inAirTicks;

        // 7 tick needs to always finish the motion or itll lag back
        if (!ModuleManager.bhop.isEnabled() && ModuleManager.bhop.mode.getInput() == 3 && ModuleManager.bhop.didMove) {
            int simpleY = (int) Math.round((e.posY % 1) * 10000);

            if (mc.thePlayer.hurtTime == 0) {
                switch (simpleY) {
                    case 4200:
                        mc.thePlayer.motionY = 0.39;
                        break;
                    case 1138:
                        mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                        ModuleManager.bhop.lowhop = false;
                        ModuleManager.bhop.didMove = false;
                        Utils.print("7 tick");
                        break;
                    /*case 2031:
                        mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2;
                        didMove = false;
                        break;*/
                }
            }
        }

        if (ModuleManager.bhop.setRotation) {
            if (!ModuleManager.killAura.isTargeting && !Utils.noSlowingBackWithBow() && !ModuleManager.scaffold.isEnabled && !mc.thePlayer.isCollidedHorizontally) {
                float yaw = mc.thePlayer.rotationYaw;
                e.setYaw(yaw - 55);
            }
            if (mc.thePlayer.onGround) {
                ModuleManager.bhop.setRotation = false;
            }
        }

        if (ModuleManager.scaffold.canBlockFade && !ModuleManager.scaffold.isEnabled && ++fadeEdge >= 45) {
            ModuleManager.scaffold.canBlockFade = false;
            fadeEdge = 0;
            ModuleManager.scaffold.highlight.clear();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!ModuleManager.scaffold.canBlockFade) {
            return;
        }
        if (!Utils.nullCheck() || !ModuleManager.scaffold.highlightBlocks.isToggled() || ModuleManager.scaffold.highlight.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Timer>> iterator = ModuleManager.scaffold.highlight.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Timer> entry = iterator.next();
            if (entry.getValue() == null) {
                entry.setValue(new Timer(750));
                entry.getValue().start();
            }
            int alpha = entry.getValue() == null ? 210 : 210 - entry.getValue().getValueInt(0, 210, 1);
            if (alpha == 0) {
                iterator.remove();
                continue;
            }
            RenderUtils.renderBlock(entry.getKey(), Utils.mergeAlpha(Theme.getGradient((int) HUD.theme.getInput(), 0), alpha), true, false);
        }
    }
}